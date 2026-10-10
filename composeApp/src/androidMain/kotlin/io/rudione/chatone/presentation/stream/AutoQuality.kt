package io.rudione.chatone.presentation.stream

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.Timeline
import androidx.media3.common.TrackGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.chunk.MediaChunk
import androidx.media3.exoplayer.source.chunk.MediaChunkIterator
import androidx.media3.exoplayer.trackselection.BaseTrackSelection
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.trackselection.TrackSelectionUtil
import androidx.media3.exoplayer.upstream.BandwidthMeter
import io.rudione.chatone.domain.stream.AdaptiveQualityPolicy
import io.rudione.chatone.domain.stream.TransferSample
import io.rudione.chatone.domain.stream.TransferSampler

internal class NetworkCapacityMemory {
    @Volatile
    var capacityBps: Long = 0L
}

@UnstableApi
internal class AutoQualityController(
    private val context: Context,
    private val memory: NetworkCapacityMemory
) {
    private val policy = AdaptiveQualityPolicy()
    private val lock = Any()

    @Volatile
    private var loadingBitrate = 0L

    val capacityBps: Long get() = synchronized(lock) { policy.capacityBps }

    fun startSession() {
        val initial = memory.capacityBps.takeIf { it > 0L } ?: linkHintBps()
        synchronized(lock) { policy.start(now(), initial) }
    }

    fun onTransfer(sample: TransferSample) {
        val capacity = synchronized(lock) {
            policy.onTransfer(sample, loadingBitrate, now())
            policy.capacityBps
        }
        memory.capacityBps = capacity
    }

    fun onRebuffer() {
        synchronized(lock) { policy.onRebuffer(loadingBitrate, now()) }
    }

    fun select(bitrates: IntArray, current: Int, isAllowed: (Int) -> Boolean): Int {
        val selected = synchronized(lock) { policy.select(bitrates, current, isAllowed, now()) }
        loadingBitrate = bitrates.getOrNull(selected)?.toLong() ?: 0L
        return selected
    }

    private fun linkHintBps(): Long {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return DEFAULT_HINT_BPS
        val capabilities = runCatching { manager.getNetworkCapabilities(manager.activeNetwork) }.getOrNull()
            ?: return DEFAULT_HINT_BPS
        val linkKbps = capabilities.linkDownstreamBandwidthKbps
        if (linkKbps <= 0) return DEFAULT_HINT_BPS
        return (linkKbps * 1_000L * LINK_SHARE).toLong().coerceIn(MIN_HINT_BPS, MAX_HINT_BPS)
    }

    private fun now(): Long = SystemClock.elapsedRealtime()

    private companion object {
        const val DEFAULT_HINT_BPS = 5_000_000L
        const val MIN_HINT_BPS = 1_500_000L
        const val MAX_HINT_BPS = 12_000_000L
        const val LINK_SHARE = 0.25
    }
}

@UnstableApi
internal class StreamBandwidthMeter(private val quality: AutoQualityController) : BandwidthMeter, TransferListener {

    private val dispatcher = BandwidthMeter.EventListener.EventDispatcher()
    private val samplers = HashMap<DataSource, TransferSampler>()

    override fun getBitrateEstimate(): Long = quality.capacityBps

    override fun getTransferListener(): TransferListener = this

    override fun addEventListener(eventHandler: Handler, eventListener: BandwidthMeter.EventListener) {
        dispatcher.addListener(eventHandler, eventListener)
    }

    override fun removeEventListener(eventListener: BandwidthMeter.EventListener) {
        dispatcher.removeListener(eventListener)
    }

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        if (!isNetwork) return
        synchronized(samplers) { samplers[source] = TransferSampler() }
    }

    override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {
        if (!isNetwork) return
        val now = SystemClock.elapsedRealtimeNanos()
        synchronized(samplers) { samplers[source]?.onBytes(now, bytesTransferred) }
    }

    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {
        if (!isNetwork) return
        val sample = synchronized(samplers) { samplers.remove(source) }?.finish() ?: return
        quality.onTransfer(sample)
        dispatcher.bandwidthSample(0, sample.totalBytes, quality.capacityBps)
    }
}

@UnstableApi
internal class AutoQualityTrackSelectionFactory(private val quality: AutoQualityController) : ExoTrackSelection.Factory {
    override fun createTrackSelections(
        definitions: Array<out ExoTrackSelection.Definition?>,
        bandwidthMeter: BandwidthMeter,
        mediaPeriodId: MediaSource.MediaPeriodId,
        timeline: Timeline
    ): Array<ExoTrackSelection?> =
        TrackSelectionUtil.createTrackSelectionsForDefinitions(definitions) { definition ->
            AutoQualityTrackSelection(definition.group, definition.tracks, definition.type, quality)
        }
}

@UnstableApi
private class AutoQualityTrackSelection(
    group: TrackGroup,
    tracks: IntArray,
    type: Int,
    private val quality: AutoQualityController
) : BaseTrackSelection(group, tracks, type) {

    private val bitrates = IntArray(length) { getFormat(it).bitrate.coerceAtLeast(0) }
    private var selected = quality.select(bitrates, C.INDEX_UNSET) { true }
    private var reason = C.SELECTION_REASON_INITIAL

    override fun getSelectedIndex(): Int = selected

    override fun getSelectionReason(): Int = reason

    override fun getSelectionData(): Any? = null

    override fun updateSelectedTrack(
        playbackPositionUs: Long,
        bufferedDurationUs: Long,
        availableDurationUs: Long,
        queue: List<MediaChunk>,
        mediaChunkIterators: Array<out MediaChunkIterator>
    ) {
        val nowMs = SystemClock.elapsedRealtime()
        val next = quality.select(bitrates, selected) { !isTrackExcluded(it, nowMs) }
        if (next != selected) {
            selected = next
            reason = C.SELECTION_REASON_ADAPTIVE
        }
    }

    override fun onRebuffer() {
        quality.onRebuffer()
    }
}

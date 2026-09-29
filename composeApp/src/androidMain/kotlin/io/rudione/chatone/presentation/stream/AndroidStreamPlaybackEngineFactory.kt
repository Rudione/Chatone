package io.rudione.chatone.presentation.stream

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import io.rudione.chatone.data.remote.proxy.buildMediaOkHttpClient
import io.rudione.chatone.data.repository.AccountManager
import io.rudione.chatone.domain.model.AccountProxyConfig
import io.rudione.chatone.domain.stream.StreamAdBreakSource
import io.rudione.chatone.domain.stream.StreamPlaybackEngine
import io.rudione.chatone.domain.stream.StreamPlaybackEngineFactory
import okhttp3.OkHttpClient

internal class AndroidStreamPlaybackEngineFactory(
    private val context: Context,
    private val accountManager: AccountManager,
    private val adBreakSource: StreamAdBreakSource
) : StreamPlaybackEngineFactory {

    override val isSupported: Boolean = true

    override val supportedCodecs: String by lazy(::detectCodecs)

    private var cachedClient: Pair<AccountProxyConfig?, OkHttpClient>? = null

    @UnstableApi
    override fun create(): StreamPlaybackEngine =
        AndroidStreamPlaybackEngine(
            context = context,
            httpClient = clientFor(accountManager.activeProxy.value),
            adBreakSource = adBreakSource,
            supportedCodecs = supportedCodecs
        )

    private fun clientFor(proxy: AccountProxyConfig?): OkHttpClient {
        cachedClient?.takeIf { it.first == proxy }?.let { return it.second }
        return buildMediaOkHttpClient(proxy).also { cachedClient = proxy to it }
    }

    private fun detectCodecs(): String {
        val hardwareHevc = runCatching { hasHardwareDecoder(MimeTypes.VIDEO_H265) }.getOrDefault(false)
        return if (hardwareHevc) "h265,h264" else "h264"
    }

    private fun hasHardwareDecoder(mimeType: String): Boolean =
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } && info.isHardwareBacked()
        }

    private fun MediaCodecInfo.isHardwareBacked(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isHardwareAccelerated
        } else {
            val lower = name.lowercase()
            !lower.startsWith("omx.google.") && !lower.startsWith("c2.android.") && !lower.contains(".sw.")
        }
}

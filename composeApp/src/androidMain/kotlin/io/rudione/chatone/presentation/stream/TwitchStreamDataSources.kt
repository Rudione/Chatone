package io.rudione.chatone.presentation.stream

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSchemeDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.hls.playlist.DefaultHlsPlaylistParserFactory
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParserFactory
import androidx.media3.exoplayer.upstream.ParsingLoadable
import androidx.media3.common.ParserException
import io.rudione.chatone.data.remote.stream.TwitchHlsPlaylist
import io.rudione.chatone.util.link.OutboundUrlPolicy
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

@UnstableApi
internal class RestrictedStreamDataSourceFactory(
    private val httpFactory: DataSource.Factory
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        RestrictedStreamDataSource(httpFactory.createDataSource(), DataSchemeDataSource())
}

@UnstableApi
private class RestrictedStreamDataSource(
    private val http: DataSource,
    private val inline: DataSource
) : DataSource {

    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        http.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val source = when (dataSpec.uri.scheme?.lowercase()) {
            SCHEME_HTTPS -> http.takeIf { isAllowedHost(dataSpec.uri) }
            SCHEME_DATA -> inline
            else -> null
        } ?: throw DataSourceException(PlaybackException.ERROR_CODE_IO_NO_PERMISSION)
        active = source
        return source.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(active) { "read before open" }.read(buffer, offset, length)

    override fun getUri(): Uri? = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = active?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = active
        active = null
        source?.close()
    }

    private fun isAllowedHost(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return uri.userInfo == null && OutboundUrlPolicy.isPublicHost(host)
    }

    private companion object {
        const val SCHEME_HTTPS = "https"
        const val SCHEME_DATA = "data"
    }
}

@UnstableApi
internal class TwitchPlaylistParserFactory(
    private val lowLatency: Boolean,
    private val mediaPlaylistFilter: (url: String, playlist: String) -> String
) : HlsPlaylistParserFactory {

    private val delegate = DefaultHlsPlaylistParserFactory()
    private val prefetchSeen = AtomicBoolean(false)

    val isPrefetchActive: Boolean get() = prefetchSeen.get()

    override fun createPlaylistParser(): ParsingLoadable.Parser<HlsPlaylist> =
        rewriting(delegate.createPlaylistParser())

    override fun createPlaylistParser(
        multivariantPlaylist: HlsMultivariantPlaylist,
        previousMediaPlaylist: HlsMediaPlaylist?
    ): ParsingLoadable.Parser<HlsPlaylist> =
        rewriting(delegate.createPlaylistParser(multivariantPlaylist, previousMediaPlaylist))

    private fun rewriting(parser: ParsingLoadable.Parser<HlsPlaylist>): ParsingLoadable.Parser<HlsPlaylist> =
        ParsingLoadable.Parser { uri, input ->
            val received = input.readLimited(TwitchHlsPlaylist.MAX_PLAYLIST_CHARS).decodeToString()
            val text = mediaPlaylistFilter(uri.toString(), received)
            val rewritten = TwitchHlsPlaylist.rewriteMediaPlaylist(text, expandPrefetch = lowLatency)
            if (lowLatency && rewritten != null && text.contains(PREFETCH_TAG)) prefetchSeen.set(true)
            parser.parse(uri, ByteArrayInputStream((rewritten ?: text).encodeToByteArray()))
        }

    private fun InputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, INITIAL_BUFFER))
        val buffer = ByteArray(INITIAL_BUFFER)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw ParserException.createForMalformedManifest("Playlist too large", null)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val INITIAL_BUFFER = 8_192
        const val PREFETCH_TAG = "#EXT-X-TWITCH-PREFETCH:"
    }
}

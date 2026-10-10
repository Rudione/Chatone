package io.rudione.chatone.presentation.stream

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommands
import io.github.aakira.napier.Napier
import io.rudione.chatone.shared.R
import org.koin.android.ext.android.inject

@UnstableApi
internal class StreamMediaSessions(private val context: Context) {

    private var session: MediaSession? = null
    private var service: StreamPlaybackService? = null
    private var sessionCounter = 0

    val current: MediaSession? get() = session

    fun open(player: Player, artworkLoader: BitmapLoader): MediaSession {
        session?.let(::close)
        sessionCounter++
        val created = MediaSession.Builder(context, LiveSessionPlayer(player))
            .setId("$SESSION_PREFIX$sessionCounter")
            .setBitmapLoader(artworkLoader)
            .setCallback(LiveSessionCallback)
            .apply { launchIntent()?.let(::setSessionActivity) }
            .build()
        session = created
        val attached = service
        if (attached != null) attached.addSession(created) else startService()
        return created
    }

    fun close(closing: MediaSession) {
        if (session !== closing) return
        session = null
        service?.let { attached ->
            attached.removeSession(closing)
            attached.stopSelf()
        }
        closing.release()
    }

    fun attach(playbackService: StreamPlaybackService) {
        service = playbackService
        session?.let(playbackService::addSession)
    }

    fun detach(playbackService: StreamPlaybackService) {
        if (service !== playbackService) return
        service = null
        if (session != null) startService()
    }

    private fun startService() {
        runCatching { context.startService(Intent(context, StreamPlaybackService::class.java)) }
            .onFailure { Napier.w("Stream playback service did not start: ${it.message}", tag = TAG) }
    }

    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private companion object {
        const val TAG = "StreamMediaSessions"
        const val SESSION_PREFIX = "chatone-stream-"
    }
}

@UnstableApi
private object LiveSessionCallback : MediaSession.Callback {
    private val playerCommands = Player.Commands.Builder()
        .addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
            Player.COMMAND_GET_METADATA,
            Player.COMMAND_GET_TIMELINE,
            Player.COMMAND_GET_AUDIO_ATTRIBUTES
        )
        .build()

    override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
        MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailablePlayerCommands(playerCommands)
            .setAvailableSessionCommands(SessionCommands.EMPTY)
            .build()
}

@UnstableApi
private class LiveSessionPlayer(player: Player) : ForwardingPlayer(player) {
    override fun play() {
        val wrapped = wrappedPlayer
        if (!wrapped.playWhenReady && wrapped.isCurrentMediaItemLive) wrapped.seekToDefaultPosition()
        super.play()
    }
}

@UnstableApi
class StreamPlaybackService : MediaSessionService() {

    private val sessions: StreamMediaSessions by inject()

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.chatbubbles) }
        )
        sessions.attach(this)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = sessions.current

    override fun onDestroy() {
        sessions.detach(this)
        super.onDestroy()
    }
}

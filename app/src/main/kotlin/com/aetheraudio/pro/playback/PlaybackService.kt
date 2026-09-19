package com.aetheraudio.pro.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Keeps playback alive in the background and is what drives the system media notification,
 * lock-screen transport controls, and hardware/Bluetooth media-button handling — Media3's
 * MediaSession does all of that for free once a session is published here.
 */
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true) // Headset Safety Guard (spec 6): auto-pause on unplug
            .build()

        AudioEffectsManager.attach(player.audioSessionId)

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {})
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        AudioEffectsManager.release()
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Only stop when nothing is actively playing, so audio keeps going after the
        // task/recents entry is swiped away (standard music-app behaviour).
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }
}

package de.displayware.app.player

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * Controller for Video Playback using ExoPlayer (Media3).
 * Optimized for low RAM footprint and seamless looping.
 */
class VideoPlayerController(
    private val context: Context,
    private val playerView: PlayerView
) {
    private var player: ExoPlayer? = null

    /**
     * Initializes the player with low-buffer settings to save RAM.
     */
    fun initialize() {
        if (player != null) return

        // Low RAM configuration: Small buffers
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000, // minBufferMs
                15000, // maxBufferMs
                1000, // bufferForPlaybackMs
                2000 // bufferForPlaybackAfterRebufferMs
            )
            .build()

        player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }

        playerView.player = player
    }

    /**
     * Starts playing a local video file.
     */
    fun playVideo(file: File) {
        initialize()
        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        player?.setMediaItem(mediaItem)
        player?.prepare()
        playerView.visibility = View.VISIBLE
    }

    /**
     * Stops and hides the video player.
     */
    fun stop() {
        player?.stop()
        playerView.visibility = View.GONE
    }

    /**
     * Releases resources when the activity is destroyed.
     */
    fun release() {
        player?.release()
        player = null
        playerView.player = null
    }
}

package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.Video
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(UnstableApi::class)
object VideoPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    
    private val _currentVideo = MutableStateFlow<Video?>(null)
    val currentVideo: StateFlow<Video?> = _currentVideo

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _isBackgroundPlaybackEnabled = MutableStateFlow(true)
    val isBackgroundPlaybackEnabled: StateFlow<Boolean> = _isBackgroundPlaybackEnabled

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled

    var onVideoEnded: (() -> Unit)? = null

    private var progressTrackerRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startTrackingProgress()
                        } else {
                            stopTrackingProgress()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                        } else if (playbackState == Player.STATE_ENDED) {
                            if (_isRepeatEnabled.value) {
                                seekTo(0)
                                play()
                            } else {
                                onVideoEnded?.invoke()
                            }
                        }
                    }
                })
            }
        }
        return exoPlayer!!
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun toggleAutoplay() {
        _isAutoplayEnabled.value = !_isAutoplayEnabled.value
    }

    fun playVideo(context: Context, video: Video) {
        _currentVideo.value = video
        val player = getPlayer(context)
        player.stop()
        player.clearMediaItems()
        
        val uri = Uri.parse(video.videoUri)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        // Check if background playback is active, start service
        if (_isBackgroundPlaybackEnabled.value) {
            startAudioService(context)
        }
    }

    fun togglePlayPause(context: Context) {
        val player = getPlayer(context)
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        updateServiceIfNeeded(context)
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun play() {
        exoPlayer?.play()
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        _playbackPosition.value = position
    }

    fun toggleBackgroundPlayback(context: Context, enabled: Boolean) {
        _isBackgroundPlaybackEnabled.value = enabled
        if (enabled && _isPlaying.value) {
            startAudioService(context)
        } else {
            stopAudioService(context)
        }
    }

    private fun startTrackingProgress() {
        stopTrackingProgress()
        progressTrackerRunnable = object : Runnable {
            override fun run() {
                exoPlayer?.let {
                    _playbackPosition.value = it.currentPosition
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(progressTrackerRunnable!!)
    }

    private fun stopTrackingProgress() {
        progressTrackerRunnable?.let { handler.removeCallbacks(it) }
        progressTrackerRunnable = null
    }

    fun startAudioService(context: Context) {
        val serviceIntent = Intent(context.applicationContext, BackgroundAudioService::class.java).apply {
            action = BackgroundAudioService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    fun stopAudioService(context: Context) {
        val serviceIntent = Intent(context.applicationContext, BackgroundAudioService::class.java)
        context.stopService(serviceIntent)
    }

    private fun updateServiceIfNeeded(context: Context) {
        if (_isBackgroundPlaybackEnabled.value && _isPlaying.value) {
            startAudioService(context)
        }
    }

    fun release() {
        stopTrackingProgress()
        exoPlayer?.release()
        exoPlayer = null
    }
}

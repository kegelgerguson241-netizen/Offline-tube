package com.example.service

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import com.example.data.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode {
    NONE,  // Stops when playlist ends
    ONE,   // Repeats current track
    ALL    // Loops the entire playlist (default)
}

object AudioPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _isRepeatTrackEnabled = MutableStateFlow(false)
    val isRepeatTrackEnabled: StateFlow<Boolean> = _isRepeatTrackEnabled

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    var activePlaylist: List<AudioTrack> = emptyList()

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
            RepeatMode.NONE -> RepeatMode.ALL
        }
    }

    fun toggleRepeatTrack() {
        _isRepeatTrackEnabled.value = !_isRepeatTrackEnabled.value
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun playRandom(context: Context) {
        if (activePlaylist.isEmpty()) return
        val current = _currentTrack.value
        val nextTrack = if (activePlaylist.size > 1 && current != null) {
            val otherTracks = activePlaylist.filter { it.id != current.id }
            otherTracks.random()
        } else {
            activePlaylist.random()
        }
        playTrack(context, nextTrack)
    }

    fun playTrack(context: Context, track: AudioTrack, playlist: List<AudioTrack> = emptyList()) {
        // Stop video player if playing to avoid audio overlap
        VideoPlayerManager.pause()

        if (playlist.isNotEmpty()) {
            activePlaylist = playlist
        } else if (!activePlaylist.contains(track)) {
            activePlaylist = listOf(track)
        }

        stopProgressTracker()
        mediaPlayer?.release()

        try {
            mediaPlayer = MediaPlayer().apply {
                if (track.audioUri.startsWith("http://") || track.audioUri.startsWith("https://")) {
                    setDataSource(track.audioUri)
                } else {
                    setDataSource(context, Uri.parse(track.audioUri))
                }
                prepare()
                start()
                _isPlaying.value = true
                _currentTrack.value = track
                _duration.value = duration.toLong()
                
                setOnCompletionListener {
                    if (_isRepeatTrackEnabled.value) {
                        playTrack(context, track)
                    } else if (_isShuffleEnabled.value) {
                        _isPlaying.value = false
                        playRandom(context)
                    } else {
                        _isPlaying.value = false
                        playNext(context)
                    }
                }
            }
            startProgressTracker()
            startAudioService(context)
        } catch (e: Exception) {
            e.printStackTrace()
            _isPlaying.value = false
        }
    }

    fun togglePlayPause(context: Context) {
        val current = _currentTrack.value ?: return
        val player = mediaPlayer

        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
                startAudioService(context)
            }
        } else {
            playTrack(context, current)
        }
    }

    fun playNext(context: Context) {
        val current = _currentTrack.value ?: return
        if (activePlaylist.isEmpty()) return

        if (_isShuffleEnabled.value) {
            playRandom(context)
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == current.id }
            if (currentIndex != -1 && currentIndex < activePlaylist.size - 1) {
                playTrack(context, activePlaylist[currentIndex + 1])
            } else if (activePlaylist.isNotEmpty()) {
                playTrack(context, activePlaylist[0]) // Loop back to start
            }
        }
    }

    fun playNextNone(context: Context) {
        val current = _currentTrack.value ?: return
        if (activePlaylist.isEmpty()) return

        val currentIndex = activePlaylist.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex < activePlaylist.size - 1) {
            playTrack(context, activePlaylist[currentIndex + 1])
        } else {
            stop()
        }
    }

    fun playPrevious(context: Context) {
        val current = _currentTrack.value ?: return
        if (activePlaylist.isEmpty()) return

        if (_isShuffleEnabled.value) {
            playRandom(context)
        } else {
            val currentIndex = activePlaylist.indexOfFirst { it.id == current.id }
            if (currentIndex > 0) {
                playTrack(context, activePlaylist[currentIndex - 1])
            } else if (activePlaylist.isNotEmpty()) {
                playTrack(context, activePlaylist[activePlaylist.size - 1]) // Loop to end
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _playbackPosition.value = positionMs
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentTrack.value = null
        _playbackPosition.value = 0L
        _duration.value = 0L
        stopProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob = coroutineScope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playbackPosition.value = it.currentPosition.toLong()
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
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
}

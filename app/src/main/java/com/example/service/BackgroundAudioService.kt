package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class BackgroundAudioService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var notificationManager: NotificationManager
    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "offline_tube_playback_channel"
        const val NOTIFICATION_ID = 8123

        const val ACTION_START = "com.example.action.START"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        // Initialize MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "OfflineTubeMediaSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    handlePlay()
                }

                override fun onPause() {
                    handlePause()
                }

                override fun onSkipToNext() {
                    handleNext()
                }

                override fun onSkipToPrevious() {
                    handlePrevious()
                }

                override fun onStop() {
                    handleStop()
                }
            })
        }

        // Observe video player changes
        VideoPlayerManager.isPlaying
            .onEach { updateNotification() }
            .launchIn(serviceScope)

        VideoPlayerManager.currentVideo
            .onEach { updateNotification() }
            .launchIn(serviceScope)

        // Observe audio player changes
        AudioPlayerManager.isPlaying
            .onEach { updateNotification() }
            .launchIn(serviceScope)

        AudioPlayerManager.currentTrack
            .onEach { updateNotification() }
            .launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundServiceCompat()
            }
            ACTION_PLAY -> {
                handlePlay()
            }
            ACTION_PAUSE -> {
                handlePause()
            }
            ACTION_STOP -> {
                handleStop()
            }
            ACTION_NEXT -> {
                handleNext()
            }
            ACTION_PREVIOUS -> {
                handlePrevious()
            }
        }
        return START_NOT_STICKY
    }

    private fun handlePlay() {
        if (AudioPlayerManager.currentTrack.value != null) {
            AudioPlayerManager.togglePlayPause(this)
        } else {
            VideoPlayerManager.play()
        }
        updateNotification()
    }

    private fun handlePause() {
        if (AudioPlayerManager.currentTrack.value != null) {
            AudioPlayerManager.togglePlayPause(this)
        } else {
            VideoPlayerManager.pause()
        }
        updateNotification()
    }

    private fun handleStop() {
        if (AudioPlayerManager.currentTrack.value != null) {
            AudioPlayerManager.stop()
        } else {
            VideoPlayerManager.pause()
        }
        stopForeground(true)
        stopSelf()
    }

    private fun handleNext() {
        if (AudioPlayerManager.currentTrack.value != null) {
            AudioPlayerManager.playNext(this)
        }
        updateNotification()
    }

    private fun handlePrevious() {
        if (AudioPlayerManager.currentTrack.value != null) {
            AudioPlayerManager.playPrevious(this)
        }
        updateNotification()
    }

    private fun startForegroundServiceCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val isAudioActive = AudioPlayerManager.currentTrack.value != null
        val isVideoActive = VideoPlayerManager.currentVideo.value != null

        if (isAudioActive || isVideoActive) {
            updateMediaSession()
            val notification = buildNotification()
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun updateMediaSession() {
        val session = mediaSession ?: return

        val audioTrack = AudioPlayerManager.currentTrack.value
        val isAudioActive = audioTrack != null

        val title: String
        val artist: String
        val isPlaying: Boolean
        val position: Long

        if (isAudioActive) {
            title = audioTrack?.title ?: ""
            artist = audioTrack?.artist ?: ""
            isPlaying = AudioPlayerManager.isPlaying.value
            position = AudioPlayerManager.playbackPosition.value
        } else {
            val video = VideoPlayerManager.currentVideo.value
            title = video?.title ?: ""
            artist = video?.description ?: ""
            isPlaying = VideoPlayerManager.isPlaying.value
            position = VideoPlayerManager.playbackPosition.value
        }

        // Set Metadata
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .build()
        session.setMetadata(metadata)

        // Set PlaybackState
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                position,
                1.0f
            )
            .build()
        session.setPlaybackState(state)
    }

    private fun buildNotification(): Notification {
        val audioTrack = AudioPlayerManager.currentTrack.value
        val isAudioActive = audioTrack != null

        val title: String
        val desc: String
        val isPlaying: Boolean

        if (isAudioActive) {
            title = audioTrack?.title ?: "Аудио"
            desc = audioTrack?.artist ?: "Неизвестный исполнитель"
            isPlaying = AudioPlayerManager.isPlaying.value
        } else {
            val video = VideoPlayerManager.currentVideo.value
            title = video?.title ?: "OfflineTube"
            desc = video?.description ?: "Фоновое воспроизведение"
            isPlaying = VideoPlayerManager.isPlaying.value
        }

        // Intent to open MainActivity on tap
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Action Play/Pause
        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_PAUSE }
            val pendingPauseIntent = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Пауза",
                pendingPauseIntent
            )
        } else {
            val playIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_PLAY }
            val pendingPlayIntent = PendingIntent.getService(
                this, 2, playIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Воспроизвести",
                pendingPlayIntent
            )
        }

        // Action Stop
        val stopIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_STOP }
        val pendingStopIntent = PendingIntent.getService(
            this, 3, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Закрыть",
            pendingStopIntent
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(desc)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .addAction(playPauseAction)

        // Add Next/Prev actions if it's Audio mode
        if (isAudioActive) {
            val prevIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_PREVIOUS }
            val pendingPrevIntent = PendingIntent.getService(
                this, 4, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_media_previous, "Назад", pendingPrevIntent)

            val nextIntent = Intent(this, BackgroundAudioService::class.java).apply { action = ACTION_NEXT }
            val pendingNextIntent = PendingIntent.getService(
                this, 5, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_media_next, "Вперед", pendingNextIntent)
        }

        builder.addAction(stopAction)
            .setOngoing(isPlaying)

        // Apply MediaStyle for Android media controls widget support
        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0) // Show play/pause in compact view

        builder.setStyle(mediaStyle)

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Фоновое воспроизведение",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Используется для управления аудио/видео во время фонового воспроизведения"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSession?.release()
        mediaSession = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

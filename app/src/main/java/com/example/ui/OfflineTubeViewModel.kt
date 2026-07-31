package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Channel
import com.example.data.Video
import com.example.data.VideoRepository
import com.example.data.WatchHistory
import com.example.service.VideoPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.Locale
import com.example.data.Comment
import com.example.data.Playlist
import com.example.data.PlaylistVideo
import com.example.data.AudioTrack
import com.example.data.AudioPlaylist
import com.example.data.AudioPlaylistTrack
import com.example.data.Manga
import com.example.data.MangaChapter
import com.example.data.MangaReadingProgress
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileOutputStream
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject

data class AppNotification(
    val id: Int,
    val title: String,
    val text: String,
    val videoId: Int,
    val channelName: String,
    val channelAvatar: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

class OfflineTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    
    val isAdultMode = MutableStateFlow(false)
    val notifications = MutableStateFlow<List<AppNotification>>(emptyList())

    private val rawChannels: StateFlow<List<Channel>>
    private val rawVideos: StateFlow<List<Video>>
    private val rawRecommendedVideos: StateFlow<List<Video>>

    val allChannels: StateFlow<List<Channel>>
    val allVideos: StateFlow<List<Video>>
    val recommendedVideos: StateFlow<List<Video>>
    val watchHistory: StateFlow<List<WatchHistory>>
    val videoProgressMap: StateFlow<Map<Int, Float>>
    val isPlaying = VideoPlayerManager.isPlaying
    val currentVideo = VideoPlayerManager.currentVideo
    val playbackPosition = VideoPlayerManager.playbackPosition
    val duration = VideoPlayerManager.duration
    val isBackgroundPlaybackEnabled = VideoPlayerManager.isBackgroundPlaybackEnabled
    val isRepeatEnabled = VideoPlayerManager.isRepeatEnabled
    val isAutoplayEnabled = VideoPlayerManager.isAutoplayEnabled

    // Global Floating/Overlay player states
    val isPlayerActive = MutableStateFlow(false)
    val isPlayerExpanded = MutableStateFlow(false)
    val isAudioOnly = MutableStateFlow(false)
    val currentPlaylistVideos = MutableStateFlow<List<Video>?>(null)

    private fun parseDurationToMs(durationStr: String): Long {
        try {
            val parts = durationStr.split(":").map { it.trim().toIntOrNull() ?: 0 }
            val seconds = when (parts.size) {
                1 -> parts[0]
                2 -> parts[0] * 60 + parts[1] // MM:SS
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2] // HH:MM:SS
                else -> 0
            }
            return seconds.toLong() * 1000
        } catch (e: Exception) {
            return 0L
        }
    }

    fun saveVideoProgress() {
        val video = currentVideo.value ?: return
        val position = VideoPlayerManager.playbackPosition.value
        viewModelScope.launch(Dispatchers.IO) {
            val channel = repository.getChannelById(video.channelId)
            if (channel?.isLectorMode == true) {
                repository.addToHistory(video.id, position)
            }
        }
    }

    fun minimizePlayer() {
        saveVideoProgress()
        isPlayerExpanded.value = false
    }

    fun expandPlayer() {
        isPlayerExpanded.value = true
    }

    fun closePlayer() {
        saveVideoProgress()
        isPlayerActive.value = false
        isPlayerExpanded.value = false
        VideoPlayerManager.pause()
        VideoPlayerManager.stopAudioService(getApplication())
    }

    // Audio Player Manager Delegates
    val isAudioPlaying = com.example.service.AudioPlayerManager.isPlaying
    val currentAudioTrack = com.example.service.AudioPlayerManager.currentTrack
    val audioPlaybackPosition = com.example.service.AudioPlayerManager.playbackPosition
    val audioDuration = com.example.service.AudioPlayerManager.duration
    val audioRepeatMode = com.example.service.AudioPlayerManager.repeatMode
    val audioRepeatTrackEnabled = com.example.service.AudioPlayerManager.isRepeatTrackEnabled
    val audioShuffleEnabled = com.example.service.AudioPlayerManager.isShuffleEnabled

    fun playAudioTrack(track: AudioTrack, playlist: List<AudioTrack> = emptyList()) {
        com.example.service.AudioPlayerManager.playTrack(getApplication(), track, playlist)
    }

    fun toggleAudioPlayPause() {
        com.example.service.AudioPlayerManager.togglePlayPause(getApplication())
    }

    fun playNextAudio() {
        com.example.service.AudioPlayerManager.playNext(getApplication())
    }

    fun playPreviousAudio() {
        com.example.service.AudioPlayerManager.playPrevious(getApplication())
    }

    fun toggleAudioRepeatMode() {
        com.example.service.AudioPlayerManager.toggleRepeatMode()
    }

    fun toggleAudioRepeatTrack() {
        com.example.service.AudioPlayerManager.toggleRepeatTrack()
    }

    fun toggleAudioShuffle() {
        com.example.service.AudioPlayerManager.toggleShuffle()
    }

    fun seekAudioTo(position: Long) {
        com.example.service.AudioPlayerManager.seekTo(position)
    }

    fun stopAudioPlayback() {
        com.example.service.AudioPlayerManager.stop()
    }

    // Search & Filtering State
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Все") // "Все", "Новые", "Рекомендованные", "Популярные", "Понравившиеся"
    val selectedChannelId = MutableStateFlow<Int?>(null)
    val activeChannelDetailId = MutableStateFlow<Int?>(null)
    val selectedShortsId = MutableStateFlow<Int?>(null)
    val playlistShortsToPlay = MutableStateFlow<List<Video>?>(null)

    // UI Feedback State
    val uiEvent = MutableStateFlow<String?>(null)

    val allPlaylists: StateFlow<List<Playlist>>
    val allAudioTracks: StateFlow<List<AudioTrack>>
    val allAudioPlaylists: StateFlow<List<AudioPlaylist>>
    val allManga: StateFlow<List<Manga>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = VideoRepository(db)

        rawChannels = repository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        rawVideos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        rawRecommendedVideos = repository.recommendedVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allChannels = combine(rawChannels, isAdultMode) { channels, adult ->
            channels.filter { it.isAgeRestricted == adult }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allVideos = combine(rawVideos, isAdultMode) { videos, adult ->
            videos.filter { it.isAgeRestricted == adult }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        recommendedVideos = combine(rawRecommendedVideos, isAdultMode) { videos, adult ->
            videos.filter { it.isAgeRestricted == adult }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        watchHistory = repository.watchHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        videoProgressMap = combine(repository.watchHistory, repository.allVideos) { history, videos ->
            history.associate { hist ->
                val video = videos.find { it.id == hist.videoId }
                val progress = if (video != null && hist.lastPosition > 0) {
                    val durMs = parseDurationToMs(video.duration)
                    if (durMs > 0) {
                        hist.lastPosition.toFloat() / durMs.toFloat()
                    } else 0f
                } else 0f
                hist.videoId to progress
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

        allPlaylists = combine(repository.allPlaylists, isAdultMode) { playlists, adult ->
            playlists.filter { it.isAgeRestricted == adult }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAudioTracks = repository.allAudioTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAudioPlaylists = repository.allAudioPlaylists.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val mangaDao = db.mangaDao()
        allManga = mangaDao.getAllManga().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Setup Auto-advance sequential play callback
        VideoPlayerManager.onVideoEnded = {
            val playlist = currentPlaylistVideos.value
            val current = VideoPlayerManager.currentVideo.value
            if (playlist != null && playlist.isNotEmpty() && current != null) {
                val currentIndex = playlist.indexOfFirst { it.id == current.id }
                if (currentIndex != -1) {
                    if (currentIndex < playlist.size - 1) {
                        val nextVideo = playlist[currentIndex + 1]
                        playVideo(nextVideo, playlist)
                    } else {
                        // Last video ended, loop back to the first video in playlist
                        val firstVideo = playlist[0]
                        playVideo(firstVideo, playlist)
                    }
                }
            } else if (VideoPlayerManager.isAutoplayEnabled.value) {
                if (current != null) {
                    val currentList = allVideos.value.filter { !it.isShorts }.sortedByDescending { it.uploadDate }
                    val currentIndex = currentList.indexOfFirst { it.id == current.id }
                    if (currentIndex != -1 && currentIndex < currentList.size - 1) {
                        val nextVideo = currentList[currentIndex + 1]
                        playVideo(nextVideo)
                    } else if (currentList.isNotEmpty()) {
                        // Loop back to the first video
                        playVideo(currentList[0])
                    }
                }
            }
        }

        // Prepopulate on startup if empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndPrepopulate()
        }

        // Initialize simulated notifications once channels and videos are ready
        viewModelScope.launch {
            combine(rawVideos, rawChannels) { videos, channels ->
                videos.isNotEmpty() && channels.isNotEmpty()
            }.collect { available ->
                if (available && notifications.value.isEmpty()) {
                    generateSimulatedNotifications()
                }
            }
        }
    }

    // Filtered Feed combining videos, search query, selected categories, and selected channel
    val filteredVideos: StateFlow<List<Video>> = combine(
        allVideos,
        recommendedVideos,
        searchQuery,
        selectedCategory,
        selectedChannelId
    ) { videos, recommended, query, category, channelId ->
        var list = when (category) {
            "Рекомендованные" -> recommended
            "Популярные" -> videos.sortedWith(compareByDescending<Video> { it.views }.thenByDescending { it.likes }.thenByDescending { it.id })
            else -> videos.sortedWith(compareByDescending<Video> { it.uploadDate }.thenByDescending { it.id })
        }

        if (channelId != null) {
            list = list.filter { it.channelId == channelId }
        }

        if (query.isNotEmpty()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun playVideo(video: Video, playlistContext: List<Video>? = null) {
        currentPlaylistVideos.value = playlistContext
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementViews(video.id)
            
            val channel = repository.getChannelById(video.channelId)
            var resumePos = 0L
            if (channel?.isLectorMode == true) {
                val histList = repository.watchHistory.first()
                val hist = histList.find { it.videoId == video.id }
                if (hist != null) {
                    resumePos = hist.lastPosition
                }
            } else {
                repository.addToHistory(video.id, 0L)
            }
            
            withContext(Dispatchers.Main) {
                VideoPlayerManager.playVideo(getApplication(), video)
                if (resumePos > 0) {
                    VideoPlayerManager.seekTo(resumePos)
                }
                isPlayerActive.value = true
                isPlayerExpanded.value = true
            }
        }
    }

    fun togglePlayPause() {
        VideoPlayerManager.togglePlayPause(getApplication())
        saveVideoProgress()
    }

    fun playNextVideo() {
        val playlist = currentPlaylistVideos.value
        val current = VideoPlayerManager.currentVideo.value
        if (current != null) {
            val listToUse = if (!playlist.isNullOrEmpty()) playlist else allVideos.value.filter { !it.isShorts }
            if (listToUse.isNotEmpty()) {
                val currentIndex = listToUse.indexOfFirst { it.id == current.id }
                if (currentIndex != -1) {
                    val nextIndex = (currentIndex + 1) % listToUse.size
                    playVideo(listToUse[nextIndex], playlist)
                } else {
                    playVideo(listToUse[0], playlist)
                }
            }
        }
    }

    fun playPreviousVideo() {
        val playlist = currentPlaylistVideos.value
        val current = VideoPlayerManager.currentVideo.value
        if (current != null) {
            val listToUse = if (!playlist.isNullOrEmpty()) playlist else allVideos.value.filter { !it.isShorts }
            if (listToUse.isNotEmpty()) {
                val currentIndex = listToUse.indexOfFirst { it.id == current.id }
                if (currentIndex != -1) {
                    val prevIndex = if (currentIndex - 1 < 0) listToUse.size - 1 else currentIndex - 1
                    playVideo(listToUse[prevIndex], playlist)
                } else {
                    playVideo(listToUse[0], playlist)
                }
            }
        }
    }

    fun seekTo(position: Long) {
        VideoPlayerManager.seekTo(position)
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        VideoPlayerManager.toggleBackgroundPlayback(getApplication(), enabled)
    }

    fun toggleLike(video: Video) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedVideo = video.copy(
                isLiked = !video.isLiked,
                likes = if (video.isLiked) video.likes - 1 else video.likes + 1
            )
            repository.updateVideo(updatedVideo)
        }
    }

    private fun copyUriToLocalStorage(context: Context, uriString: String, isVideo: Boolean): String {
        if (uriString.startsWith("http://") || uriString.startsWith("https://") || uriString.isBlank()) {
            return uriString
        }
        try {
            val uri = android.net.Uri.parse(uriString)
            val contentResolver = context.contentResolver

            if (uri.scheme == "content") {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Ignore if permission flag cannot be persisted
                }
            }

            // For videos, do NOT copy huge files to internal storage.
            // Using the content URI or file path directly saves disk space and prevents crashes on large videos of any format.
            if (isVideo) {
                return uriString
            }

            val extension = "jpg"
            val fileName = "local_${System.currentTimeMillis()}_${(100..999).random()}.$extension"
            val destFile = File(context.filesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
            return destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return uriString
        }
    }

    suspend fun saveFileToInternalStorage(uriString: String, isVideo: Boolean): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            copyUriToLocalStorage(getApplication(), uriString, isVideo)
        }
    }

    fun toggleRepeat() {
        VideoPlayerManager.toggleRepeat()
    }

    fun toggleAutoplay() {
        VideoPlayerManager.toggleAutoplay()
    }

    fun toggleAdultMode() {
        isAdultMode.value = !isAdultMode.value
        if (isAdultMode.value) {
            uiEvent.value = "Режим 18+ активирован 🤫"
        } else {
            uiEvent.value = "Обычный режим активирован"
        }
    }

    fun createChannel(name: String, description: String, avatar: String, banner: String, subscribers: Int, isAgeRestricted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isBlank()) {
                uiEvent.value = "Имя канала не может быть пустым"
                return@launch
            }
            val localAvatar = if (avatar.isNotBlank()) saveFileToInternalStorage(avatar, isVideo = false) else ""
            val localBanner = if (banner.isNotBlank()) saveFileToInternalStorage(banner, isVideo = false) else ""

            repository.insertChannel(
                Channel(
                    name = name,
                    description = description,
                    avatarUri = localAvatar.ifBlank { "default_avatar" },
                    bannerUri = localBanner.ifBlank { "default_banner" },
                    subscribers = subscribers,
                    isAgeRestricted = isAgeRestricted
                )
            )
            uiEvent.value = "Канал '$name' успешно создан!"
        }
    }

    fun addVideo(
        channelId: Int,
        title: String,
        description: String,
        videoUri: String,
        thumbnailUri: String,
        isAgeRestricted: Boolean = isAdultMode.value,
        isShorts: Boolean = false,
        uploadDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (title.isBlank() || videoUri.isBlank()) {
                uiEvent.value = "Заполните название и укажите файл видео"
                return@launch
            }
            val localVideoPath = saveFileToInternalStorage(videoUri, isVideo = true)
            val localThumbnailPath = if (thumbnailUri.isNotBlank()) saveFileToInternalStorage(thumbnailUri, isVideo = false) else ""
            val finalAgeRestricted = if (isAdultMode.value) true else isAgeRestricted

            // Extract real video duration automatically using MediaMetadataRetriever
            val extractedDuration = try {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    if (localVideoPath.startsWith("content://") || localVideoPath.startsWith("android.resource://") || localVideoPath.startsWith("file://")) {
                        retriever.setDataSource(getApplication(), android.net.Uri.parse(localVideoPath))
                    } else if (localVideoPath.startsWith("http://") || localVideoPath.startsWith("https://")) {
                        retriever.setDataSource(localVideoPath, HashMap<String, String>())
                    } else {
                        retriever.setDataSource(localVideoPath)
                    }
                    val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val timeMs = time?.toLongOrNull() ?: 0L
                    if (timeMs > 0) {
                        val totalSecs = timeMs / 1000
                        val hours = totalSecs / 3600
                        val minutes = (totalSecs % 3600) / 60
                        val seconds = totalSecs % 60
                        if (hours > 0) {
                            String.format("%d:%02d:%02d", hours, minutes, seconds)
                        } else {
                            String.format("%d:%02d", minutes, seconds)
                        }
                    } else {
                        "3:15"
                    }
                } finally {
                    try {
                        retriever.release()
                    } catch (ex: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "3:15"
            }

            repository.insertVideo(
                Video(
                    channelId = channelId,
                    title = title,
                    description = description,
                    videoUri = localVideoPath,
                    thumbnailUri = localThumbnailPath.ifBlank { "default_video" },
                    duration = extractedDuration,
                    views = 0,
                    likes = 0,
                    uploadDate = uploadDate,
                    isAgeRestricted = finalAgeRestricted,
                    isShorts = isShorts
                )
            )
            uiEvent.value = "Видео '$title' добавлено на канал!"
        }
    }

    fun incrementVideoViews(videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.incrementViews(videoId)
        }
    }

    fun deleteVideo(videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = VideoPlayerManager.currentVideo.value
            if (current != null && current.id == videoId) {
                withContext(Dispatchers.Main) {
                    closePlayer()
                }
            }
            val video = repository.getVideoById(videoId)
            if (video != null) {
                if (video.videoUri.isNotBlank()) {
                    try {
                        val file = File(video.videoUri.removePrefix("file://"))
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (video.thumbnailUri.isNotBlank()) {
                    try {
                        val file = File(video.thumbnailUri.removePrefix("file://"))
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            val db = AppDatabase.getDatabase(getApplication())
            db.playlistDao().removeVideoFromAllPlaylists(videoId)
            repository.deleteVideo(videoId)
            withContext(Dispatchers.Main) {
                uiEvent.value = "Видео удалено"
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", coverUri: String = "", channelId: Int = 0, isShorts: Boolean = false, isAgeRestricted: Boolean = isAdultMode.value) {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isBlank()) {
                uiEvent.value = "Название плейлиста не может быть пустым"
                return@launch
            }
            val finalCover = if (coverUri.isNotBlank()) saveFileToInternalStorage(coverUri, isVideo = false) else ""
            repository.insertPlaylist(Playlist(name = name, description = description, coverUri = finalCover, channelId = channelId, isShorts = isShorts, isAgeRestricted = isAgeRestricted))
            uiEvent.value = "Плейлист '$name' создан!"
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playlist.name.isBlank()) {
                uiEvent.value = "Название плейлиста не может быть пустым"
                return@launch
            }
            repository.insertPlaylist(playlist)
            uiEvent.value = "Плейлист обновлен!"
        }
    }

    fun updatePlaylistWithCover(playlist: Playlist, newCoverUri: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playlist.name.isBlank()) {
                uiEvent.value = "Название плейлиста не может быть пустым"
                return@launch
            }
            val finalCover = if (!newCoverUri.isNullOrBlank() && newCoverUri != playlist.coverUri) {
                saveFileToInternalStorage(newCoverUri, isVideo = false)
            } else {
                playlist.coverUri
            }
            repository.insertPlaylist(playlist.copy(coverUri = finalCover))
            uiEvent.value = "Плейлист обновлен!"
        }
    }

    fun reorderPlaylist(playlistId: Int, videoIds: List<Int>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.reorderPlaylist(playlistId, videoIds)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = repository.getPlaylistById(playlistId)
            if (playlist != null && playlist.coverUri.isNotBlank()) {
                try {
                    val file = File(playlist.coverUri.removePrefix("file://"))
                    if (file.exists() && file.isFile) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val db = AppDatabase.getDatabase(getApplication())
            db.playlistDao().clearPlaylistVideos(playlistId)
            repository.deletePlaylist(playlistId)
            withContext(Dispatchers.Main) {
                uiEvent.value = "Плейлист удален"
            }
        }
    }

    fun addVideoToPlaylist(playlistId: Int, videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertPlaylistVideo(playlistId, videoId)
            uiEvent.value = "Видео добавлено в плейлист"
        }
    }

    fun removeVideoFromPlaylist(playlistId: Int, videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeVideoFromPlaylist(playlistId, videoId)
            uiEvent.value = "Видео удалено из плейлиста"
        }
    }

    fun getVideosForPlaylist(playlistId: Int) = repository.getVideosForPlaylist(playlistId)

    fun getCommentsForVideo(videoId: Int) = repository.getCommentsForVideo(videoId)

    fun addComment(videoId: Int, authorName: String, content: String, authorAvatarUri: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            if (content.isBlank()) return@launch
            val author = if (authorName.isBlank()) "Анонимный зритель" else authorName
            repository.insertComment(
                Comment(
                    videoId = videoId,
                    authorName = author,
                    content = content,
                    authorAvatarUri = authorAvatarUri
                )
            )
            uiEvent.value = "Комментарий добавлен!"
        }
    }

    fun deleteComment(commentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId)
            uiEvent.value = "Комментарий удален"
        }
    }

    fun updateChannel(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChannel(channel)
            uiEvent.value = "Канал '${channel.name}' обновлен!"
        }
    }

    fun deleteChannel(channelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = repository.getChannelById(channelId)
            if (channel != null) {
                if (channel.avatarUri.isNotBlank()) {
                    try {
                        val file = File(channel.avatarUri)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (channel.bannerUri.isNotBlank()) {
                    try {
                        val file = File(channel.bannerUri)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            
            val videos = repository.getVideosByChannelList(channelId)
            videos.forEach { video ->
                if (video.videoUri.isNotBlank()) {
                    try {
                        val file = File(video.videoUri)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (video.thumbnailUri.isNotBlank()) {
                    try {
                        val file = File(video.thumbnailUri)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            repository.deleteHistoryForChannel(channelId)
            repository.deleteVideosByChannel(channelId)
            repository.deleteChannel(channelId)
            if (activeChannelDetailId.value == channelId) {
                activeChannelDetailId.value = null
            }
            if (selectedChannelId.value == channelId) {
                selectedChannelId.value = null
            }
            uiEvent.value = "Канал удален"
        }
    }

    fun toggleLectorMode(channel: Channel) {
        val updated = channel.copy(isLectorMode = !channel.isLectorMode)
        updateChannel(updated)
    }

    fun clearVideoProgress(videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryForVideo(videoId)
            uiEvent.value = "Прогресс видео сброшен"
        }
    }

    fun clearChannelProgress(channelId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteHistoryForChannel(channelId)
            uiEvent.value = "Прогресс всех видео канала сброшен"
        }
    }

    fun updateVideo(video: Video) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVideo(video)
            uiEvent.value = "Видео '${video.title}' обновлено!"
        }
    }

    fun createAudioPlaylist(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            if (name.isBlank()) {
                uiEvent.value = "Название плейлиста не может быть пустым"
                return@launch
            }
            repository.insertAudioPlaylist(AudioPlaylist(name = name, description = description))
            uiEvent.value = "Аудио плейлист '$name' создан!"
        }
    }

    fun deleteAudioPlaylist(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            db.audioDao().clearAudioPlaylistTracks(id)
            repository.deleteAudioPlaylist(id)
            withContext(Dispatchers.Main) {
                uiEvent.value = "Аудио плейлист удален"
            }
        }
    }

    fun addTrackToAudioPlaylist(playlistId: Int, trackId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAudioPlaylistTrack(playlistId, trackId)
            uiEvent.value = "Аудио добавлено в плейлист"
        }
    }

    fun removeTrackFromAudioPlaylist(playlistId: Int, trackId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeTrackFromPlaylist(playlistId, trackId)
            uiEvent.value = "Аудио удалено из плейлиста"
        }
    }

    fun getTracksForAudioPlaylist(playlistId: Int): Flow<List<AudioTrack>> {
        return repository.getTracksForPlaylist(playlistId)
    }

    fun addAudioTrack(title: String, artist: String, audioUri: String, coverUri: String = "", year: Int = 2026) {
        viewModelScope.launch(Dispatchers.IO) {
            if (title.isBlank()) {
                uiEvent.value = "Название аудио не может быть пустым"
                return@launch
            }
            val localAudioPath = saveFileToInternalStorage(audioUri, isVideo = false)
            val localCoverPath = if (coverUri.isNotBlank()) {
                saveFileToInternalStorage(coverUri, isVideo = false)
            } else {
                ""
            }
            
            val extractedDuration = try {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    retriever.setDataSource(localAudioPath)
                    val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val timeMs = time?.toLongOrNull() ?: 0L
                    if (timeMs > 0) {
                        val totalSecs = timeMs / 1000
                        val minutes = totalSecs / 60
                        val seconds = totalSecs % 60
                        String.format("%d:%02d", minutes, seconds)
                    } else {
                        "3:15"
                    }
                } finally {
                    try { retriever.release() } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                "3:15"
            }

            repository.insertAudioTrack(
                AudioTrack(
                    title = title,
                    artist = artist,
                    audioUri = localAudioPath,
                    duration = extractedDuration,
                    coverUri = localCoverPath,
                    year = year
                )
            )
            uiEvent.value = "Аудиофайл '$title' добавлен!"
        }
    }

    fun deleteAudioTrack(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val track = repository.getAudioTrackById(id)
            if (track != null) {
                if (track.audioUri.isNotBlank()) {
                    try {
                        val file = File(track.audioUri.removePrefix("file://"))
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (track.coverUri.isNotBlank()) {
                    try {
                        val file = File(track.coverUri.removePrefix("file://"))
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                if (com.example.service.AudioPlayerManager.currentTrack.value?.id == id) {
                    withContext(Dispatchers.Main) {
                        com.example.service.AudioPlayerManager.stop()
                    }
                }
            }
            val db = AppDatabase.getDatabase(getApplication())
            db.audioDao().deleteAudioPlaylistTrackByTrack(id)
            repository.deleteAudioTrack(id)
            withContext(Dispatchers.Main) {
                uiEvent.value = "Аудио удалено"
            }
        }
    }

    fun getChaptersForManga(mangaId: Int): Flow<List<MangaChapter>> {
        val db = AppDatabase.getDatabase(getApplication())
        return db.mangaDao().getChaptersForManga(mangaId)
    }

    fun getProgressForManga(mangaId: Int): Flow<MangaReadingProgress?> {
        val db = AppDatabase.getDatabase(getApplication())
        return db.mangaDao().getProgressForManga(mangaId)
    }

    fun saveMangaProgress(mangaId: Int, lastReadChapterId: Int, lastReadPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            db.mangaDao().saveProgress(
                MangaReadingProgress(
                    mangaId = mangaId,
                    lastReadChapterId = lastReadChapterId,
                    lastReadPage = lastReadPage,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addManga(
        title: String,
        author: String,
        description: String,
        coverUri: String,
        genre: String,
        rating: Float,
        isCompleted: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (title.isBlank()) {
                uiEvent.value = "Название манги не может быть пустым"
                return@launch
            }
            val localCover = if (coverUri.isNotBlank()) saveFileToInternalStorage(coverUri, isVideo = false) else ""
            val db = AppDatabase.getDatabase(getApplication())
            db.mangaDao().insertManga(
                Manga(
                    title = title,
                    author = author.ifBlank { "Неизвестен" },
                    description = description,
                    coverUri = localCover,
                    rating = rating,
                    isCompleted = isCompleted,
                    genre = genre.ifBlank { "Романтика, Драма" }
                )
            )
            uiEvent.value = "Манга '$title' добавлена!"
        }
    }

    fun addMangaChapter(
        mangaId: Int,
        title: String,
        chapterNumber: Int,
        pageUris: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (title.isBlank()) {
                uiEvent.value = "Название главы не может быть пустым"
                return@launch
            }
            if (pageUris.isEmpty()) {
                uiEvent.value = "Выберите хотя бы одну страницу"
                return@launch
            }
            val localPages = pageUris.map { uri ->
                saveFileToInternalStorage(uri, isVideo = false)
            }
            val pagesCsv = localPages.joinToString(",")
            val db = AppDatabase.getDatabase(getApplication())
            db.mangaDao().insertChapter(
                MangaChapter(
                    mangaId = mangaId,
                    title = title,
                    chapterNumber = chapterNumber,
                    pageCount = localPages.size,
                    pagesCsv = pagesCsv
                )
            )
            uiEvent.value = "Глава '$title' успешно добавлена!"
        }
    }

    fun deleteManga(mangaId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(getApplication())
            val manga = db.mangaDao().getMangaById(mangaId)
            if (manga != null) {
                if (manga.coverUri.isNotBlank()) {
                    try {
                        val file = File(manga.coverUri)
                        if (file.exists() && file.isFile) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val chapters = db.mangaDao().getChaptersForMangaList(mangaId)
                chapters.forEach { chapter ->
                    if (chapter.pagesCsv.isNotBlank()) {
                        chapter.pagesCsv.split(",").forEach { path ->
                            if (path.isNotBlank()) {
                                try {
                                    val file = File(path)
                                    if (file.exists() && file.isFile) {
                                        file.delete()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                db.mangaDao().deleteManga(mangaId)
                uiEvent.value = "Манга '${manga.title}' удалена"
            } else {
                db.mangaDao().deleteManga(mangaId)
                uiEvent.value = "Манга удалена"
            }
        }
    }

    fun generateSimulatedNotifications() {
        viewModelScope.launch(Dispatchers.Default) {
            val videosList = rawVideos.value.filter { !it.isAgeRestricted }
            val channelsList = rawChannels.value.filter { !it.isAgeRestricted }
            if (videosList.isNotEmpty() && channelsList.isNotEmpty()) {
                val list = mutableListOf<AppNotification>()
                
                videosList.find { it.title.contains("Юпитер") }?.let { video ->
                    val channel = channelsList.find { it.id == video.channelId }
                    list.add(
                        AppNotification(
                            id = 1,
                            title = "Рекомендация дня ✨",
                            text = "Канал '${channel?.name ?: "Космос Плюс"}' рекомендует к просмотру: '${video.title}'",
                            videoId = video.id,
                            channelName = channel?.name ?: "Космос Плюс",
                            channelAvatar = channel?.avatarUri ?: "space_avatar",
                            timestamp = System.currentTimeMillis() - 3600000
                        )
                    )
                }

                videosList.find { it.title.contains("лес") }?.let { video ->
                    val channel = channelsList.find { it.id == video.channelId }
                    list.add(
                        AppNotification(
                            id = 2,
                            title = "Видео дня от подписки 🌿",
                            text = "Вам может понравиться расслабляющее видео: '${video.title}'",
                            videoId = video.id,
                            channelName = channel?.name ?: "Дикая Природа",
                            channelAvatar = channel?.avatarUri ?: "nature_avatar",
                            timestamp = System.currentTimeMillis() - 7200000
                        )
                    )
                }

                videosList.find { it.title.contains("Лоу-Фай") }?.let { video ->
                    val channel = channelsList.find { it.id == video.channelId }
                    list.add(
                        AppNotification(
                            id = 3,
                            title = "Музыка для фокуса 🎵",
                            text = "Слушайте '${video.title}' во время учебы или отдыха.",
                            videoId = video.id,
                            channelName = channel?.name ?: "Музыкальный Дзен",
                            channelAvatar = channel?.avatarUri ?: "dzen_avatar",
                            timestamp = System.currentTimeMillis() - 14400000
                        )
                    )
                }
                notifications.value = list
            }
        }
    }

    fun simulateNewNotification() {
        viewModelScope.launch(Dispatchers.Default) {
            val videosList = rawVideos.value.filter { !it.isAgeRestricted }
            val channelsList = rawChannels.value.filter { !it.isAgeRestricted }
            if (videosList.isNotEmpty() && channelsList.isNotEmpty()) {
                val randomVideo = videosList.random()
                val channel = channelsList.find { it.id == randomVideo.channelId }
                val nextId = (notifications.value.maxOfOrNull { it.id } ?: 0) + 1
                val newNotif = AppNotification(
                    id = nextId,
                    title = "Новое видео сегодня! 🔔",
                    text = "Новый хит от '${channel?.name ?: "Авторы"}': '${randomVideo.title}'",
                    videoId = randomVideo.id,
                    channelName = channel?.name ?: "Автор",
                    channelAvatar = channel?.avatarUri ?: "",
                    timestamp = System.currentTimeMillis()
                )
                notifications.value = listOf(newNotif) + notifications.value
                uiEvent.value = "Получено новое уведомление!"
            }
        }
    }

    fun markNotificationAsRead(id: Int) {
        notifications.value = notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    fun clearAllNotifications() {
        notifications.value = emptyList()
    }

    fun clearUiEvent() {
        uiEvent.value = null
    }

    fun exportBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                
                // Export Channels directly from Flow
                val channelsList = repository.allChannels.first()
                val channelsArr = JSONArray()
                channelsList.forEach { channel ->
                    val cObj = JSONObject().apply {
                        put("id", channel.id)
                        put("name", channel.name)
                        put("subscribers", channel.subscribers)
                        put("avatarUri", channel.avatarUri)
                        put("bannerUri", channel.bannerUri)
                        put("description", channel.description)
                        put("isLectorMode", channel.isLectorMode)
                        put("isAgeRestricted", channel.isAgeRestricted)
                    }
                    channelsArr.put(cObj)
                }
                json.put("channels", channelsArr)

                // Export Videos directly from Flow
                val videosList = repository.allVideos.first()
                val videosArr = JSONArray()
                videosList.forEach { video ->
                    val vObj = JSONObject().apply {
                        put("id", video.id)
                        put("channelId", video.channelId)
                        put("title", video.title)
                        put("description", video.description)
                        put("videoUri", video.videoUri)
                        put("thumbnailUri", video.thumbnailUri)
                        put("views", video.views)
                        put("likes", video.likes)
                        put("duration", video.duration)
                        put("uploadDate", video.uploadDate)
                        put("isLiked", video.isLiked)
                        put("isAgeRestricted", video.isAgeRestricted)
                        put("isShorts", video.isShorts)
                    }
                    videosArr.put(vObj)
                }
                json.put("videos", videosArr)

                // Export Playlists directly from Flow
                val playlistsList = repository.allPlaylists.first()
                val playlistsArr = JSONArray()
                playlistsList.forEach { playlist ->
                    val pObj = JSONObject().apply {
                        put("id", playlist.id)
                        put("name", playlist.name)
                        put("description", playlist.description)
                        put("coverUri", playlist.coverUri)
                        put("channelId", playlist.channelId)
                        put("isShorts", playlist.isShorts)
                        put("isAgeRestricted", playlist.isAgeRestricted)
                    }
                    playlistsArr.put(pObj)
                }
                json.put("playlists", playlistsArr)

                // Export PlaylistVideos directly from Flow
                val playlistVideosList = repository.allPlaylistVideos.first()
                val playlistVideosArr = JSONArray()
                playlistVideosList.forEach { pv ->
                    val pvObj = JSONObject().apply {
                        put("playlistId", pv.playlistId)
                        put("videoId", pv.videoId)
                    }
                    playlistVideosArr.put(pvObj)
                }
                json.put("playlistVideos", playlistVideosArr)

                // Export Audio Tracks directly from Flow
                val audioTracksList = repository.allAudioTracks.first()
                val audioArr = JSONArray()
                audioTracksList.forEach { track ->
                    val aObj = JSONObject().apply {
                        put("id", track.id)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("year", track.year)
                        put("duration", track.duration)
                        put("audioUri", track.audioUri)
                        put("coverUri", track.coverUri)
                    }
                    audioArr.put(aObj)
                }
                json.put("audioTracks", audioArr)

                // Export Audio Playlists directly from Flow
                val audioPlaylistsList = repository.allAudioPlaylists.first()
                val audioPlaylistsArr = JSONArray()
                audioPlaylistsList.forEach { apl ->
                    val apObj = JSONObject().apply {
                        put("id", apl.id)
                        put("name", apl.name)
                        put("description", apl.description)
                    }
                    audioPlaylistsArr.put(apObj)
                }
                json.put("audioPlaylists", audioPlaylistsArr)

                // Export AudioPlaylistTracks directly from Flow
                val audioPlaylistTracksList = repository.allAudioPlaylistTracks.first()
                val audioPlaylistTracksArr = JSONArray()
                audioPlaylistTracksList.forEach { apt ->
                    val aptObj = JSONObject().apply {
                        put("playlistId", apt.playlistId)
                        put("trackId", apt.trackId)
                    }
                    audioPlaylistTracksArr.put(aptObj)
                }
                json.put("audioPlaylistTracks", audioPlaylistTracksArr)

                // Export Manga directly from Flow
                val db = AppDatabase.getDatabase(getApplication())
                val mangaDao = db.mangaDao()
                val mangaList = mangaDao.getAllManga().first()
                val mangaArr = JSONArray()
                mangaList.forEach { manga ->
                    val mObj = JSONObject().apply {
                        put("id", manga.id)
                        put("title", manga.title)
                        put("author", manga.author)
                        put("description", manga.description)
                        put("coverUri", manga.coverUri)
                        put("rating", manga.rating)
                        put("isCompleted", manga.isCompleted)
                        put("genre", manga.genre)
                    }
                    mangaArr.put(mObj)
                }
                json.put("mangas", mangaArr)

                // Save to file in Downloads
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val backupFile = File(downloadsDir, "OfflineTube_Backup.json")
                backupFile.writeText(json.toString(2))
                
                uiEvent.value = "Бэкап сохранен в Загрузки: OfflineTube_Backup.json"
            } catch (e: Exception) {
                uiEvent.value = "Ошибка бэкапа: ${e.localizedMessage}"
            }
        }
    }

    fun importBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val backupFile = File(downloadsDir, "OfflineTube_Backup.json")
                if (!backupFile.exists()) {
                    uiEvent.value = "Файл OfflineTube_Backup.json не найден в Загрузках!"
                    return@launch
                }

                val jsonStr = backupFile.readText()
                val json = JSONObject(jsonStr)

                // Import Channels
                val channelsArr = json.optJSONArray("channels")
                if (channelsArr != null) {
                    for (i in 0 until channelsArr.length()) {
                        val cObj = channelsArr.getJSONObject(i)
                        val channel = Channel(
                            id = cObj.optInt("id", 0),
                            name = cObj.optString("name", "Канал"),
                            subscribers = cObj.optInt("subscribers", 0),
                            avatarUri = cObj.optString("avatarUri", ""),
                            bannerUri = cObj.optString("bannerUri", ""),
                            description = cObj.optString("description", ""),
                            isLectorMode = cObj.optBoolean("isLectorMode", false),
                            isAgeRestricted = cObj.optBoolean("isAgeRestricted", false)
                        )
                        repository.insertChannel(channel)
                    }
                }

                // Import Videos
                val videosArr = json.optJSONArray("videos")
                if (videosArr != null) {
                    for (i in 0 until videosArr.length()) {
                        val vObj = videosArr.getJSONObject(i)
                        val video = Video(
                            id = vObj.optInt("id", 0),
                            channelId = vObj.optInt("channelId", 1),
                            title = vObj.optString("title", "Видео"),
                            description = vObj.optString("description", ""),
                            videoUri = vObj.optString("videoUri", ""),
                            thumbnailUri = vObj.optString("thumbnailUri", ""),
                            views = vObj.optInt("views", 0),
                            likes = vObj.optInt("likes", 0),
                            duration = vObj.optString("duration", "0:00"),
                            uploadDate = vObj.optLong("uploadDate", System.currentTimeMillis()),
                            isLiked = vObj.optBoolean("isLiked", false),
                            isAgeRestricted = vObj.optBoolean("isAgeRestricted", false),
                            isShorts = vObj.optBoolean("isShorts", false)
                        )
                        repository.insertVideo(video)
                    }
                }

                // Import Playlists
                val playlistsArr = json.optJSONArray("playlists")
                if (playlistsArr != null) {
                    for (i in 0 until playlistsArr.length()) {
                        val pObj = playlistsArr.getJSONObject(i)
                        val playlist = Playlist(
                            id = pObj.optInt("id", 0),
                            name = pObj.optString("name", "Плейлист"),
                            description = pObj.optString("description", ""),
                            coverUri = pObj.optString("coverUri", ""),
                            channelId = pObj.optInt("channelId", 0),
                            isShorts = pObj.optBoolean("isShorts", false),
                            isAgeRestricted = pObj.optBoolean("isAgeRestricted", false)
                        )
                        repository.insertPlaylist(playlist)
                    }
                }

                // Import Playlist Videos cross-reference
                val playlistVideosArr = json.optJSONArray("playlistVideos")
                if (playlistVideosArr != null) {
                    for (i in 0 until playlistVideosArr.length()) {
                        val pvObj = playlistVideosArr.getJSONObject(i)
                        val pId = pvObj.optInt("playlistId", 0)
                        val vId = pvObj.optInt("videoId", 0)
                        if (pId > 0 && vId > 0) {
                            repository.insertPlaylistVideo(pId, vId)
                        }
                    }
                }

                // Import Audio Tracks
                val audioArr = json.optJSONArray("audioTracks") ?: json.optJSONArray("tracks") ?: json.optJSONArray("music")
                if (audioArr != null) {
                    for (i in 0 until audioArr.length()) {
                        val aObj = audioArr.getJSONObject(i)
                        val track = AudioTrack(
                            id = aObj.optInt("id", 0),
                            title = aObj.optString("title", "Трек"),
                            artist = aObj.optString("artist", "Исполнитель"),
                            year = aObj.optInt("year", 2026),
                            duration = aObj.optString("duration", "3:00"),
                            audioUri = aObj.optString("audioUri", ""),
                            coverUri = aObj.optString("coverUri", "")
                        )
                        repository.insertAudioTrack(track)
                    }
                }

                // Import Audio Playlists
                val audioPlaylistsArr = json.optJSONArray("audioPlaylists")
                if (audioPlaylistsArr != null) {
                    for (i in 0 until audioPlaylistsArr.length()) {
                        val apObj = audioPlaylistsArr.getJSONObject(i)
                        val apl = AudioPlaylist(
                            id = apObj.optInt("id", 0),
                            name = apObj.optString("name", "Плейлист"),
                            description = apObj.optString("description", "")
                        )
                        repository.insertAudioPlaylist(apl)
                    }
                }

                // Import Audio Playlist Tracks cross-reference
                val audioPlaylistTracksArr = json.optJSONArray("audioPlaylistTracks")
                if (audioPlaylistTracksArr != null) {
                    for (i in 0 until audioPlaylistTracksArr.length()) {
                        val aptObj = audioPlaylistTracksArr.getJSONObject(i)
                        val pId = aptObj.optInt("playlistId", 0)
                        val tId = aptObj.optInt("trackId", 0)
                        if (pId > 0 && tId > 0) {
                            repository.insertAudioPlaylistTrack(pId, tId)
                        }
                    }
                }

                // Import Manga
                val mangaArr = json.optJSONArray("mangas")
                if (mangaArr != null) {
                    val db = AppDatabase.getDatabase(getApplication())
                    val mangaDao = db.mangaDao()
                    for (i in 0 until mangaArr.length()) {
                        val mObj = mangaArr.getJSONObject(i)
                        val manga = Manga(
                            id = mObj.optInt("id", 0),
                            title = mObj.optString("title", "Манга"),
                            author = mObj.optString("author", "Неизвестен"),
                            description = mObj.optString("description", ""),
                            coverUri = mObj.optString("coverUri", ""),
                            genre = mObj.optString("genre", ""),
                            rating = mObj.optDouble("rating", 4.5).toFloat(),
                            isCompleted = mObj.optBoolean("isCompleted", false)
                        )
                        mangaDao.insertManga(manga)
                    }
                }

                uiEvent.value = "Данные успешно восстановлены из бэкапа!"
            } catch (e: Exception) {
                uiEvent.value = "Ошибка импорта бэкапа: ${e.localizedMessage}"
            }
        }
    }

    fun cleanupOrphanFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val usedPaths = mutableSetOf<String>()

                // Channels
                val channels = db.channelDao().getAllChannels().first()
                channels.forEach { c ->
                    if (c.avatarUri.isNotBlank()) usedPaths.add(c.avatarUri)
                    if (c.bannerUri.isNotBlank()) usedPaths.add(c.bannerUri)
                }

                // Videos
                val videos = db.videoDao().getAllVideos().first()
                videos.forEach { v ->
                    if (v.videoUri.isNotBlank()) usedPaths.add(v.videoUri)
                    if (v.thumbnailUri.isNotBlank()) usedPaths.add(v.thumbnailUri)
                }

                // Playlists
                val playlists = db.playlistDao().getAllPlaylists().first()
                playlists.forEach { p ->
                    if (p.coverUri.isNotBlank()) usedPaths.add(p.coverUri)
                }

                // Audio Tracks
                val audioTracks = db.audioDao().getAllAudioTracks().first()
                audioTracks.forEach { a ->
                    if (a.audioUri.isNotBlank()) usedPaths.add(a.audioUri)
                    if (a.coverUri.isNotBlank()) usedPaths.add(a.coverUri)
                }

                // Manga
                val mangas = db.mangaDao().getAllManga().first()
                mangas.forEach { m ->
                    if (m.coverUri.isNotBlank()) usedPaths.add(m.coverUri)
                    val chapters = db.mangaDao().getChaptersForManga(m.id).first()
                    chapters.forEach { ch ->
                        if (ch.pagesCsv.isNotBlank()) {
                            ch.pagesCsv.split(",").forEach { page ->
                                if (page.isNotBlank()) usedPaths.add(page)
                            }
                        }
                    }
                }

                val filesDir = getApplication<Application>().filesDir
                var deletedCount = 0
                var freedBytes = 0L

                fun scanAndClean(dir: File) {
                    val contents = dir.listFiles() ?: return
                    for (f in contents) {
                        if (f.isDirectory) {
                            scanAndClean(f)
                            if (f.listFiles()?.isEmpty() == true) {
                                f.delete()
                            }
                        } else if (f.isFile) {
                            val absPath = f.absolutePath
                            val canonicalPath = f.canonicalPath
                            val fileName = f.name

                            val isReferenced = usedPaths.contains(absPath) ||
                                    usedPaths.contains("file://$absPath") ||
                                    usedPaths.contains(canonicalPath) ||
                                    usedPaths.contains("file://$canonicalPath") ||
                                    usedPaths.any { it.contains(fileName) }

                            if (!isReferenced) {
                                val size = f.length()
                                if (f.delete()) {
                                    deletedCount++
                                    freedBytes += size
                                }
                            }
                        }
                    }
                }

                scanAndClean(filesDir)

                val freedMb = String.format(Locale.getDefault(), "%.1f", freedBytes / (1024f * 1024f))
                withContext(Dispatchers.Main) {
                    uiEvent.value = if (deletedCount > 0) {
                        "Очистка завершена: удалено $deletedCount неиспользуемых файлов ($freedMb МБ)"
                    } else {
                        "Память чиста: неиспользуемых файлов не обнаружено!"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    uiEvent.value = "Ошибка при очистке: ${e.localizedMessage}"
                }
            }
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OfflineTubeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return OfflineTubeViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

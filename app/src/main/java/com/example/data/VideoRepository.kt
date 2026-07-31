package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class VideoRepository(private val db: AppDatabase) {

    val allChannels: Flow<List<Channel>> = db.channelDao().getAllChannels()
    val allVideos: Flow<List<Video>> = db.videoDao().getAllVideos()
    val watchHistory: Flow<List<WatchHistory>> = db.watchHistoryDao().getWatchHistory()
    val allPlaylists: Flow<List<Playlist>> = db.playlistDao().getAllPlaylists()
    val allPlaylistVideos: Flow<List<PlaylistVideo>> = db.playlistDao().getAllPlaylistVideos()
    
    val allAudioTracks: Flow<List<AudioTrack>> = db.audioDao().getAllAudioTracks()
    val allAudioPlaylists: Flow<List<AudioPlaylist>> = db.audioDao().getAllAudioPlaylists()
    val allAudioPlaylistTracks: Flow<List<AudioPlaylistTrack>> = db.audioDao().getAllAudioPlaylistTracks()

    suspend fun getChannelById(id: Int): Channel? = db.channelDao().getChannelById(id)
    suspend fun insertChannel(channel: Channel): Long = db.channelDao().insertChannel(channel)
    suspend fun updateChannel(channel: Channel) = db.channelDao().updateChannel(channel)
    suspend fun deleteChannel(id: Int) = db.channelDao().deleteChannel(id)
    suspend fun getVideosByChannelList(channelId: Int): List<Video> = db.videoDao().getVideosByChannelList(channelId)
    suspend fun deleteVideosByChannel(channelId: Int) = db.videoDao().deleteVideosByChannel(channelId)

    suspend fun getVideoById(id: Int): Video? = db.videoDao().getVideoById(id)
    suspend fun insertVideo(video: Video): Long = db.videoDao().insertVideo(video)
    suspend fun updateVideo(video: Video) = db.videoDao().updateVideo(video)
    suspend fun incrementViews(id: Int) = db.videoDao().incrementViews(id)
    suspend fun deleteVideo(id: Int) = db.videoDao().deleteVideo(id)

    suspend fun addToHistory(videoId: Int, progress: Long) {
        db.watchHistoryDao().deleteHistoryForVideo(videoId)
        db.watchHistoryDao().insertHistory(
            WatchHistory(videoId = videoId, watchedAt = System.currentTimeMillis(), lastPosition = progress)
        )
    }

    suspend fun deleteHistoryForVideo(videoId: Int) {
        db.watchHistoryDao().deleteHistoryForVideo(videoId)
    }

    suspend fun deleteHistoryForChannel(channelId: Int) {
        db.watchHistoryDao().deleteHistoryForChannel(channelId)
    }

    fun getVideosByChannel(channelId: Int): Flow<List<Video>> = db.videoDao().getVideosByChannel(channelId)

    fun getCommentsForVideo(videoId: Int): Flow<List<Comment>> = db.commentDao().getCommentsForVideo(videoId)
    suspend fun insertComment(comment: Comment): Long = db.commentDao().insertComment(comment)
    suspend fun deleteComment(id: Int) = db.commentDao().deleteComment(id)

    fun getVideosForPlaylist(playlistId: Int): Flow<List<Video>> = db.playlistDao().getVideosForPlaylist(playlistId)
    suspend fun insertPlaylist(playlist: Playlist): Long = db.playlistDao().insertPlaylist(playlist)
    suspend fun getPlaylistById(id: Int): Playlist? = db.playlistDao().getPlaylistById(id)
    suspend fun deletePlaylist(id: Int) = db.playlistDao().deletePlaylist(id)
    suspend fun insertPlaylistVideo(playlistId: Int, videoId: Int): Long = 
        db.playlistDao().insertPlaylistVideo(PlaylistVideo(playlistId = playlistId, videoId = videoId))
    suspend fun removeVideoFromPlaylist(playlistId: Int, videoId: Int) = 
        db.playlistDao().removeVideoFromPlaylist(playlistId, videoId)

    suspend fun reorderPlaylist(playlistId: Int, videoIds: List<Int>) {
        db.playlistDao().clearPlaylistVideos(playlistId)
        videoIds.forEach { videoId ->
            db.playlistDao().insertPlaylistVideo(PlaylistVideo(playlistId = playlistId, videoId = videoId))
        }
    }

    suspend fun insertAudioTrack(track: AudioTrack): Long = db.audioDao().insertAudioTrack(track)
    suspend fun getAudioTrackById(id: Int): AudioTrack? = db.audioDao().getAudioTrackById(id)
    suspend fun deleteAudioTrack(id: Int) = db.audioDao().deleteAudioTrack(id)
    suspend fun insertAudioPlaylist(playlist: AudioPlaylist): Long = db.audioDao().insertAudioPlaylist(playlist)
    suspend fun deleteAudioPlaylist(id: Int) = db.audioDao().deleteAudioPlaylist(id)
    suspend fun insertAudioPlaylistTrack(playlistId: Int, trackId: Int): Long = 
        db.audioDao().insertAudioPlaylistTrack(AudioPlaylistTrack(playlistId = playlistId, trackId = trackId))
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int) = 
        db.audioDao().removeTrackFromPlaylist(playlistId, trackId)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<AudioTrack>> = db.audioDao().getTracksForPlaylist(playlistId)

    /**
     * Offline recommendation algorithm:
     * Calculates a weight score for each video based on:
     * 1. Recency (uploadDate): Newer videos get higher weight.
     * 2. View Count: Popular offline videos get secondary boost.
     * 3. Channel Affinity: If user has liked/watched videos from this channel, boost other videos from same channel.
     * 4. Watch Progress: Incomplete videos get a slight nudge so user can resume.
     * Hides age restricted videos from standard recommendations.
     */
    val recommendedVideos: Flow<List<Video>> = combine(allVideos, allChannels, watchHistory) { videos, channels, history ->
        val likedChannelIds = videos.filter { it.isLiked }.map { it.channelId }.toSet()
        val historyChannelIds = history.mapNotNull { hist ->
            videos.find { it.id == hist.videoId }?.channelId
        }.toSet()

        val currentTime = System.currentTimeMillis()

        videos.filter { !it.isAgeRestricted }.map { video ->
            var score = 100.0

            // 1. Recency Decay (e.g. up to 500 bonus points for extremely new videos, decaying over 7 days)
            val ageMs = currentTime - video.uploadDate
            val ageHours = ageMs / (1000.0 * 60 * 60)
            val recencyBonus = if (ageHours < 168) {
                (168 - ageHours) * 3.0 // New videos get up to 500 points
            } else {
                0.0
            }
            score += recencyBonus

            // 2. Popularity (views)
            score += video.views * 0.5

            // 3. Likes
            score += video.likes * 5.0

            // 4. Channel Affinity (liked or watched this channel's content)
            if (likedChannelIds.contains(video.channelId)) {
                score += 150.0
            }
            if (historyChannelIds.contains(video.channelId)) {
                score += 80.0
            }

            // 5. Nudge incomplete watches
            val hasRecentHistory = history.any { it.videoId == video.id }
            if (hasRecentHistory) {
                // Slightly lower score to prioritize other recommendations, but not completely hidden
                score += 30.0
            } else {
                // Completely unwatched gets a fresh recommendation boost
                score += 50.0
            }

            video to score
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }

    /**
     * Pre-populate standard offline channels and mock/default entries if database is empty.
     */
    suspend fun checkAndPrepopulate() {
        val existingChannels = allChannels.first()
        if (existingChannels.isEmpty()) {
            val spaceChannelId = insertChannel(
                Channel(
                    name = "Космос Плюс",
                    description = "Удивительные видео о тайнах вселенной, планетах и далеких галактиках.",
                    avatarUri = "space_avatar",
                    bannerUri = "space_banner",
                    subscribers = 12500
                )
            ).toInt()

            val natureChannelId = insertChannel(
                Channel(
                    name = "Дикая Природа",
                    description = "Красивые расслабляющие кадры диких лесов, океанов и обитателей нашей планеты.",
                    avatarUri = "nature_avatar",
                    bannerUri = "nature_banner",
                    subscribers = 43000
                )
            ).toInt()

            val dzenChannelId = insertChannel(
                Channel(
                    name = "Музыкальный Дзен",
                    description = "Эмбиент-мелодии и лоу-фай музыка для полной концентрации и медитации.",
                    avatarUri = "dzen_avatar",
                    bannerUri = "dzen_banner",
                    subscribers = 8500
                )
            ).toInt()

            val adultChannelId = insertChannel(
                Channel(
                    name = "Ночной Чат 18+",
                    description = "Вечерние подкасты, разбор страшных историй и обсуждения исключительно для взрослой аудитории.",
                    avatarUri = "adult_avatar",
                    bannerUri = "adult_banner",
                    subscribers = 9400,
                    isAgeRestricted = true
                )
            ).toInt()

            // Prepopulate some default online/stream fallbacks for instant playability
            insertVideo(
                Video(
                    channelId = spaceChannelId,
                    title = "Секреты Юпитера и его Спутников",
                    description = "Исследование крупнейшего газового гиганта нашей солнечной системы и его удивительного спутника Европы.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    thumbnailUri = "jupiter",
                    views = 1520,
                    likes = 124,
                    duration = "9:56",
                    uploadDate = System.currentTimeMillis() - 3600000 * 2 // 2 hours ago
                )
            )

            insertVideo(
                Video(
                    channelId = natureChannelId,
                    title = "Утренний лес в тумане: 4К релакс",
                    description = "Присядьте, закройте глаза и вдохните прохладный лесной воздух с пением птиц и журчанием ручья.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    thumbnailUri = "forest",
                    views = 4280,
                    likes = 398,
                    duration = "10:53",
                    uploadDate = System.currentTimeMillis() - 3600000 * 12 // 12 hours ago
                )
            )

            insertVideo(
                Video(
                    channelId = dzenChannelId,
                    title = "Лоу-Фай Бит для Кодинга и Чтения",
                    description = "Музыкальный плейлист с мягким шумом дождя для повышения продуктивности.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    thumbnailUri = "lofi",
                    views = 890,
                    likes = 72,
                    duration = "0:15",
                    uploadDate = System.currentTimeMillis() - 3600000 * 48, // 2 days ago
                    isShorts = true
                )
            )

            insertVideo(
                Video(
                    channelId = natureChannelId,
                    title = "Дикая природа: Величественный водопад 🌊",
                    description = "Красивые расслабляющие кадры горного водопада в вертикальном формате YouTube Shorts.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    thumbnailUri = "waterfall_shorts",
                    views = 1250,
                    likes = 98,
                    duration = "0:45",
                    uploadDate = System.currentTimeMillis() - 3600000 * 3, // 3 hours ago
                    isShorts = true
                )
            )

            insertVideo(
                Video(
                    channelId = spaceChannelId,
                    title = "Запуск Ракеты в космос 🚀",
                    description = "Вертикальный ролик о запуске суборбитальной ракеты за пределы атмосферы Земли.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    thumbnailUri = "rocket_shorts",
                    views = 5400,
                    likes = 412,
                    duration = "0:30",
                    uploadDate = System.currentTimeMillis() - 3600000 * 4, // 4 hours ago
                    isShorts = true
                )
            )

            insertVideo(
                Video(
                    channelId = adultChannelId,
                    title = "Ночные страшные истории перед сном [18+]",
                    description = "Озвучка пугающих крипипаст и мистических происшествий. Видео содержит страшные сцены и предназначено исключительно для взрослой аудитории.",
                    videoUri = "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    thumbnailUri = "adult_scary",
                    views = 13500,
                    likes = 1420,
                    duration = "0:52",
                    uploadDate = System.currentTimeMillis() - 3600000 * 1, // 1 hour ago
                    isAgeRestricted = true
                )
            )
        }

        // Prepopulate Manga if empty
        val mangaDao = db.mangaDao()
        val existingMangas = mangaDao.getAllManga().first()
        if (existingMangas.isEmpty()) {
            val m1 = mangaDao.insertManga(
                Manga(
                    title = "Тайная любовь горничной 🖤",
                    author = "Аои Миямото",
                    description = "Красивая и запретная история о девушке, вынужденной скрывать свои чувства к наследнику крупного конгломерата. Тайны прошлого и страстные клятвы.",
                    coverUri = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=400&auto=format&fit=crop&q=80",
                    rating = 4.9f,
                    isCompleted = false,
                    genre = "Романтика, Драма, Повседневность"
                )
            ).toInt()

            val m2 = mangaDao.insertManga(
                Manga(
                    title = "Сладкое Искушение 🍓",
                    author = "Хироси Сато",
                    description = "Романтическая комедия о шеф-кондитере престижного ресторана и его новой строптивой помощнице. На кухне становится слишком жарко, и дело далеко не в духовке!",
                    coverUri = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=400&auto=format&fit=crop&q=80",
                    rating = 4.7f,
                    isCompleted = true,
                    genre = "Романтика, Комедия, Этти"
                )
            ).toInt()

            val m3 = mangaDao.insertManga(
                Manga(
                    title = "Полуночный Шепот ✨",
                    author = "Кэй Танака",
                    description = "Взрослая психологическая драма о двух одиноких душах, случайно встретившихся в ночном Токио. Их связывают общие шрамы и негласные секреты, которые они открывают лишь после полуночи.",
                    coverUri = "https://images.unsplash.com/photo-1560942485-b2a11cc13456?w=400&auto=format&fit=crop&q=80",
                    rating = 4.8f,
                    isCompleted = false,
                    genre = "Драма, Психология, Эротика"
                )
            ).toInt()

            val titlesM1 = listOf("Глава 1: Случайная встреча", "Глава 2: Скрытое влечение", "Глава 3: Тень подозрения")
            val titlesM2 = listOf("Глава 1: Сладкий старт", "Глава 2: Острый спор", "Глава 3: Клубничное признание")
            val titlesM3 = listOf("Глава 1: Городские тени", "Глава 2: Шёпот во тьме", "Глава 3: Срывая маски")

            for (chNum in 1..3) {
                mangaDao.insertChapter(
                    MangaChapter(
                        mangaId = m1,
                        title = titlesM1[chNum - 1],
                        chapterNumber = chNum,
                        pageCount = 5,
                        pagesCsv = (1..5).joinToString(",") { page -> "panel://m1_${chNum}_$page" }
                    )
                )

                mangaDao.insertChapter(
                    MangaChapter(
                        mangaId = m2,
                        title = titlesM2[chNum - 1],
                        chapterNumber = chNum,
                        pageCount = 5,
                        pagesCsv = (1..5).joinToString(",") { page -> "panel://m2_${chNum}_$page" }
                    )
                )

                mangaDao.insertChapter(
                    MangaChapter(
                        mangaId = m3,
                        title = titlesM3[chNum - 1],
                        chapterNumber = chNum,
                        pageCount = 5,
                        pagesCsv = (1..5).joinToString(",") { page -> "panel://m3_${chNum}_$page" }
                    )
                )
            }
        }
    }
}

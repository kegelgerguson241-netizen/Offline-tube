package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: Int): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel): Long

    @Update
    suspend fun updateChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE id = :id")
    suspend fun deleteChannel(id: Int)
}

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY uploadDate DESC")
    fun getAllVideos(): Flow<List<Video>>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getVideoById(id: Int): Video?

    @Query("SELECT * FROM videos WHERE channelId = :channelId ORDER BY uploadDate DESC")
    fun getVideosByChannel(channelId: Int): Flow<List<Video>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: Video): Long

    @Update
    suspend fun updateVideo(video: Video)

    @Query("UPDATE videos SET views = views + 1 WHERE id = :id")
    suspend fun incrementViews(id: Int)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: Int)

    @Query("SELECT * FROM videos WHERE channelId = :channelId")
    suspend fun getVideosByChannelList(channelId: Int): List<Video>

    @Query("DELETE FROM videos WHERE channelId = :channelId")
    suspend fun deleteVideosByChannel(channelId: Int)
}

@Dao
interface WatchHistoryDao {
    @Query("""
        SELECT watch_history.* FROM watch_history 
        INNER JOIN videos ON watch_history.videoId = videos.id 
        ORDER BY watchedAt DESC
    """)
    fun getWatchHistory(): Flow<List<WatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistory)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun deleteHistoryForVideo(videoId: Int)

    @Query("DELETE FROM watch_history WHERE videoId IN (SELECT id FROM videos WHERE channelId = :channelId)")
    suspend fun deleteHistoryForChannel(channelId: Int)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE videoId = :videoId ORDER BY timestamp DESC")
    fun getCommentsForVideo(videoId: Int): Flow<List<Comment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment): Long

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteComment(id: Int)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Int)

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Int): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistVideo(playlistVideo: PlaylistVideo): Long

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Int, videoId: Int)

    @Query("DELETE FROM playlist_videos WHERE playlistId = :playlistId")
    suspend fun clearPlaylistVideos(playlistId: Int)

    @Query("DELETE FROM playlist_videos WHERE videoId = :videoId")
    suspend fun removeVideoFromAllPlaylists(videoId: Int)

    @Query("SELECT * FROM playlist_videos")
    fun getAllPlaylistVideos(): Flow<List<PlaylistVideo>>

    @Query("""
        SELECT videos.* FROM videos 
        INNER JOIN playlist_videos ON videos.id = playlist_videos.videoId 
        WHERE playlist_videos.playlistId = :playlistId
        ORDER BY playlist_videos.id ASC
    """)
    fun getVideosForPlaylist(playlistId: Int): Flow<List<Video>>
}

@Dao
interface AudioDao {
    @Query("SELECT * FROM audio_tracks ORDER BY year DESC, createdAt DESC")
    fun getAllAudioTracks(): Flow<List<AudioTrack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioTrack(track: AudioTrack): Long

    @Query("DELETE FROM audio_tracks WHERE id = :id")
    suspend fun deleteAudioTrack(id: Int)

    @Query("SELECT * FROM audio_tracks WHERE id = :id")
    suspend fun getAudioTrackById(id: Int): AudioTrack?

    @Query("SELECT * FROM audio_playlists ORDER BY name ASC")
    fun getAllAudioPlaylists(): Flow<List<AudioPlaylist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioPlaylist(playlist: AudioPlaylist): Long

    @Query("DELETE FROM audio_playlists WHERE id = :id")
    suspend fun deleteAudioPlaylist(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioPlaylistTrack(playlistTrack: AudioPlaylistTrack): Long

    @Query("DELETE FROM audio_playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: Int)

    @Query("DELETE FROM audio_playlist_tracks WHERE trackId = :trackId")
    suspend fun deleteAudioPlaylistTrackByTrack(trackId: Int)

    @Query("DELETE FROM audio_playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearAudioPlaylistTracks(playlistId: Int)

    @Query("SELECT * FROM audio_playlist_tracks")
    fun getAllAudioPlaylistTracks(): Flow<List<AudioPlaylistTrack>>

    @Query("""
        SELECT audio_tracks.* FROM audio_tracks 
        INNER JOIN audio_playlist_tracks ON audio_tracks.id = audio_playlist_tracks.trackId 
        WHERE audio_playlist_tracks.playlistId = :playlistId
        ORDER BY audio_playlist_tracks.id ASC
    """)
    fun getTracksForPlaylist(playlistId: Int): Flow<List<AudioTrack>>
}

@Dao
interface MangaDao {
    @Query("SELECT * FROM mangas ORDER BY addedAt DESC")
    fun getAllManga(): Flow<List<Manga>>

    @Query("SELECT * FROM mangas WHERE id = :id")
    suspend fun getMangaById(id: Int): Manga?

    @Query("SELECT * FROM manga_chapters WHERE mangaId = :mangaId ORDER BY chapterNumber ASC")
    fun getChaptersForManga(mangaId: Int): Flow<List<MangaChapter>>

    @Query("SELECT * FROM manga_chapters WHERE mangaId = :mangaId")
    suspend fun getChaptersForMangaList(mangaId: Int): List<MangaChapter>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManga(manga: Manga): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: MangaChapter): Long

    @Query("SELECT * FROM manga_reading_progress WHERE mangaId = :mangaId")
    fun getProgressForManga(mangaId: Int): Flow<MangaReadingProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: MangaReadingProgress)

    @Query("DELETE FROM mangas WHERE id = :id")
    suspend fun deleteManga(id: Int)
}

@Database(
    entities = [
        Channel::class, 
        Video::class, 
        WatchHistory::class, 
        Comment::class, 
        Playlist::class, 
        PlaylistVideo::class,
        AudioTrack::class,
        AudioPlaylist::class,
        AudioPlaylistTrack::class,
        Manga::class,
        MangaChapter::class,
        MangaReadingProgress::class
    ], 
    version = 14, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun videoDao(): VideoDao
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun commentDao(): CommentDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun audioDao(): AudioDao
    abstract fun mangaDao(): MangaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "offlinetube_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

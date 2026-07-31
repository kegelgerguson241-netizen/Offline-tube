package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val avatarUri: String,
    val bannerUri: String,
    val subscribers: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val isAgeRestricted: Boolean = false,
    val isLectorMode: Boolean = false
)

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = Channel::class,
            parentColumns = ["id"],
            childColumns = ["channelId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["channelId"])]
)
data class Video(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: Int,
    val title: String,
    val description: String,
    val videoUri: String,
    val thumbnailUri: String,
    val views: Int = 0,
    val likes: Int = 0,
    val duration: String = "0:00",
    val uploadDate: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false,
    val isAgeRestricted: Boolean = false,
    val isShorts: Boolean = false
)

@Entity(
    tableName = "watch_history",
    foreignKeys = [
        ForeignKey(
            entity = Video::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["videoId"])]
)
data class WatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val watchedAt: Long = System.currentTimeMillis(),
    val lastPosition: Long = 0L
)

@Entity(
    tableName = "comments",
    foreignKeys = [
        ForeignKey(
            entity = Video::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["videoId"])]
)
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: Int,
    val authorName: String,
    val content: String,
    val authorAvatarUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val coverUri: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val channelId: Int = 0,
    val isShorts: Boolean = false,
    val isAgeRestricted: Boolean = false
)

@Entity(
    tableName = "playlist_videos",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Video::class,
            parentColumns = ["id"],
            childColumns = ["videoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["videoId"])]
)
data class PlaylistVideo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val videoId: Int
)

@Entity(tableName = "audio_tracks")
data class AudioTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val audioUri: String,
    val duration: String = "3:00",
    val coverUri: String = "",
    val year: Int = 2026,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audio_playlists")
data class AudioPlaylist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "audio_playlist_tracks",
    foreignKeys = [
        ForeignKey(
            entity = AudioPlaylist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AudioTrack::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId"]), Index(value = ["trackId"])]
)
data class AudioPlaylistTrack(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val trackId: Int
)

@Entity(tableName = "mangas")
data class Manga(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String = "Неизвестен",
    val description: String = "",
    val coverUri: String = "",
    val rating: Float = 4.8f,
    val isCompleted: Boolean = false,
    val genre: String = "Романтика, Драма",
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "manga_chapters",
    foreignKeys = [
        ForeignKey(
            entity = Manga::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mangaId"])]
)
data class MangaChapter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mangaId: Int,
    val title: String,
    val chapterNumber: Int,
    val pageCount: Int,
    val pagesCsv: String // Comma separated list of page URLs or placeholders
)

@Entity(
    tableName = "manga_reading_progress",
    foreignKeys = [
        ForeignKey(
            entity = Manga::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mangaId"])]
)
data class MangaReadingProgress(
    @PrimaryKey val mangaId: Int,
    val lastReadChapterId: Int,
    val lastReadPage: Int,
    val updatedAt: Long = System.currentTimeMillis()
)



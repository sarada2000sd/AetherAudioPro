package com.aetheraudio.pro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single audio track indexed from MediaStore within a user-selected folder.
 * mediaStoreId doubles as the content:// URI id (content://media/external/audio/media/{id}).
 */
@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val genre: String = "Unknown",
    val year: Int = 0,
    val trackNumber: Int = 0,
    val durationMs: Long,
    val folderPath: String,
    val filePath: String,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val lyricsPath: String? = null
)

@Entity(tableName = "albums")
data class Album(
    @PrimaryKey val albumId: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val year: Int = 0,
    val customArtworkPath: String? = null
)

@Entity(tableName = "artists")
data class Artist(
    @PrimaryKey val name: String,
    val songCount: Int,
    val albumCount: Int
)

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val coverArtSongId: Long? = null
)

@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int
)

/** A directory explicitly opted into scanning by the user (via SAF tree picker). */
@Entity(tableName = "watched_folders")
data class WatchedFolder(
    @PrimaryKey val uri: String,
    val displayName: String
)

data class Genre(val name: String, val songCount: Int)

/** Default folders never auto-indexed even if inside a watched tree, per spec. */
val DEFAULT_EXCLUDED_FOLDER_KEYWORDS = listOf(
    "whatsapp audio", "voice notes", "call recording", "call recordings", "notifications", "ringtones"
)

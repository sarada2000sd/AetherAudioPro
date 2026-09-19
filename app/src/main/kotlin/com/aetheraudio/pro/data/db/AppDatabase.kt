package com.aetheraudio.pro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aetheraudio.pro.data.model.Album
import com.aetheraudio.pro.data.model.Artist
import com.aetheraudio.pro.data.model.Playlist
import com.aetheraudio.pro.data.model.PlaylistSongCrossRef
import com.aetheraudio.pro.data.model.Song
import com.aetheraudio.pro.data.model.WatchedFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber ASC, title ASC")
    fun observeByAlbum(albumId: Long): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album, trackNumber ASC")
    fun observeByArtist(artist: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun observeByGenre(genre: String): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE folderPath = :folderPath ORDER BY title ASC")
    fun observeByFolder(folderPath: String): Flow<List<Song>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 50): Flow<List<Song>>

    @Query("""SELECT * FROM songs WHERE title LIKE '%' || :query || '%'
        OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'
        OR genre LIKE '%' || :query || '%' OR folderPath LIKE '%' || :query || '%'""")
    fun search(query: String): Flow<List<Song>>

    @Query("SELECT DISTINCT folderPath FROM songs ORDER BY folderPath ASC")
    fun observeFolders(): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM songs ORDER BY genre ASC")
    fun observeGenreNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(songs: List<Song>)

    @Update
    suspend fun update(song: Song)

    @Query("UPDATE songs SET isFavorite = :fav WHERE mediaStoreId = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("DELETE FROM songs WHERE mediaStoreId NOT IN (:keepIds)")
    suspend fun pruneMissing(keepIds: List<Long>)

    @Query("SELECT * FROM songs WHERE mediaStoreId = :id")
    suspend fun getById(id: Long): Song?
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Album>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(albums: List<Album>)

    @Query("DELETE FROM albums WHERE albumId NOT IN (:keepIds)")
    suspend fun pruneMissing(keepIds: List<Long>)
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Artist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(artists: List<Artist>)

    @Query("DELETE FROM artists WHERE name NOT IN (:keepNames)")
    suspend fun pruneMissing(keepNames: List<String>)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Playlist>>

    @Insert
    suspend fun insert(playlist: Playlist): Long

    @Delete
    suspend fun delete(playlist: Playlist)

    @Query("""SELECT songs.* FROM songs
        INNER JOIN playlist_songs ON songs.mediaStoreId = playlist_songs.songId
        WHERE playlist_songs.playlistId = :playlistId ORDER BY playlist_songs.position ASC""")
    fun observeSongsInPlaylist(playlistId: Long): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(ref: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int
}

@Dao
interface WatchedFolderDao {
    @Query("SELECT * FROM watched_folders")
    fun observeAll(): Flow<List<WatchedFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: WatchedFolder)

    @Delete
    suspend fun delete(folder: WatchedFolder)
}

@Database(
    entities = [Song::class, Album::class, Artist::class, Playlist::class,
        PlaylistSongCrossRef::class, WatchedFolder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun watchedFolderDao(): WatchedFolderDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "aetheraudio.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}

package com.aetheraudio.pro.data.repository

import android.content.Context
import com.aetheraudio.pro.data.db.AppDatabase
import com.aetheraudio.pro.data.model.Playlist
import com.aetheraudio.pro.data.model.PlaylistSongCrossRef
import com.aetheraudio.pro.data.model.Song
import kotlinx.coroutines.flow.Flow

class PlaylistRepository(context: Context) {
    private val dao = AppDatabase.get(context).playlistDao()

    fun observePlaylists(): Flow<List<Playlist>> = dao.observeAll()
    fun observeSongs(playlistId: Long): Flow<List<Song>> = dao.observeSongsInPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long = dao.insert(Playlist(name = name))
    suspend fun deletePlaylist(playlist: Playlist) = dao.delete(playlist)

    suspend fun addSong(playlistId: Long, song: Song) {
        val position = dao.nextPosition(playlistId)
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, song.mediaStoreId, position))
    }

    suspend fun removeSong(playlistId: Long, song: Song) = dao.removeSongFromPlaylist(playlistId, song.mediaStoreId)

    companion object {
        @Volatile private var instance: PlaylistRepository? = null
        fun get(context: Context): PlaylistRepository = instance ?: synchronized(this) {
            instance ?: PlaylistRepository(context.applicationContext).also { instance = it }
        }
    }
}

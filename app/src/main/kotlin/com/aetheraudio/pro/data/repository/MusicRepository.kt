package com.aetheraudio.pro.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.aetheraudio.pro.data.db.AppDatabase
import com.aetheraudio.pro.data.model.Album
import com.aetheraudio.pro.data.model.Artist
import com.aetheraudio.pro.data.model.DEFAULT_EXCLUDED_FOLDER_KEYWORDS
import com.aetheraudio.pro.data.model.Song
import com.aetheraudio.pro.data.model.WatchedFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Single source of truth for the local library. Scans MediaStore, but only within folders
 * the user has explicitly opted into via [addWatchedFolder] (Storage Access Framework),
 * and always skips noisy system folders (WhatsApp Audio, Call Recordings, Voice Notes).
 * Results are cached in Room so the rest of the app can just observe Flows.
 */
class MusicRepository(private val context: Context) {

    private val db = AppDatabase.get(context)
    private val songDao = db.songDao()
    private val albumDao = db.albumDao()
    private val artistDao = db.artistDao()
    private val folderDao = db.watchedFolderDao()

    fun observeSongs(): Flow<List<Song>> = songDao.observeAll()
    fun observeFavorites(): Flow<List<Song>> = songDao.observeFavorites()
    fun observeAlbums(): Flow<List<Album>> = albumDao.observeAll()
    fun observeArtists(): Flow<List<Artist>> = artistDao.observeAll()
    fun observeRecentlyAdded(): Flow<List<Song>> = songDao.observeRecentlyAdded()
    fun observeFolders(): Flow<List<String>> = songDao.observeFolders()
    fun observeGenreNames(): Flow<List<String>> = songDao.observeGenreNames()
    fun observeWatchedFolders(): Flow<List<WatchedFolder>> = folderDao.observeAll()
    fun search(query: String): Flow<List<Song>> = songDao.search(query)
    fun songsInAlbum(albumId: Long): Flow<List<Song>> = songDao.observeByAlbum(albumId)
    fun songsByArtist(artist: String): Flow<List<Song>> = songDao.observeByArtist(artist)
    fun songsByGenre(genre: String): Flow<List<Song>> = songDao.observeByGenre(genre)
    fun songsInFolder(path: String): Flow<List<Song>> = songDao.observeByFolder(path)

    suspend fun toggleFavorite(song: Song) =
        songDao.setFavorite(song.mediaStoreId, !song.isFavorite)

    suspend fun addWatchedFolder(treeUri: Uri, displayName: String) {
        context.contentResolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        folderDao.insert(WatchedFolder(treeUri.toString(), displayName))
        rescan()
    }

    suspend fun removeWatchedFolder(folder: WatchedFolder) {
        folderDao.delete(folder)
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(folder.uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        rescan()
    }

    private fun isExcluded(path: String): Boolean {
        val lower = path.lowercase()
        return DEFAULT_EXCLUDED_FOLDER_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * Re-scans MediaStore for tracks whose file path falls under one of the watched folder
     * trees, upserts them into Room, and prunes anything that's no longer present.
     * Cheap enough to call after every watched-folder change; a real Inotify-based
     * background watcher (per spec 5.1) would additionally call this from a
     * FileObserver / WorkManager periodic job.
     */
    suspend fun rescan() = withContext(Dispatchers.IO) {
        // Resolve watched SAF tree URIs down to plain filesystem path prefixes we can match
        // against MediaStore's DATA/RELATIVE_PATH columns.
        val watchedPathPrefixes = context.contentResolver.persistedUriPermissions.mapNotNull {
            DocumentFile.fromTreeUri(context, it.uri)?.let { doc ->
                uriTreeToFilePathPrefix(it.uri)
            }
        }

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val songs = mutableListOf<Song>()
        context.contentResolver.query(collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataCol) ?: continue
                if (isExcluded(path)) continue
                // Only index files under a user-approved folder. If no folders are configured
                // yet, fall back to indexing nothing (strict, per spec 5.1) rather than
                // silently importing the whole device.
                if (watchedPathPrefixes.isNotEmpty() && watchedPathPrefixes.none { path.startsWith(it) }) {
                    continue
                }
                val id = cursor.getLong(idCol)
                songs += Song(
                    mediaStoreId = id,
                    title = cursor.getString(titleCol) ?: File(path).nameWithoutExtension,
                    artist = cursor.getString(artistCol) ?: "Unknown Artist",
                    album = cursor.getString(albumCol) ?: "Unknown Album",
                    albumId = cursor.getLong(albumIdCol),
                    year = cursor.getInt(yearCol),
                    trackNumber = cursor.getInt(trackCol) % 1000,
                    durationMs = cursor.getLong(durationCol),
                    folderPath = File(path).parent ?: path,
                    filePath = path,
                    dateAdded = cursor.getLong(dateAddedCol) * 1000L,
                    lyricsPath = findLrcFor(path)
                )
            }
        }

        songDao.upsertAll(songs)
        songDao.pruneMissing(songs.map { it.mediaStoreId })

        val albums = songs.groupBy { it.albumId }.map { (albumId, group) ->
            Album(albumId, group.first().album, group.first().artist, group.size, group.first().year)
        }
        albumDao.upsertAll(albums)
        albumDao.pruneMissing(albums.map { it.albumId })

        val artists = songs.groupBy { it.artist }.map { (name, group) ->
            Artist(name, group.size, group.map { it.albumId }.distinct().size)
        }
        artistDao.upsertAll(artists)
        artistDao.pruneMissing(artists.map { it.name })
    }

    /** A same-named .lrc file next to the audio file, if present. */
    private fun findLrcFor(audioPath: String): String? {
        val f = File(audioPath)
        val lrc = File(f.parentFile, f.nameWithoutExtension + ".lrc")
        return if (lrc.exists()) lrc.absolutePath else null
    }

    fun albumArtUri(albumId: Long): Uri =
        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)

    /**
     * Best-effort mapping from a SAF tree URI to an actual filesystem path prefix so it can be
     * matched against MediaStore.DATA. Works for the common "primary" external storage case.
     */
    private fun uriTreeToFilePathPrefix(treeUri: Uri): String? {
        val docId = DocumentFile.fromTreeUri(context, treeUri)?.uri?.let {
            android.provider.DocumentsContract.getTreeDocumentId(it)
        } ?: return null
        val split = docId.split(":")
        if (split.size < 2) return null
        val type = split[0]
        val relative = split[1]
        return if (type.equals("primary", ignoreCase = true)) {
            "${android.os.Environment.getExternalStorageDirectory().absolutePath}/$relative"
        } else {
            // Removable storage volume; best effort common mount point.
            "/storage/$type/$relative"
        }
    }

    companion object {
        @Volatile private var instance: MusicRepository? = null
        fun get(context: Context): MusicRepository = instance ?: synchronized(this) {
            instance ?: MusicRepository(context.applicationContext).also { instance = it }
        }
    }
}

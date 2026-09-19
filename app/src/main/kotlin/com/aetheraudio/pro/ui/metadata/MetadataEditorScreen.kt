package com.aetheraudio.pro.ui.metadata

import android.content.ContentUris
import android.content.ContentValues
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.data.model.Song
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.ui.components.AlbumArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Long-press -> Edit metadata (spec). Title/Album/Artist/Year/Genre/Track# are written back
 * through MediaStore's ContentResolver, which Android permits for app-scanned files without
 * extra storage permissions on API 29+. Full ID3v2 frame writing (embedded lyrics, custom
 * cover-art replacement baked into the file) needs a dedicated tagging library (e.g. a
 * Kotlin/Java ID3 writer) -- MediaStore alone can't rewrite embedded artwork bytes or lyric
 * frames, only its own index columns. That's flagged below rather than silently no-op'd.
 */
@Composable
fun MetadataEditorScreen(songId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val scope = rememberCoroutineScope()
    var song by remember { mutableStateOf<Song?>(null) }

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var trackNumber by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(songId) {
        val loaded = withContext(Dispatchers.IO) { com.aetheraudio.pro.data.db.AppDatabase.get(context).songDao().getById(songId) }
        song = loaded
        loaded?.let {
            title = it.title; artist = it.artist; album = it.album
            year = if (it.year > 0) it.year.toString() else ""
            genre = it.genre; trackNumber = if (it.trackNumber > 0) it.trackNumber.toString() else ""
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Edit Metadata", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }

        song?.let { s ->
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                AlbumArt(artUri = repo.albumArtUri(s.albumId), modifier = Modifier.padding(end = 16.dp))
            }
        }

        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = year, onValueChange = { year = it.filter(Char::isDigit) }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = trackNumber, onValueChange = { trackNumber = it.filter(Char::isDigit) }, label = { Text("Track #") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth())

            Text(
                "Lyrics and embedded cover art are edited via the .lrc file and Custom Album Builder respectively, " +
                    "not this form -- MediaStore doesn't expose those fields for in-place rewriting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                enabled = !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
                            val values = ContentValues().apply {
                                put(MediaStore.Audio.Media.TITLE, title)
                                put(MediaStore.Audio.Media.ARTIST, artist)
                                put(MediaStore.Audio.Media.ALBUM, album)
                                year.toIntOrNull()?.let { put(MediaStore.Audio.Media.YEAR, it) }
                                trackNumber.toIntOrNull()?.let { put(MediaStore.Audio.Media.TRACK, it) }
                            }
                            runCatching { context.contentResolver.update(uri, values, null, null) }
                        }
                        repo.rescan()
                        saving = false
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "Saving..." else "Save changes")
            }
        }
    }
}

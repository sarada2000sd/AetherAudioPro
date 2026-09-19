package com.aetheraudio.pro.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.components.AlbumArt
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(albumId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val songs by repo.songsInAlbum(albumId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(Modifier.padding(start = 4.dp)) {
                Text(songs.firstOrNull()?.album ?: "Album", style = MaterialTheme.typography.titleLarge)
                Text(songs.firstOrNull()?.artist ?: "", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(artUri = repo.albumArtUri(albumId), modifier = Modifier.size(96.dp))
            Button(
                onClick = { if (songs.isNotEmpty()) PlayerController.playQueue(songs, 0) },
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(" Play all")
            }
        }
        LazyColumn {
            items(songs, key = { it.mediaStoreId }) { song ->
                SongRow(song = song, repo = repo,
                    onClick = { PlayerController.playQueue(songs, songs.indexOf(song)) },
                    onToggleFavorite = { scope.launch { repo.toggleFavorite(song) } })
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(artistName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val songs by repo.songsByArtist(artistName).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(artistName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }
        LazyColumn {
            items(songs, key = { it.mediaStoreId }) { song ->
                SongRow(song = song, repo = repo,
                    onClick = { PlayerController.playQueue(songs, songs.indexOf(song)) },
                    onToggleFavorite = { scope.launch { repo.toggleFavorite(song) } })
            }
        }
    }
}

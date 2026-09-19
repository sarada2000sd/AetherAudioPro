package com.aetheraudio.pro.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.library.SongRow
import kotlinx.coroutines.launch

/** Global offline search across song title, artist, album, genre and folder path (spec: Search screen). */
@Composable
fun SearchScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val results by (if (query.isBlank()) remember { kotlinx.coroutines.flow.flowOf(emptyList()) }
        else repo.search(query)).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                placeholder = { Text("Search songs, artists, albums, genres, folders") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, contentDescription = "Clear") }
                    }
                },
                singleLine = true
            )
        }

        if (query.isBlank()) {
            Text(
                "Search is entirely offline -- results come only from your indexed folders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn {
                items(results, key = { it.mediaStoreId }) { song ->
                    SongRow(
                        song = song, repo = repo,
                        onClick = { PlayerController.playQueue(results, results.indexOf(song)) },
                        onToggleFavorite = { scope.launch { repo.toggleFavorite(song) } }
                    )
                }
            }
        }
    }
}

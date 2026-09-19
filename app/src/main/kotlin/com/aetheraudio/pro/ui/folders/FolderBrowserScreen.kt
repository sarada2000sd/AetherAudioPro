package com.aetheraudio.pro.ui.folders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.aetheraudio.pro.data.model.DEFAULT_EXCLUDED_FOLDER_KEYWORDS
import com.aetheraudio.pro.data.repository.MusicRepository
import kotlinx.coroutines.launch

/**
 * Users explicitly select which folders to scan via the Storage Access Framework tree picker
 * (spec 5.1: "strict folder boundaries"). WhatsApp Audio / Call Recordings / Voice Notes are
 * always skipped even if they fall inside a selected tree.
 */
@Composable
fun FolderBrowserScreen(onOpenFolder: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val watched by repo.observeWatchedFolders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            scope.launch {
                val name = uri.lastPathSegment?.substringAfterLast(':') ?: uri.toString()
                repo.addWatchedFolder(uri, name)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { pickFolder.launch(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add folder")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Folders", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 4.dp))
            Text(
                "Only tracks in folders you add below are indexed. Never included: " +
                    DEFAULT_EXCLUDED_FOLDER_KEYWORDS.joinToString(", ") { it.replaceFirstChar(Char::uppercase) } + ".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            if (watched.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { pickFolder.launch(null) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(" Select a folder to scan")
                    }
                }
            } else {
                LazyColumn {
                    items(watched, key = { it.uri }) { folder ->
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp, 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenFolder(folder.displayName) }) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(folder.displayName, style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 12.dp))
                            }
                            IconButton(onClick = { scope.launch { repo.removeWatchedFolder(folder) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderDetailScreen(path: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val songs by repo.songsInFolder(path).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(path.substringAfterLast('/'), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }
        LazyColumn {
            items(songs, key = { it.mediaStoreId }) { song ->
                com.aetheraudio.pro.ui.library.SongRow(
                    song = song, repo = repo,
                    onClick = { com.aetheraudio.pro.playback.PlayerController.playQueue(songs, songs.indexOf(song)) },
                    onToggleFavorite = { scope.launch { repo.toggleFavorite(song) } }
                )
            }
        }
    }
}

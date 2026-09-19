package com.aetheraudio.pro.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.data.model.Song
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.components.AlbumArt

@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenNowPlaying: () -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    val recentlyAdded by repo.observeRecentlyAdded().collectAsState(initial = emptyList())
    val favorites by repo.observeFavorites().collectAsState(initial = emptyList())
    val allSongs by repo.observeSongs().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(bottom = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp, 16.dp, 12.dp, 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AetherAudio", style = MaterialTheme.typography.headlineLarge)
            IconButton(onClick = onOpenSearch) { Icon(Icons.Filled.Search, contentDescription = "Search") }
        }

        if (allSongs.isEmpty()) {
            EmptyLibraryHint(Modifier.fillMaxSize())
            return@Column
        }

        SectionHeader("Recently Added")
        SongCarousel(recentlyAdded, repo) { song -> PlayerController.playQueue(recentlyAdded, recentlyAdded.indexOf(song)); onOpenNowPlaying() }

        if (favorites.isNotEmpty()) {
            SectionHeader("Favorites")
            SongCarousel(favorites, repo) { song -> PlayerController.playQueue(favorites, favorites.indexOf(song)); onOpenNowPlaying() }
        }

        SectionHeader("Quick Access")
        SongCarousel(allSongs.take(20), repo) { song -> PlayerController.playQueue(allSongs, allSongs.indexOf(song)); onOpenNowPlaying() }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SongCarousel(songs: List<Song>, repo: MusicRepository, onClick: (Song) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(songs, key = { it.mediaStoreId }) { song ->
            Column(
                Modifier.width(140.dp).clickable { onClick(song) }
            ) {
                AlbumArt(
                    artUri = repo.albumArtUri(song.albumId),
                    modifier = Modifier.size(140.dp)
                )
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
                Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyLibraryHint(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No music yet", style = MaterialTheme.typography.titleLarge)
            Text(
                "Add a folder from the Folders tab to start building your library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}


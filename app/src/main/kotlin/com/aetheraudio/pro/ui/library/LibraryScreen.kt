package com.aetheraudio.pro.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.data.model.Album
import com.aetheraudio.pro.data.model.Artist
import com.aetheraudio.pro.data.model.Song
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.components.AlbumArt
import com.aetheraudio.pro.util.formatDuration
import kotlinx.coroutines.launch

private val tabs = listOf("Songs", "Albums", "Artists", "Genres", "Folders", "Recently Added", "Favorites")

@Composable
fun LibraryScreen(
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenFolder: (String) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { MusicRepository.get(context) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Text("Library", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp))

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 20.dp) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
            }
        }

        when (selectedTab) {
            0 -> SongsList(repo.observeSongs(), repo, onOpenAlbum = null)
            1 -> AlbumsGrid(repo, onOpenAlbum)
            2 -> ArtistsList(repo, onOpenArtist)
            3 -> GenresList(repo)
            4 -> FoldersList(repo, onOpenFolder)
            5 -> SongsList(repo.observeRecentlyAdded(), repo, onOpenAlbum = null)
            6 -> SongsList(repo.observeFavorites(), repo, onOpenAlbum = null)
        }
    }
}

@Composable
private fun SongsList(
    flow: kotlinx.coroutines.flow.Flow<List<Song>>,
    repo: MusicRepository,
    onOpenAlbum: ((Long) -> Unit)?
) {
    val songs by flow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    LazyColumn {
        items(songs, key = { it.mediaStoreId }) { song ->
            SongRow(
                song = song,
                repo = repo,
                onClick = { PlayerController.playQueue(songs, songs.indexOf(song)) },
                onToggleFavorite = { scope.launch { repo.toggleFavorite(song) } }
            )
        }
    }
}

@Composable
fun SongRow(song: Song, repo: MusicRepository, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(artUri = repo.albumArtUri(song.albumId), corner = 8, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text("${song.artist} \u2022 ${song.album}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
        IconButton(onClick = onToggleFavorite) {
            Icon(
                if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AlbumsGrid(repo: MusicRepository, onOpenAlbum: (Long) -> Unit) {
    val albums by repo.observeAlbums().collectAsState(initial = emptyList())
    LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp), contentPadding = PaddingValues(12.dp)) {
        items(albums, key = { it.albumId }) { album: Album ->
            Column(Modifier.padding(8.dp).clickable { onOpenAlbum(album.albumId) }) {
                AlbumArt(artUri = repo.albumArtUri(album.albumId), modifier = Modifier.fillMaxWidth().size(150.dp))
                Text(album.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp))
                Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ArtistsList(repo: MusicRepository, onOpenArtist: (String) -> Unit) {
    val artists by repo.observeArtists().collectAsState(initial = emptyList())
    LazyColumn {
        items(artists, key = { it.name }) { artist: Artist ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenArtist(artist.name) }.padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(artist.name, style = MaterialTheme.typography.titleMedium)
                    Text("${artist.songCount} songs \u2022 ${artist.albumCount} albums",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun GenresList(repo: MusicRepository) {
    val genres by repo.observeGenreNames().collectAsState(initial = emptyList())
    LazyColumn {
        items(genres) { genre ->
            Text(genre, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(16.dp, 14.dp))
        }
    }
}

@Composable
private fun FoldersList(repo: MusicRepository, onOpenFolder: (String) -> Unit) {
    val folders by repo.observeFolders().collectAsState(initial = emptyList())
    LazyColumn {
        items(folders) { folder ->
            Row(
                Modifier.fillMaxWidth().clickable { onOpenFolder(folder) }.padding(16.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(folder.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

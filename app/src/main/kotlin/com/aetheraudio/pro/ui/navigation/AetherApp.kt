package com.aetheraudio.pro.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aetheraudio.pro.playback.PlayerController
import com.aetheraudio.pro.ui.audiooutput.AudioOutputScreen
import com.aetheraudio.pro.ui.components.MiniPlayer
import com.aetheraudio.pro.ui.eq.EqualizerScreen
import com.aetheraudio.pro.ui.folders.FolderBrowserScreen
import com.aetheraudio.pro.ui.folders.FolderDetailScreen
import com.aetheraudio.pro.ui.home.HomeScreen
import com.aetheraudio.pro.ui.library.AlbumDetailScreen
import com.aetheraudio.pro.ui.library.ArtistDetailScreen
import com.aetheraudio.pro.ui.library.LibraryScreen
import com.aetheraudio.pro.ui.lyrics.LyricsScreen
import com.aetheraudio.pro.ui.metadata.MetadataEditorScreen
import com.aetheraudio.pro.ui.nowplaying.NowPlayingScreen
import com.aetheraudio.pro.ui.playlists.PlaylistDetailScreen
import com.aetheraudio.pro.ui.playlists.PlaylistsScreen
import com.aetheraudio.pro.ui.search.SearchScreen
import com.aetheraudio.pro.ui.settings.SettingsScreen

/**
 * Root of the app: adaptive navigation shell (bottom bar on phones, a rail on wider/unfolded
 * screens per Material 3 Adaptive) with a persistent mini player docked above it.
 */
@Composable
fun AetherApp(widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact) {
    val navController = rememberNavController()
    val playbackState by PlayerController.state.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val hasCurrentSong = playbackState.currentSong != null
    val useRail = widthSizeClass != WindowWidthSizeClass.Compact

    if (useRail) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail {
                bottomDestinations.forEach { item ->
                    NavigationRailItem(
                        selected = currentRoute == item.dest.route,
                        onClick = { navController.navigateTopLevel(item.dest.route) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
            Column(Modifier.weight(1f).fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    AetherNavHost(navController, Modifier.fillMaxSize())
                }
                MiniPlayerBar(hasCurrentSong) { navController.navigate(Dest.NowPlaying.route) }
            }
        }
    } else {
        Scaffold(
            bottomBar = {
                Column {
                    MiniPlayerBar(hasCurrentSong) { navController.navigate(Dest.NowPlaying.route) }
                    NavigationBar {
                        bottomDestinations.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.dest.route,
                                onClick = { navController.navigateTopLevel(item.dest.route) },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            AetherNavHost(navController, Modifier.padding(padding))
        }
    }
}

@Composable
private fun MiniPlayerBar(visible: Boolean, onExpand: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        MiniPlayer(onExpand = onExpand)
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AetherNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = Dest.Home.route, modifier = modifier) {
        composable(Dest.Home.route) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Dest.Search.route) },
                onOpenAlbum = { navController.navigate(Dest.AlbumDetail.path(it)) },
                onOpenNowPlaying = { navController.navigate(Dest.NowPlaying.route) }
            )
        }
        composable(Dest.Library.route) {
            LibraryScreen(
                onOpenAlbum = { navController.navigate(Dest.AlbumDetail.path(it)) },
                onOpenArtist = { navController.navigate(Dest.ArtistDetail.path(it)) },
                onOpenFolder = { navController.navigate(Dest.FolderDetail.path(it)) }
            )
        }
        composable(Dest.Folders.route) {
            FolderBrowserScreen(onOpenFolder = { navController.navigate(Dest.FolderDetail.path(it)) })
        }
        composable(Dest.Playlists.route) {
            PlaylistsScreen(onOpenPlaylist = { navController.navigate(Dest.PlaylistDetail.path(it)) })
        }
        composable(Dest.Settings.route) {
            SettingsScreen(onOpenAudioOutput = { navController.navigate(Dest.AudioOutput.route) })
        }
        composable(Dest.Search.route) {
            SearchScreen(onBack = { navController.popBackStack() })
        }
        composable(Dest.NowPlaying.route) {
            NowPlayingScreen(
                onBack = { navController.popBackStack() },
                onOpenEq = { navController.navigate(Dest.Equalizer.route) },
                onOpenLyrics = { navController.navigate(Dest.Lyrics.route) },
                onOpenAudioOutput = { navController.navigate(Dest.AudioOutput.route) }
            )
        }
        composable(Dest.Equalizer.route) { EqualizerScreen(onBack = { navController.popBackStack() }) }
        composable(Dest.AudioOutput.route) { AudioOutputScreen(onBack = { navController.popBackStack() }) }
        composable(Dest.Lyrics.route) { LyricsScreen(onBack = { navController.popBackStack() }) }
        composable(Dest.Metadata.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("songId")?.toLongOrNull() ?: return@composable
            MetadataEditorScreen(songId = id, onBack = { navController.popBackStack() })
        }
        composable(Dest.AlbumDetail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("albumId")?.toLongOrNull() ?: return@composable
            AlbumDetailScreen(albumId = id, onBack = { navController.popBackStack() })
        }
        composable(Dest.ArtistDetail.route) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("artistName") ?: return@composable
            ArtistDetailScreen(artistName = name, onBack = { navController.popBackStack() })
        }
        composable(Dest.FolderDetail.route) { backStackEntry ->
            val encoded = backStackEntry.arguments?.getString("encodedPath") ?: return@composable
            val path = java.net.URLDecoder.decode(encoded, "UTF-8")
            FolderDetailScreen(path = path, onBack = { navController.popBackStack() })
        }
        composable(Dest.PlaylistDetail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: return@composable
            PlaylistDetailScreen(playlistId = id, onBack = { navController.popBackStack() })
        }
    }
}

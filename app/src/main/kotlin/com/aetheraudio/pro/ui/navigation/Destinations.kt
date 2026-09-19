package com.aetheraudio.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String) {
    data object Home : Dest("home")
    data object Library : Dest("library")
    data object Folders : Dest("folders")
    data object Playlists : Dest("playlists")
    data object Settings : Dest("settings")

    data object Search : Dest("search")
    data object NowPlaying : Dest("now_playing")
    data object Equalizer : Dest("equalizer")
    data object AudioOutput : Dest("audio_output")
    data object Lyrics : Dest("lyrics")
    data object Metadata : Dest("metadata/{songId}") {
        fun path(songId: Long) = "metadata/$songId"
    }
    data object AlbumDetail : Dest("album/{albumId}") {
        fun path(albumId: Long) = "album/$albumId"
    }
    data object ArtistDetail : Dest("artist/{artistName}") {
        fun path(artistName: String) = "artist/$artistName"
    }
    data object FolderDetail : Dest("folder/{encodedPath}") {
        fun path(p: String) = "folder/${java.net.URLEncoder.encode(p, "UTF-8")}"
    }
    data object PlaylistDetail : Dest("playlist/{playlistId}") {
        fun path(playlistId: Long) = "playlist/$playlistId"
    }
}

data class BottomDest(val dest: Dest, val label: String, val icon: ImageVector)

val bottomDestinations = listOf(
    BottomDest(Dest.Home, "Home", Icons.Filled.Home),
    BottomDest(Dest.Library, "Library", Icons.Filled.LibraryMusic),
    BottomDest(Dest.Folders, "Folders", Icons.Filled.Folder),
    BottomDest(Dest.Playlists, "Playlists", Icons.Filled.QueueMusic),
    BottomDest(Dest.Settings, "Settings", Icons.Filled.Settings)
)

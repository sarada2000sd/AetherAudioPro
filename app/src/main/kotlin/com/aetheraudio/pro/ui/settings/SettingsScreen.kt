package com.aetheraudio.pro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.aetheraudio.pro.BuildConfig
import com.aetheraudio.pro.data.repository.MusicRepository
import com.aetheraudio.pro.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onOpenAudioOutput: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository.get(context) }
    val repo = remember { MusicRepository.get(context) }
    val followSystem by settings.followSystemTheme.collectAsState(initial = true)
    val darkTheme by settings.darkTheme.collectAsState(initial = true)
    val amoled by settings.amoledTrueBlack.collectAsState(initial = false)
    val watchedFolders by repo.observeWatchedFolders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(bottom = 24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp))

        SettingsSectionLabel("Appearance")
        SettingsSwitchRow(
            title = "Follow system theme",
            subtitle = "Switch between light and dark automatically",
            checked = followSystem,
            onCheckedChange = { scope.launch { settings.setFollowSystemTheme(it) } }
        )
        if (!followSystem) {
            SettingsSwitchRow(
                title = "Dark theme",
                subtitle = null,
                checked = darkTheme,
                onCheckedChange = { scope.launch { settings.setDarkTheme(it) } }
            )
        }
        SettingsSwitchRow(
            title = "AMOLED true black",
            subtitle = "Pure black background to save battery on OLED screens",
            checked = amoled,
            onCheckedChange = { scope.launch { settings.setAmoledTrueBlack(it) } }
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionLabel("Playback")
        SettingsNavRow(
            title = "Audio output",
            subtitle = "Output device, sample rate, DSP",
            icon = Icons.Filled.Speaker,
            onClick = onOpenAudioOutput
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionLabel("Library")
        Text(
            "${watchedFolders.size} folder(s) currently scanned. Manage them from the Folders tab.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        SettingsSectionLabel("About")
        Text(
            "AetherAudio Pro v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavRow(title: String, subtitle: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

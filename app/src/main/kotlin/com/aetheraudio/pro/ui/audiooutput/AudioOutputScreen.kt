package com.aetheraudio.pro.ui.audiooutput

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.playback.AudioEffectsManager

/**
 * Real device/output info pulled from AudioManager -- not mocked -- showing the currently
 * available playback devices and, where the platform exposes it, sample rate / channel info.
 */
@Composable
fun AudioOutputScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val devices = remember { getPlaybackDevices(context) }
    var dspEnabled by remember { mutableStateOf(true) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Audio Output", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
        }

        Text(
            "Available output devices",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        devices.forEach { device ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(device.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 16.dp)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium)
                    Text(device.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("DSP processing", style = MaterialTheme.typography.titleMedium)
                Text("Equalizer, bass boost & reverb chain", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = dspEnabled, onCheckedChange = { dspEnabled = it; AudioEffectsManager.setEnabled(it) })
        }
    }
}

private data class OutputDeviceUi(
    val name: String,
    val detail: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private fun getPlaybackDevices(context: Context): List<OutputDeviceUi> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val infos = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    if (infos.isEmpty()) {
        return listOf(OutputDeviceUi("Phone speaker", "Default output", Icons.Filled.Speaker))
    }
    return infos.map { info ->
        val (name, icon) = when (info.type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker" to Icons.Filled.Speaker
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth" to Icons.Filled.Bluetooth
            AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Wired headphones" to Icons.Filled.Headset
            AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> "USB DAC" to Icons.Filled.Usb
            else -> (info.productName?.toString() ?: "Output device") to Icons.Filled.Speaker
        }
        val sampleRates = info.sampleRates?.joinToString(", ") ?: "device default"
        val channels = info.channelCounts?.maxOrNull()?.let { "$it ch" } ?: ""
        OutputDeviceUi(name, "Sample rate: $sampleRates Hz  $channels", icon)
    }.distinctBy { it.name }
}

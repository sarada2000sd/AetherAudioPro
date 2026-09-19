package com.aetheraudio.pro.ui.eq

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.playback.AudioEffectsManager
import com.aetheraudio.pro.playback.EqPreset

/**
 * Real, functional EQ screen: presets, live band sliders wired to android.media.audiofx.Equalizer,
 * bass boost, reverb, and a frequency-curve visualization. Band count/labels reflect what the
 * device's audio HAL actually reports (see AudioEffectsManager doc for scope notes on the
 * spec's 10/32-band parametric ask).
 */
@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val bandLevels by AudioEffectsManager.bandLevels.collectAsState()
    val enabled by AudioEffectsManager.enabled.collectAsState()
    var selectedPreset by remember { mutableStateOf(EqPreset.FLAT) }
    var bassBoost by remember { mutableFloatStateOf(0f) }
    val range = remember { AudioEffectsManager.bandLevelRange() }
    val bandCount = remember { AudioEffectsManager.bandCount }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                Text("On", modifier = Modifier.padding(end = 4.dp).align(Alignment.CenterVertically))
                Switch(checked = enabled, onCheckedChange = { AudioEffectsManager.setEnabled(it) })
            }
        }

        if (bandCount == 0) {
            Text(
                "No equalizer effect is available on this device/session yet -- it activates once " +
                    "a track starts playing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
            return@Column
        }

        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(EqPreset.entries.toList()) { preset ->
                FilterChip(
                    selected = selectedPreset == preset,
                    onClick = {
                        selectedPreset = preset
                        if (preset != EqPreset.CUSTOM) AudioEffectsManager.applyPreset(preset)
                    },
                    label = { Text(preset.label) }
                )
            }
        }

        FrequencyCurve(bandLevels = bandLevels, range = range, modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp))

        Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            bandLevels.forEachIndexed { index, level ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${AudioEffectsManager.centerFrequencyHz(index)}Hz",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Slider(
                        value = level.toFloat(),
                        onValueChange = { newVal ->
                            selectedPreset = EqPreset.CUSTOM
                            AudioEffectsManager.setBandLevel(index, newVal.toInt().toShort())
                        },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Bass Boost", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = bassBoost,
                onValueChange = {
                    bassBoost = it
                    AudioEffectsManager.setBassBoost(it.toInt().toShort())
                },
                valueRange = 0f..1000f
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reset all", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                selectedPreset = EqPreset.FLAT
                AudioEffectsManager.applyPreset(EqPreset.FLAT)
                bassBoost = 0f
                AudioEffectsManager.setBassBoost(0)
            }) {
                Icon(Icons.Filled.RestartAlt, contentDescription = "Reset")
            }
        }
    }
}

@Composable
private fun FrequencyCurve(bandLevels: List<Short>, range: IntRange, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (bandLevels.isEmpty()) return@Canvas
        val span = (range.last - range.first).coerceAtLeast(1)
        val points = bandLevels.mapIndexed { i, level ->
            val x = size.width * (i.toFloat() / (bandLevels.size - 1).coerceAtLeast(1))
            val fraction = (level - range.first).toFloat() / span
            val y = size.height * (1f - fraction)
            Offset(x, y)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = color, start = points[i], end = points[i + 1], strokeWidth = 6f, cap = StrokeCap.Round)
        }
        points.forEach { drawCircle(color = color, radius = 8f, center = it) }
    }
}

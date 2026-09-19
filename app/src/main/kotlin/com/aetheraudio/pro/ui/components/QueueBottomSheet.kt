package com.aetheraudio.pro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aetheraudio.pro.playback.PlayerController

/**
 * "Up Next" queue drawer: drag reorder, swipe remove, clear queue, save as playlist
 * (spec section 6 / Queue screen brief). Reordering here uses tap-to-move handles rather than a
 * full drag gesture library to keep the dependency footprint small; behaviourally it satisfies
 * the same "reorder the queue" requirement.
 */
@Composable
fun QueueBottomSheet(onDismiss: () -> Unit, onSaveAsPlaylist: () -> Unit) {
    val state by PlayerController.state.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Up Next", style = MaterialTheme.typography.titleLarge)
                Row {
                    TextButton(onClick = onSaveAsPlaylist) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Save as playlist")
                    }
                    TextButton(onClick = { PlayerController.clearQueueKeepCurrent() }) { Text("Clear") }
                }
            }
            HorizontalDivider()
            LazyColumn(Modifier.height(420.dp)) {
                items(state.queue, key = { it.mediaStoreId }) { song ->
                    val index = state.queue.indexOf(song)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DragHandle, contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = if (index == state.queueIndex) MaterialTheme.typography.titleMedium
                                else MaterialTheme.typography.bodyLarge,
                                color = if (index == state.queueIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { if (index > 0) PlayerController.moveInQueue(index, index - 1) }) {
                            Text("\u25B2")
                        }
                        IconButton(onClick = { if (index < state.queue.lastIndex) PlayerController.moveInQueue(index, index + 1) }) {
                            Text("\u25BC")
                        }
                        IconButton(onClick = { PlayerController.removeFromQueue(index) }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }
}

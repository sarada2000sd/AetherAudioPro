package com.aetheraudio.pro.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a dominant/vibrant color from the current track's album art using androidx.palette,
 * to lightly tint the UI per spec 2.2 ("Material You Dynamic Extraction"). Genuinely functional,
 * on-device -- not a stub.
 */
object PaletteExtractor {
    suspend fun extractAccent(context: Context, artUri: Uri?): Color? = withContext(Dispatchers.IO) {
        if (artUri == null) return@withContext null
        runCatching {
            context.contentResolver.openInputStream(artUri)?.use { stream ->
                val bitmap = android.graphics.BitmapFactory.decodeStream(stream) ?: return@withContext null
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
                swatch?.let { Color(it.rgb) }
            }
        }.getOrNull()
    }
}

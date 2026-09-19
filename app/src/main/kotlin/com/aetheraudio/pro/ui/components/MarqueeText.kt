package com.aetheraudio.pro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow

/**
 * Long titles scroll horizontally and fade at both edges instead of hard-clipping (spec 2.3).
 * Only scrolls if the text actually overflows its container.
 */
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current
) {
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var textWidthPx by remember { mutableIntStateOf(0) }
    val overflowing = textWidthPx > containerWidthPx && containerWidthPx > 0

    val transition = rememberInfiniteTransition(label = "marquee")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (overflowing) -(textWidthPx - containerWidthPx + 48).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (textWidthPx * 18).coerceAtLeast(3000), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeOffset"
    )

    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .fadeEdges(overflowing)
    ) {
        Text(
            text = text,
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .onSizeChanged { textWidthPx = it.width }
                .graphicsLayer { translationX = if (overflowing) offset else 0f }
        )
    }
}

/** Alpha-mask the left/right 10% of the container to transparent, per spec 2.3. */
private fun Modifier.fadeEdges(enabled: Boolean): Modifier =
    if (!enabled) this else this.drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.1f to Color.Black,
                0.9f to Color.Black,
                1.0f to Color.Transparent
            ),
            blendMode = BlendMode.DstIn
        )
    }

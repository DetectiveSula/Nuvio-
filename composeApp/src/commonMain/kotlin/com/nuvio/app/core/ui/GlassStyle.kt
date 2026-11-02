package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** True when the Glass theme is selected in Settings › Appearance › Theme. */
@Composable
@ReadOnlyComposable
fun isGlassTheme(): Boolean = LocalAppTheme.current == AppTheme.GLASS

private val GlassWhite = Color(0xFFFFFFFF)
private val GlassSilver = Color(0xFFC7CCD6)
private val GlassMist = Color(0xFFE5E5EA)

/**
 * The Glass theme's background: near-black with soft white and silver glows. Static on purpose:
 * an animated background forces the blurred bottom bar (and everything above it) to redraw every
 * frame, which made navigation lag. Brushes are cached and only rebuilt when the size changes.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val radius = w * 0.95f
            val glows = listOf(
                glowBrush(GlassWhite.copy(alpha = 0.14f), Offset(w * 0.14f, h * 0.12f), radius),
                glowBrush(GlassSilver.copy(alpha = 0.12f), Offset(w * 0.90f, h * 0.44f), radius),
                glowBrush(GlassMist.copy(alpha = 0.10f), Offset(w * 0.20f, h * 0.84f), radius),
            )
            onDrawBehind {
                drawRect(background)
                glows.forEach { (brush, center) -> drawCircle(brush = brush, radius = radius, center = center) }
            }
        },
    )
}

private fun glowBrush(color: Color, center: Offset, radius: Float): Pair<Brush, Offset> =
    Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius) to center


/**
 * Frosted glass surface: translucent white fill with a light edge. [strong] is the brighter
 * variant used for primary buttons such as Play.
 */
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    strong: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = if (strong) 0.30f else 0.08f))
    .border(1.dp, Color.White.copy(alpha = if (strong) 0.55f else 0.20f), shape)

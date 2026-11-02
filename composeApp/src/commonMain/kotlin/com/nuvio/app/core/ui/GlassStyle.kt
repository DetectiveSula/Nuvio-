package com.nuvio.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

/** True when the Glass theme is selected in Settings › Appearance › Theme. */
@Composable
@ReadOnlyComposable
fun isGlassTheme(): Boolean = LocalAppTheme.current == AppTheme.GLASS

private val GlassViolet = Color(0xFF7B3FE4)
private val GlassPink = Color(0xFFE4527A)
private val GlassBlue = Color(0xFF2F9BE4)

/**
 * The Glass theme's background: the theme colour with soft violet, pink and blue glows that
 * drift slowly. Only redraws (no recomposition) while animating.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    val background = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "glass_backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glass_backdrop_drift",
    )
    Box(
        modifier = modifier.drawBehind {
            drawRect(background)
            val w = size.width
            val h = size.height
            val radius = w * 0.95f
            glow(GlassViolet.copy(alpha = 0.55f), Offset(w * (0.08f + 0.12f * drift), h * (0.10f + 0.04f * drift)), radius)
            glow(GlassPink.copy(alpha = 0.42f), Offset(w * (0.96f - 0.14f * drift), h * (0.42f + 0.05f * drift)), radius)
            glow(GlassBlue.copy(alpha = 0.42f), Offset(w * (0.14f + 0.12f * drift), h * (0.86f - 0.05f * drift)), radius)
        },
    )
}

private fun DrawScope.glow(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

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

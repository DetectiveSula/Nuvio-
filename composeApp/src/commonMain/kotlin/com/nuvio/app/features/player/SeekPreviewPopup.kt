package com.nuvio.app.features.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private val PreviewWidth = SeekPreviewWidth
private val PreviewHeight = 99.dp
private val PreviewGap = 14.dp

/**
 * Netflix-style still frame shown above the seek bar while scrubbing. Frames come from
 * [SeekPreviewStore], which prefetches them during playback; the frame under the finger is
 * requested first. Must be placed inside a container that has the timeline's bounds.
 */
@Composable
internal fun SeekPreviewPopup(
    store: SeekPreviewStore,
    positionMs: Long,
    durationMs: Long,
    timelineWidthPx: Int,
) {
    val density = LocalDensity.current
    val bucket = store.bucketOf(positionMs)
    LaunchedEffect(store, bucket) { store.request(positionMs) }

    // Exact frame when loaded, otherwise the nearest one already fetched.
    val frame = store.frameNear(positionMs)
    val previewWidthPx = with(density) { PreviewWidth.roundToPx() }

    val fraction = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val gapPx = with(density) { PreviewGap.roundToPx() }
    val labelPx = with(density) { 24.dp.roundToPx() }
    val heightPx = with(density) { PreviewHeight.roundToPx() } + labelPx
    val x = (timelineWidthPx * fraction - previewWidthPx / 2f).toInt()
        .coerceIn(0, (timelineWidthPx - previewWidthPx).coerceAtLeast(0))

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x, -(heightPx + gapPx)),
        properties = PopupProperties(focusable = false),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val shape = RoundedCornerShape(6.dp)
            if (frame != null) {
                Box(
                    modifier = Modifier
                        .size(PreviewWidth, PreviewHeight)
                        .shadow(10.dp, shape)
                        .clip(shape)
                        .background(Color.Black)
                        .border(2.dp, Color.White, shape),
                ) {
                    Image(
                        bitmap = frame,
                        contentDescription = null,
                        modifier = Modifier.size(PreviewWidth, PreviewHeight),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Box(modifier = Modifier.size(PreviewWidth, PreviewHeight))
            }
            Text(
                text = formatPlaybackTime(positionMs),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

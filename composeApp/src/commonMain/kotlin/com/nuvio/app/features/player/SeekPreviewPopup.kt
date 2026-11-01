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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

private val PreviewWidth = 176.dp
private val PreviewHeight = 99.dp
private val PreviewGap = 14.dp
private const val MaxCachedFrames = 48

/**
 * Netflix-style still frame shown above the seek bar while scrubbing. Frames are fetched per
 * [SeekPreviewBucketMs] interval and cached, so moving back over the same spot is instant.
 * Must be placed inside a container that has the timeline's bounds.
 */
@Composable
internal fun SeekPreviewPopup(
    source: SeekPreviewSource,
    positionMs: Long,
    durationMs: Long,
    timelineWidthPx: Int,
) {
    val density = LocalDensity.current
    val grabber = remember(source) { SeekPreviewFrameGrabber(source) }
    DisposableEffect(grabber) { onDispose { grabber.release() } }
    val frames = remember(source) { mutableStateMapOf<Long, ImageBitmap>() }

    val bucket = (positionMs / SeekPreviewBucketMs).coerceAtLeast(0L)
    val previewWidthPx = with(density) { PreviewWidth.roundToPx() }

    LaunchedEffect(grabber, bucket) {
        if (frames.containsKey(bucket)) return@LaunchedEffect
        delay(90) // wait until the finger slows down a little before fetching
        val frameTime = (bucket * SeekPreviewBucketMs + SeekPreviewBucketMs / 2).coerceAtMost(durationMs)
        val frame = grabber.frameAt(frameTime, previewWidthPx) ?: return@LaunchedEffect
        if (frames.size >= MaxCachedFrames) frames.keys.firstOrNull()?.let { frames.remove(it) }
        frames[bucket] = frame
    }

    // Show the exact frame when it's ready, otherwise the closest cached one while it loads.
    val frame = frames[bucket] ?: frames.keys.minByOrNull { kotlin.math.abs(it - bucket) }?.let { frames[it] }

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

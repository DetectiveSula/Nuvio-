package com.nuvio.app.features.player

import androidx.compose.ui.graphics.ImageBitmap

/** Preview frames are Android-only for now; on iOS the timeline keeps showing just the time. */
internal actual class SeekPreviewFrameGrabber actual constructor(
    private val source: SeekPreviewSource,
) {
    actual suspend fun frameAt(positionMs: Long, maxWidthPx: Int): ImageBitmap? = null

    actual fun release() = Unit
}

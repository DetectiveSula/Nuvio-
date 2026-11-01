package com.nuvio.app.features.player

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** The stream the seek-bar preview frames are taken from. */
data class SeekPreviewSource(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * Provided by the player screen around its controls. Null when preview frames can't work, for
 * example during peer-to-peer torrent playback where the file isn't fully reachable.
 */
internal val LocalSeekPreviewSource = staticCompositionLocalOf<SeekPreviewSource?> { null }

/** Interval the preview frames are grouped into while scrubbing, so nearby positions reuse a frame. */
internal const val SeekPreviewBucketMs = 5_000L

/** Only plain web links and local files can be opened for preview frames. */
internal fun seekPreviewSourceOrNull(url: String?, headers: Map<String, String>): SeekPreviewSource? {
    val trimmed = url?.trim().orEmpty()
    val supported = trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("file://", ignoreCase = true) ||
        trimmed.startsWith("/")
    if (!supported || trimmed.contains(".m3u8", ignoreCase = true)) return null
    return SeekPreviewSource(trimmed, headers)
}

/**
 * Grabs still frames from a video for the seek-bar preview. Returns null when a frame can't be
 * produced (unsupported platform, codec or link); the timeline then just shows the time.
 */
internal expect class SeekPreviewFrameGrabber(source: SeekPreviewSource) {
    suspend fun frameAt(positionMs: Long, maxWidthPx: Int): ImageBitmap?
    fun release()
}

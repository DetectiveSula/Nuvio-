package com.nuvio.app.features.player

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Uses Android's MediaMetadataRetriever on the stream link. Works for direct and debrid links
 * (e.g. TorBox) because they support jumping to any byte range, and for downloaded files.
 */
internal actual class SeekPreviewFrameGrabber actual constructor(
    private val source: SeekPreviewSource,
) {
    private val mutex = Mutex()
    private var retriever: MediaMetadataRetriever? = null
    private var unavailable = false

    actual suspend fun frameAt(positionMs: Long, maxWidthPx: Int): ImageBitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (unavailable) return@withLock null
            val opened = retriever ?: openRetriever()?.also { retriever = it }
            if (opened == null) {
                unavailable = true
                return@withLock null
            }
            runCatching {
                val timeUs = positionMs.coerceAtLeast(0L) * 1_000L
                val width = maxWidthPx.coerceAtLeast(64)
                val height = (width * 9 / 16).coerceAtLeast(36)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    opened.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, width, height)
                } else {
                    opened.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.scaledToWidth(width)
                }
                bitmap?.asImageBitmap()
            }.getOrNull()
        }
    }

    actual fun release() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock {
                runCatching { retriever?.release() }
                retriever = null
            }
        }
    }

    private fun openRetriever(): MediaMetadataRetriever? {
        val candidate = MediaMetadataRetriever()
        return runCatching {
            val url = source.url
            when {
                url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true) ->
                    candidate.setDataSource(url, source.headers)
                url.startsWith("file://", ignoreCase = true) ->
                    candidate.setDataSource(Uri.parse(url).path ?: error("No file path"))
                else -> candidate.setDataSource(url)
            }
            candidate
        }.getOrElse {
            runCatching { candidate.release() }
            null
        }
    }
}

private fun Bitmap.scaledToWidth(targetWidth: Int): Bitmap {
    if (width <= targetWidth) return this
    val targetHeight = (height.toLong() * targetWidth / width).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

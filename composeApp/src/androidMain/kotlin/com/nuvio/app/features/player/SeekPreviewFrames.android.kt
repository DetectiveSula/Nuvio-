@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.app.features.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.transformer.ExperimentalFrameExtractor
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutionException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/** Holds the application context for the seek-bar preview frame extractor. */
internal object SeekPreviewPlatform {
    @Volatile
    var appContext: Context? = null
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
}

/**
 * Grabs preview frames with Media3's frame extractor: hardware decoding, one open connection to
 * the stream, frames scaled down on the GPU. There is deliberately no slower fallback; if Media3
 * can't read a stream, the timeline just shows the time.
 */
internal actual class SeekPreviewFrameGrabber actual constructor(
    private val source: SeekPreviewSource,
) {
    // The extractor must be used from a single thread; everything here runs on the main thread.
    private var extractor: ExperimentalFrameExtractor? = null
    private var unavailable = false

    actual suspend fun frameAt(positionMs: Long, maxWidthPx: Int): ImageBitmap? = withContext(Dispatchers.Main.immediate) {
        if (unavailable) return@withContext null
        val active = extractor ?: createExtractor(maxWidthPx)?.also { extractor = it }
        if (active == null) {
            unavailable = true
            return@withContext null
        }
        try {
            val frame = active.getFrame(positionMs.coerceAtLeast(0L)).awaitResult()
            frame.bitmap.compact().asImageBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }

    actual fun release() {
        val active = extractor ?: return
        extractor = null
        Handler(Looper.getMainLooper()).post { runCatching { active.release() } }
    }

    private fun createExtractor(maxWidthPx: Int): ExperimentalFrameExtractor? = runCatching {
        val context = SeekPreviewPlatform.appContext ?: return null
        val width = maxWidthPx.coerceAtLeast(64)
        val height = (width * 9 / 16).coerceAtLeast(36)
        val configuration = ExperimentalFrameExtractor.Configuration.Builder()
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
        ExperimentalFrameExtractor(context, configuration).apply {
            setMediaItem(
                MediaItem.fromUri(source.toUri()),
                listOf(Presentation.createForWidthAndHeight(width, height, Presentation.LAYOUT_SCALE_TO_FIT)),
            )
        }
    }.getOrNull()
}

private fun SeekPreviewSource.toUri(): Uri = when {
    url.startsWith("/") -> Uri.fromFile(File(url))
    else -> Uri.parse(url)
}

/** Halves memory per frame; previews are small so the colour depth loss isn't visible. */
private fun Bitmap.compact(): Bitmap = runCatching {
    if (config == Bitmap.Config.RGB_565) this else copy(Bitmap.Config.RGB_565, false)
}.getOrNull() ?: this

private suspend fun <T> ListenableFuture<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            try {
                continuation.resume(get())
            } catch (error: ExecutionException) {
                continuation.resumeWithException(error.cause ?: error)
            } catch (error: Throwable) {
                continuation.resumeWithException(error)
            }
        },
        Runnable::run,
    )
}

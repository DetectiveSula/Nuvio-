package com.nuvio.app.features.player

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

private const val FrameSpacingMs = 10_000L
private const val MaxFrames = 400L
private const val OverviewSpacingMs = 120_000L
private const val WorkerCount = 1
private const val GiveUpAfterFailures = 4

/** Frames are fetched small (and shown at [SeekPreviewWidth]) to keep memory low. */
internal const val SeekPreviewFrameWidthPx = 240

/**
 * Seek-bar preview frames for one stream, one every 10 seconds (wider only for very long videos,
 * capped at [MaxFrames]). [run] fills them in three passes so there are never big gaps:
 * 1. one frame every 2 minutes across the whole video,
 * 2. every frame within 10 minutes of the current position,
 * 3. every remaining frame, nearest first.
 * While the user scrubs, [request] moves the frame under their finger to the front of the queue.
 * A single extractor is used so previews never take more than one hardware decoder.
 */
internal class SeekPreviewStore(
    source: SeekPreviewSource,
    private val frameWidthPx: Int,
) {
    val frames = mutableStateMapOf<Long, ImageBitmap>()

    private val grabbers = List(WorkerCount) { SeekPreviewFrameGrabber(source) }
    private val priority = Channel<Long>(Channel.CONFLATED)
    private val lock = Mutex()
    private val claimed = mutableSetOf<Long>()
    private val queue = ArrayDeque<Long>()
    private var failures = 0

    var intervalMs: Long = FrameSpacingMs
        private set

    fun bucketOf(positionMs: Long): Long = (positionMs.coerceAtLeast(0L) / intervalMs)

    /** The exact frame for [positionMs] if loaded, otherwise the nearest loaded one. */
    fun frameNear(positionMs: Long): ImageBitmap? {
        val bucket = bucketOf(positionMs)
        return frames[bucket] ?: frames.keys.minByOrNull { abs(it - bucket) }?.let { frames[it] }
    }

    fun request(positionMs: Long) {
        priority.trySend(bucketOf(positionMs))
    }

    /** Runs until cancelled (when the player closes or the stream changes). */
    suspend fun run(durationMs: Long, startPositionMs: Long) = coroutineScope {
        if (durationMs <= 0L) return@coroutineScope
        intervalMs = maxOf(FrameSpacingMs, durationMs / MaxFrames)
        val count = durationMs / intervalMs + 1
        val start = bucketOf(startPositionMs).coerceIn(0L, count - 1)
        val order = LinkedHashSet<Long>()
        // Pass 1: coarse overview of the whole video.
        val overviewStep = (OverviewSpacingMs / intervalMs).coerceAtLeast(1L)
        var overview = 0L
        while (overview < count) {
            order.add(overview)
            overview += overviewStep
        }
        // Passes 2 and 3: every frame, nearest to the current position first, so the 10 minutes
        // around the viewer fill in before the rest of the video.
        for (distance in 0 until count) {
            if (start + distance < count) order.add(start + distance)
            if (distance > 0 && start - distance >= 0) order.add(start - distance)
        }
        lock.withLock {
            queue.clear()
            queue.addAll(order)
        }
        grabbers.forEach { grabber -> launch { work(grabber, durationMs) } }
    }

    private suspend fun work(grabber: SeekPreviewFrameGrabber, durationMs: Long) = coroutineScope {
        while (isActive) {
            val bucket = priority.tryReceive().getOrNull()?.takeIf { claim(it) }
                ?: lock.withLock { nextQueuedLocked() }
                ?: priority.receive().takeIf { claim(it) }
                ?: continue
            val timeMs = (bucket * intervalMs + intervalMs / 2).coerceAtMost((durationMs - 1).coerceAtLeast(0L))
            val frame = grabber.frameAt(timeMs, frameWidthPx)
            if (frame != null) {
                frames[bucket] = frame
                lock.withLock { failures = 0 }
            } else {
                val stop = lock.withLock {
                    claimed.remove(bucket)
                    failures++
                    failures >= GiveUpAfterFailures && frames.isEmpty()
                }
                if (stop) break // this stream can't produce frames; don't keep hammering it
            }
        }
    }

    private suspend fun claim(bucket: Long): Boolean = lock.withLock {
        if (frames.containsKey(bucket) || bucket in claimed) false else claimed.add(bucket)
    }

    private fun nextQueuedLocked(): Long? {
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (!frames.containsKey(next) && next !in claimed) {
                claimed.add(next)
                return next
            }
        }
        return null
    }

    fun release() {
        grabbers.forEach { it.release() }
    }
}

package app.booklore.android.feature.reader

import android.util.Log
import app.booklore.android.core.model.ReadingSessionReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Mirrors the reading-session tracking of the BookLore web reader so client
 * reading accumulates in the server's reading records: a session starts with
 * the first page activity, ends when the reader goes idle for
 * [IDLE_TIMEOUT_MS] or is closed, and is only reported once it lasted at least
 * [MIN_SESSION_DURATION_SECONDS].
 *
 * Every step is logged under the [TAG] tag so a missing server record can be
 * diagnosed with `adb logcat -s BookLoreSession`.
 */
internal class ReadingSessionTracker(
    private val reportSession: suspend (ReadingSessionReport) -> Unit,
) {
    // Owns its own scope so a report in flight still completes after the
    // reader's ViewModel scope is cancelled while exiting the reader.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val monitor = Any()
    private var session: ActiveSession? = null
    private var idleJob: Job? = null

    private class ActiveSession(
        val bookId: Long,
        val bookType: String,
        val startTimeMs: Long,
        val startLocation: String?,
        val startProgressPercent: Float?,
    ) {
        var lastActivityMs: Long = startTimeMs
        var endLocation: String? = startLocation
        var endProgressPercent: Float? = startProgressPercent
    }

    /** Starts a session on the first recorded activity, then keeps the current position fresh. */
    fun trackActivity(bookId: Long, bookType: String, location: String?, progressPercent: Float?) {
        synchronized(monitor) {
            val active = session
            if (active == null) {
                session = ActiveSession(
                    bookId = bookId,
                    bookType = bookType,
                    startTimeMs = System.currentTimeMillis(),
                    startLocation = location,
                    startProgressPercent = progressPercent,
                )
                Log.i(TAG, "Session started: bookId=$bookId bookType=$bookType")
                scheduleIdleCheck()
            } else {
                active.lastActivityMs = System.currentTimeMillis()
                active.endLocation = location
                active.endProgressPercent = progressPercent
            }
        }
    }

    /** Ends the current session, if any, and reports it once it is long enough. */
    fun endSession() {
        var durationSeconds = 0
        val report = synchronized(monitor) {
            val active = session ?: return@synchronized null
            session = null
            idleJob?.cancel()
            idleJob = null
            // Idle time past the timeout is not counted as reading, like the web reader.
            val endTimeMs = minOf(System.currentTimeMillis(), active.lastActivityMs + IDLE_TIMEOUT_MS)
            durationSeconds = ((endTimeMs - active.startTimeMs) / 1_000L).toInt()
            if (durationSeconds < MIN_SESSION_DURATION_SECONDS) {
                Log.i(
                    TAG,
                    "Session discarded: ${durationSeconds}s is below the $MIN_SESSION_DURATION_SECONDS s minimum " +
                        "(bookId=${active.bookId}); read longer for it to count",
                )
                null
            } else {
                ReadingSessionReport(
                    bookId = active.bookId,
                    bookType = active.bookType,
                    startTimeMs = active.startTimeMs,
                    endTimeMs = endTimeMs,
                    durationSeconds = durationSeconds,
                    startLocation = active.startLocation,
                    endLocation = active.endLocation,
                    startProgressPercent = active.startProgressPercent,
                    endProgressPercent = active.endProgressPercent,
                )
            }
        }
        if (report != null) {
            Log.i(TAG, "Reporting session: bookId=${report.bookId} duration=${durationSeconds}s")
            scope.launch {
                try {
                    reportSession(report)
                    Log.i(TAG, "Session reported to server: bookId=${report.bookId} duration=${durationSeconds}s")
                } catch (error: Exception) {
                    // A failed report is dropped; reading progress syncs separately.
                    Log.w(TAG, "Failed to report reading session (bookId=${report.bookId}): $error")
                }
            }
        }
    }

    private fun scheduleIdleCheck() {
        idleJob?.cancel()
        idleJob = scope.launch {
            while (true) {
                delay(IDLE_CHECK_INTERVAL_MS)
                val shouldEnd = synchronized(monitor) {
                    val active = session ?: return@synchronized true
                    System.currentTimeMillis() - active.lastActivityMs >= IDLE_TIMEOUT_MS
                }
                if (shouldEnd) {
                    Log.i(TAG, "Session ended after idle timeout")
                    endSession()
                    break
                }
            }
        }
    }

    private companion object {
        // Fixed string so R8 renaming keeps the logcat filter stable.
        const val TAG = "BookLoreSession"

        // The web reader ends a session after five minutes without page turns.
        const val IDLE_TIMEOUT_MS = 5 * 60 * 1_000L
        const val IDLE_CHECK_INTERVAL_MS = 30 * 1_000L
        // The web reader discards sessions shorter than thirty seconds.
        const val MIN_SESSION_DURATION_SECONDS = 30
    }
}

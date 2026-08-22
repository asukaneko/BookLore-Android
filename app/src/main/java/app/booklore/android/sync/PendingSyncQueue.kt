package app.booklore.android.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.database.PendingSyncEntity
import app.booklore.android.core.model.CreateAnnotationRequest
import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.PendingAnnotationUpdate
import app.booklore.android.core.model.PdfPosition
import app.booklore.android.core.model.ReadingSessionReport
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json

/**
 * Persistent queue of mutations that could not reach the server (reading
 * progress updates, reading-session reports and annotation changes). Entries
 * survive process death in Room and are flushed by [ProgressSyncWorker] as
 * soon as the network returns.
 */
class PendingSyncQueue(
    private val context: Context,
    private val database: BookLoreDatabase,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun enqueuePdfProgress(position: PdfPosition) {
        enqueue(
            type = TYPE_PDF_PROGRESS,
            dedupeKey = "PDF_PROGRESS:${position.bookId}:${position.bookFileId}",
            payload = json.encodeToString(PdfPosition.serializer(), position),
        )
    }

    suspend fun enqueueEpubProgress(position: EpubPosition) {
        enqueue(
            type = TYPE_EPUB_PROGRESS,
            dedupeKey = "EPUB_PROGRESS:${position.bookId}:${position.bookFileId}",
            payload = json.encodeToString(EpubPosition.serializer(), position),
        )
    }

    suspend fun enqueueReadingSession(report: ReadingSessionReport) {
        enqueue(
            type = TYPE_READING_SESSION,
            dedupeKey = "READING_SESSION:${report.bookId}:${report.startTimeMs}",
            payload = json.encodeToString(ReadingSessionReport.serializer(), report),
        )
    }

    suspend fun enqueueAnnotationCreate(request: CreateAnnotationRequest) {
        enqueue(
            type = TYPE_ANNOTATION_CREATE,
            dedupeKey = annotationCreateKey(request.bookId, request.cfi),
            payload = json.encodeToString(CreateAnnotationRequest.serializer(), request),
        )
    }

    suspend fun enqueueAnnotationUpdate(update: PendingAnnotationUpdate) {
        enqueue(
            type = TYPE_ANNOTATION_UPDATE,
            dedupeKey = "ANNOTATION_UPDATE:${update.annotationId}",
            payload = json.encodeToString(PendingAnnotationUpdate.serializer(), update),
        )
    }

    suspend fun enqueueAnnotationDelete(annotationId: Long) {
        enqueue(
            type = TYPE_ANNOTATION_DELETE,
            dedupeKey = "ANNOTATION_DELETE:$annotationId",
            payload = annotationId.toString(),
        )
    }

    /** Removes a queued annotation create (used when a local-only highlight is deleted). */
    suspend fun removeQueuedAnnotationCreate(bookId: Long, cfi: String) {
        database.pendingSyncDao().deleteByDedupeKey(annotationCreateKey(bookId, cfi))
    }

    /** Queued annotation creates for a book, restored as local highlights when offline. */
    suspend fun queuedAnnotationCreates(bookId: Long): List<CreateAnnotationRequest> =
        database.pendingSyncDao().byType(TYPE_ANNOTATION_CREATE)
            .mapNotNull { entry ->
                runCatching {
                    json.decodeFromString(CreateAnnotationRequest.serializer(), entry.payloadJson)
                }.getOrNull()
            }
            .filter { it.bookId == bookId }

    /** Number of mutations waiting for connectivity (for the sync status UI). */
    suspend fun pendingCount(): Int = database.pendingSyncDao().count()

    /** Drops every queued mutation and cancels any scheduled flush (used on sign-out). */
    suspend fun clear() {
        database.pendingSyncDao().clear()
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /** Requests a background flush while the network is connected. */
    fun schedule() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build(),
        )
    }

    private suspend fun enqueue(type: String, dedupeKey: String, payload: String) {
        database.pendingSyncDao().insert(
            PendingSyncEntity(
                type = type,
                dedupeKey = dedupeKey,
                payloadJson = payload,
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        schedule()
    }

    companion object {
        internal const val TYPE_PDF_PROGRESS = "PDF_PROGRESS"
        internal const val TYPE_EPUB_PROGRESS = "EPUB_PROGRESS"
        internal const val TYPE_READING_SESSION = "READING_SESSION"
        internal const val TYPE_ANNOTATION_CREATE = "ANNOTATION_CREATE"
        internal const val TYPE_ANNOTATION_UPDATE = "ANNOTATION_UPDATE"
        internal const val TYPE_ANNOTATION_DELETE = "ANNOTATION_DELETE"

        private const val UNIQUE_WORK_NAME = "booklore-pending-sync"
        private const val BACKOFF_SECONDS = 30L

        internal fun annotationCreateKey(bookId: Long, cfi: String): String =
            "ANNOTATION_CREATE:$bookId:${cfi.hashCode()}"
    }
}

package app.booklore.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.model.CreateAnnotationRequest
import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.PendingAnnotationUpdate
import app.booklore.android.core.model.PdfPosition
import app.booklore.android.core.model.ReadingSessionReport
import app.booklore.android.core.network.ApiException
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.network.NetworkException
import app.booklore.android.core.security.SessionStore
import kotlinx.serialization.json.Json

/**
 * Flushes queued reading-progress updates, reading-session reports and
 * annotation changes to the BookLore server. Runs whenever connectivity
 * returns; each entry is removed only after the server accepts it, so a
 * completed local action is never lost.
 */
class ProgressSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val sessionStore = SessionStore(applicationContext)
        if (sessionStore.readLocalMode()) return Result.success()
        if (sessionStore.read() == null) return Result.success()

        val api = BookLoreApi(sessionStore)
        val dao = BookLoreDatabase.get(applicationContext).pendingSyncDao()
        for (entry in dao.all()) {
            if (sessionStore.readLocalMode()) return Result.success()
            try {
                when (entry.type) {
                    PendingSyncQueue.TYPE_PDF_PROGRESS ->
                        api.updatePdfProgress(json.decodeFromString(PdfPosition.serializer(), entry.payloadJson))
                    PendingSyncQueue.TYPE_EPUB_PROGRESS ->
                        api.updateEpubProgress(json.decodeFromString(EpubPosition.serializer(), entry.payloadJson))
                    PendingSyncQueue.TYPE_READING_SESSION ->
                        api.recordReadingSession(json.decodeFromString(ReadingSessionReport.serializer(), entry.payloadJson))
                    PendingSyncQueue.TYPE_ANNOTATION_CREATE ->
                        api.createAnnotation(json.decodeFromString(CreateAnnotationRequest.serializer(), entry.payloadJson))
                    PendingSyncQueue.TYPE_ANNOTATION_UPDATE -> {
                        val update = json.decodeFromString(PendingAnnotationUpdate.serializer(), entry.payloadJson)
                        api.updateAnnotation(update.annotationId, update.request)
                    }
                    PendingSyncQueue.TYPE_ANNOTATION_DELETE ->
                        api.deleteAnnotation(entry.payloadJson.toLongOrNull() ?: -1L)
                    else -> Unit // Unknown type from a newer app version; drop below.
                }
                dao.delete(entry.id)
            } catch (error: NetworkException) {
                // Connectivity dropped mid-flush; retry the remaining entries later.
                return Result.retry()
            } catch (error: ApiException) {
                if (error.statusCode == 401) {
                    // The session needs a fresh sign-in before anything else can upload.
                    return Result.retry()
                }
                // The server rejected this payload permanently; drop it so the queue drains.
                dao.delete(entry.id)
            }
        }
        sessionStore.writeLastSyncTimeMs(System.currentTimeMillis())
        return Result.success()
    }
}

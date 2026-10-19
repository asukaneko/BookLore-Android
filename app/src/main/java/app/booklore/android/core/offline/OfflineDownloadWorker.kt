package app.booklore.android.core.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.booklore.android.core.cache.BookCoverStore
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.security.SessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

/** Executes one durable download and writes progress back to Room for the UI. */
class OfflineDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val taskKey = inputData.getString(KEY_TASK) ?: return Result.failure()
        val database = BookLoreDatabase.get(applicationContext)
        val dao = database.downloadTaskDao()
        val task = dao.find(taskKey) ?: return Result.success()
        if (task.status == OfflineDownloadStatus.CANCELLED.name) return Result.success()
        val sessionStore = SessionStore(applicationContext)
        if (sessionStore.readLocalMode()) return Result.retry()
        if (dao.markStatus(taskKey, OfflineDownloadStatus.DOWNLOADING.name, System.currentTimeMillis()) == 0) {
            return Result.success()
        }

        val store = OfflineBookStore(applicationContext)
        val destination = store.fileFor(task.bookId, task.bookFileId, task.bookType)
            ?: return fail(dao, taskKey, "Unsupported offline format")
        val api = BookLoreApi(sessionStore)
        var lastProgressWriteMs = 0L
        try {
            when {
                task.bookType.equals("EPUB", ignoreCase = true) || task.bookType.equals("epub", ignoreCase = true) ->
                    api.downloadEpub(task.bookId, task.bookType, destination) { bytesRead, totalBytes ->
                        persistProgressIfDue(dao, taskKey, bytesRead, totalBytes, lastProgressWriteMs).also {
                            if (it) lastProgressWriteMs = System.currentTimeMillis()
                        }
                    }
                task.bookType.equals("PDF", ignoreCase = true) || task.bookType.equals("pdf", ignoreCase = true) ->
                    api.downloadPdf(task.bookId, task.bookType, destination) { bytesRead, totalBytes ->
                        persistProgressIfDue(dao, taskKey, bytesRead, totalBytes, lastProgressWriteMs).also {
                            if (it) lastProgressWriteMs = System.currentTimeMillis()
                        }
                    }
                else -> return fail(dao, taskKey, "Unsupported offline format")
            }
            if (!store.isValidOfflineFile(destination)) {
                store.delete(task.bookId, task.bookFileId, task.bookType)
                return fail(dao, taskKey, "Downloaded file is invalid")
            }
            val totalBytes = destination.length()
            // Keep the cover with the book, even when the book has never been opened in the UI.
            BookCoverStore(applicationContext, sessionStore).resolve(task.bookId)
            dao.markFinished(
                taskKey = taskKey,
                status = OfflineDownloadStatus.COMPLETED.name,
                bytesRead = totalBytes,
                totalBytes = totalBytes,
                updatedAtMs = System.currentTimeMillis(),
            )
            return Result.success()
        } catch (error: CancellationException) {
            store.delete(task.bookId, task.bookFileId, task.bookType)
            dao.markFailed(
                taskKey = taskKey,
                status = OfflineDownloadStatus.CANCELLED.name,
                errorMessage = null,
                updatedAtMs = System.currentTimeMillis(),
            )
            throw error
        } catch (error: Exception) {
            if (sessionStore.readLocalMode()) {
                dao.markFailed(
                    taskKey = taskKey,
                    status = OfflineDownloadStatus.PENDING.name,
                    errorMessage = null,
                    updatedAtMs = System.currentTimeMillis(),
                )
                return Result.retry()
            }
            store.delete(task.bookId, task.bookFileId, task.bookType)
            return fail(dao, taskKey, error.message ?: "Download failed")
        }
    }

    private fun persistProgressIfDue(
        dao: app.booklore.android.core.database.DownloadTaskDao,
        taskKey: String,
        bytesRead: Long,
        totalBytes: Long?,
        lastProgressWriteMs: Long,
    ): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastProgressWriteMs < PROGRESS_WRITE_INTERVAL_MS) return false
        runBlocking {
            dao.updateProgress(
                taskKey = taskKey,
                status = OfflineDownloadStatus.DOWNLOADING.name,
                bytesRead = bytesRead,
                totalBytes = totalBytes,
                errorMessage = null,
                updatedAtMs = now,
            )
        }
        return true
    }

    private suspend fun fail(
        dao: app.booklore.android.core.database.DownloadTaskDao,
        taskKey: String,
        message: String,
    ): Result {
        dao.markFailed(
            taskKey = taskKey,
            status = OfflineDownloadStatus.FAILED.name,
            errorMessage = message,
            updatedAtMs = System.currentTimeMillis(),
        )
        return Result.failure()
    }

    companion object {
        const val KEY_TASK = "task_key"
        private const val PROGRESS_WRITE_INTERVAL_MS = 400L
    }
}

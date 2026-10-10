package app.booklore.android.core.offline

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.database.DownloadTaskEntity
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.displayTitle
import kotlinx.coroutines.flow.Flow

/** Owns durable offline download requests and their WorkManager lifecycle. */
class OfflineDownloadQueue(
    context: Context,
    private val database: BookLoreDatabase,
    private val store: OfflineBookStore,
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun observeTasks(): Flow<List<DownloadTaskEntity>> = database.downloadTaskDao().observeAll()

    suspend fun enqueue(book: Book) {
        val file = book.primaryFile ?: return
        val fileId = file.id ?: return
        val bookType = file.bookType ?: file.extension ?: return
        val taskKey = OfflineDownloadTask.taskKey(book.id, fileId)
        val now = System.currentTimeMillis()
        val existing = database.downloadTaskDao().find(taskKey)
        if (existing?.status == OfflineDownloadStatus.PENDING.name ||
            existing?.status == OfflineDownloadStatus.DOWNLOADING.name
        ) {
            return
        }
        enqueueTask(
            DownloadTaskEntity(
                taskKey = taskKey,
                bookId = book.id,
                bookFileId = fileId,
                bookType = bookType,
                bookTitle = book.displayTitle(),
                status = OfflineDownloadStatus.PENDING.name,
                createdAtMs = existing?.createdAtMs ?: now,
                updatedAtMs = now,
            ),
        )
    }

    suspend fun retry(task: OfflineDownloadTask) {
        val now = System.currentTimeMillis()
        enqueueTask(
            DownloadTaskEntity(
                taskKey = task.taskKey,
                bookId = task.bookId,
                bookFileId = task.bookFileId,
                bookType = task.bookType,
                bookTitle = task.bookTitle,
                status = OfflineDownloadStatus.PENDING.name,
                bytesRead = 0L,
                totalBytes = null,
                createdAtMs = task.createdAtMs,
                updatedAtMs = now,
            ),
        )
    }

    suspend fun resumePending() {
        database.downloadTaskDao().all()
            .filter { it.status == OfflineDownloadStatus.PENDING.name }
            .forEach { task ->
                enqueueTask(task.copy(status = OfflineDownloadStatus.PENDING.name, updatedAtMs = System.currentTimeMillis()))
            }
    }

    private suspend fun enqueueTask(task: DownloadTaskEntity) {
        database.downloadTaskDao().upsert(task)
        workManager.enqueueUniqueWork(
            workName(task.taskKey),
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<OfflineDownloadWorker>()
                .setInputData(workDataOf(OfflineDownloadWorker.KEY_TASK to task.taskKey))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build(),
        )
    }

    suspend fun cancel(task: OfflineDownloadTask) {
        workManager.cancelUniqueWork(workName(task.taskKey))
        database.downloadTaskDao().markFailed(
            taskKey = task.taskKey,
            status = OfflineDownloadStatus.CANCELLED.name,
            errorMessage = null,
            updatedAtMs = System.currentTimeMillis(),
        )
        store.delete(task.bookId, task.bookFileId, task.bookType)
    }

    suspend fun delete(task: OfflineDownloadTask) {
        if (task.isActive) {
            workManager.cancelUniqueWork(workName(task.taskKey))
        }
        store.delete(task.bookId, task.bookFileId, task.bookType)
        database.downloadTaskDao().delete(task.taskKey)
    }

    suspend fun clear() {
        database.downloadTaskDao().all().forEach { task ->
            workManager.cancelUniqueWork(workName(task.taskKey))
        }
        database.downloadTaskDao().clear()
    }

    private fun workName(taskKey: String): String = "$WORK_PREFIX$taskKey"

    companion object {
        private const val WORK_PREFIX = "booklore-download-"
    }
}

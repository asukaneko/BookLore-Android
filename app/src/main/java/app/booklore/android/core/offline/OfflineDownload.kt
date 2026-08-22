package app.booklore.android.core.offline

import app.booklore.android.core.database.DownloadTaskEntity
import app.booklore.android.core.model.Book

enum class OfflineDownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED,
}

data class OfflineDownloadTask(
    val taskKey: String,
    val bookId: Long,
    val bookFileId: Long,
    val bookType: String,
    val bookTitle: String,
    val status: OfflineDownloadStatus,
    val bytesRead: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    val progress: Float?
        get() = totalBytes?.takeIf { it > 0L }?.let { (bytesRead.toFloat() / it).coerceIn(0f, 1f) }

    val isActive: Boolean
        get() = status == OfflineDownloadStatus.PENDING || status == OfflineDownloadStatus.DOWNLOADING

    companion object {
        fun taskKey(bookId: Long, bookFileId: Long): String = "$bookId:$bookFileId"
    }
}

fun DownloadTaskEntity.toModel(): OfflineDownloadTask = OfflineDownloadTask(
    taskKey = taskKey,
    bookId = bookId,
    bookFileId = bookFileId,
    bookType = bookType,
    bookTitle = bookTitle,
    status = runCatching { OfflineDownloadStatus.valueOf(status) }
        .getOrDefault(OfflineDownloadStatus.FAILED),
    bytesRead = bytesRead,
    totalBytes = totalBytes,
    errorMessage = errorMessage,
    createdAtMs = createdAtMs,
    updatedAtMs = updatedAtMs,
)

fun Book.downloadTaskKey(): String? = primaryFile?.id?.let { OfflineDownloadTask.taskKey(id, it) }

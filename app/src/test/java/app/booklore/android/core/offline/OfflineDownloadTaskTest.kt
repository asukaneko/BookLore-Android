package app.booklore.android.core.offline

import app.booklore.android.core.database.DownloadTaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineDownloadTaskTest {
    @Test
    fun `progress is bounded and unavailable when total size is unknown`() {
        val task = task(bytesRead = 120L, totalBytes = 100L)

        assertEquals(1f, task.progress)
        assertNull(task.copy(totalBytes = null).progress)
    }

    @Test
    fun `unknown persisted status is treated as failed`() {
        val task = task(status = "REMOVED")

        assertEquals(OfflineDownloadStatus.FAILED, task.status)
    }

    @Test
    fun `task keys are stable per book file`() {
        assertEquals("42:7", OfflineDownloadTask.taskKey(42L, 7L))
    }

    private fun task(
        bytesRead: Long = 0L,
        totalBytes: Long? = 1L,
        status: String = OfflineDownloadStatus.DOWNLOADING.name,
    ): OfflineDownloadTask = DownloadTaskEntity(
        taskKey = "42:7",
        bookId = 42L,
        bookFileId = 7L,
        bookType = "EPUB",
        bookTitle = "Test book",
        status = status,
        bytesRead = bytesRead,
        totalBytes = totalBytes,
        createdAtMs = 1L,
        updatedAtMs = 1L,
    ).toModel()
}

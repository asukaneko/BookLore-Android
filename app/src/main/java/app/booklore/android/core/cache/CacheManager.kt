package app.booklore.android.core.cache

import android.content.Context
import app.booklore.android.core.offline.OfflineBookStore
import app.booklore.android.core.database.BookLoreDatabase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CacheBucketId {
    COVER_IMAGES,
    READER_FILES,
    OFFLINE_BOOKS,
    OFFLINE_CATALOG,
}

data class CacheBucket(
    val id: CacheBucketId,
    val bytes: Long = 0,
    val itemCount: Int = 0,
)

data class CacheSnapshot(
    val buckets: List<CacheBucket> = emptyList(),
) {
    val totalBytes: Long get() = buckets.sumOf(CacheBucket::bytes)
}

/** Reads and clears the account-scoped data that can be safely recreated. */
class CacheManager(
    context: Context,
    private val database: BookLoreDatabase,
) {
    private val appContext = context.applicationContext
    private val readerCacheDirectory = File(appContext.filesDir, "reader-cache")
    private val coverDirectory = File(appContext.filesDir, "book-covers")
    private val offlineBookStore = OfflineBookStore(appContext)

    suspend fun snapshot(): CacheSnapshot = withContext(Dispatchers.IO) {
        val bookCount = database.catalogDao().bookCount()
        CacheSnapshot(
            buckets = listOf(
                directoryBucket(CacheBucketId.COVER_IMAGES, appContext.cacheDir).let { cached ->
                    val saved = directoryBucket(CacheBucketId.COVER_IMAGES, coverDirectory)
                    cached.copy(bytes = cached.bytes + saved.bytes, itemCount = cached.itemCount + saved.itemCount)
                },
                directoryBucket(CacheBucketId.READER_FILES, readerCacheDirectory),
                offlineBookStore.snapshot().let { storage ->
                    CacheBucket(CacheBucketId.OFFLINE_BOOKS, storage.bytes, storage.itemCount)
                },
                CacheBucket(
                    id = CacheBucketId.OFFLINE_CATALOG,
                    bytes = databaseFilesSize().takeIf { bookCount > 0 } ?: 0,
                    itemCount = bookCount,
                ),
            ),
        )
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        clearDirectoryContents(appContext.cacheDir)
        coverDirectory.deleteRecursively()
        readerCacheDirectory.deleteRecursively()
        offlineBookStore.clear()
        database.catalogDao().clearCatalog()
        database.annotationDao().clear()
    }

    private fun directoryBucket(id: CacheBucketId, directory: File): CacheBucket {
        val files = directory.walkTopDown().filter(File::isFile).toList()
        return CacheBucket(id = id, bytes = files.sumOf(File::length), itemCount = files.size)
    }

    private fun databaseFilesSize(): Long {
        val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
        return sequenceOf(
            databaseFile,
            File("${databaseFile.path}-wal"),
            File("${databaseFile.path}-shm"),
        ).filter(File::exists).sumOf(File::length)
    }

    private fun clearDirectoryContents(directory: File) {
        directory.listFiles()?.forEach { it.deleteRecursively() }
    }

    private companion object {
        const val DATABASE_NAME = "booklore.db"
    }
}

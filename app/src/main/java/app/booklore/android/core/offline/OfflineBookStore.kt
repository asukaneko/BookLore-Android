package app.booklore.android.core.offline

import android.content.Context
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.BookFile
import app.booklore.android.core.model.supportsEpubReader
import app.booklore.android.core.model.supportsPdfReader
import app.booklore.android.core.network.BookLoreApi
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A durable, user-managed copy of a book that can be opened without a server connection. */
class OfflineBookStore(context: Context) {
    private val directory = File(context.applicationContext.filesDir, DIRECTORY_NAME)

    fun fileFor(book: Book): File? {
        val bookFile = book.primaryFile ?: return null
        val fileId = bookFile.id ?: return null
        val extension = bookFile.offlineExtension() ?: return null
        return File(directory, "${book.id}-$fileId.$extension")
    }

    fun isDownloaded(book: Book): Boolean = fileFor(book)?.isValidOfflineFile() == true

    fun downloadedBookIds(books: List<Book>): Set<Long> = books
        .filter(::isDownloaded)
        .mapTo(mutableSetOf(), Book::id)

    suspend fun download(
        api: BookLoreApi,
        book: Book,
        onProgress: (bytesRead: Long, totalBytes: Long?) -> Unit = { _, _ -> },
    ): File = withContext(Dispatchers.IO) {
        val bookFile = book.primaryFile ?: error("Book has no readable file")
        val destination = fileFor(book) ?: error("Book format is not supported for offline reading")
        when {
            book.supportsEpubReader() -> api.downloadEpub(book.id, bookFile.bookType ?: "EPUB", destination, onProgress)
            book.supportsPdfReader() -> api.downloadPdf(book.id, bookFile.bookType ?: "PDF", destination, onProgress)
            else -> error("Book format is not supported for offline reading")
        }
        require(destination.isValidOfflineFile()) { "The downloaded book is invalid" }
        destination.setLastModified(System.currentTimeMillis())
        destination
    }

    suspend fun delete(book: Book) = withContext(Dispatchers.IO) {
        fileFor(book)?.delete()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        directory.deleteRecursively()
    }

    fun snapshot(): OfflineBookStorage = directory.listFiles()
        ?.filter(File::isFile)
        ?.let { files ->
            OfflineBookStorage(
                bytes = files.sumOf(File::length),
                itemCount = files.count { it.isValidOfflineFile() },
            )
        }
        ?: OfflineBookStorage()

    private fun BookFile.offlineExtension(): String? = when {
        bookType.equals("EPUB", ignoreCase = true) || extension.equals("epub", ignoreCase = true) -> "epub"
        bookType.equals("PDF", ignoreCase = true) || extension.equals("pdf", ignoreCase = true) -> "pdf"
        else -> null
    }

    private fun File.isValidOfflineFile(): Boolean {
        if (!isFile || length() < 4L) return false
        return runCatching {
            inputStream().buffered().use { input ->
                val first = input.read()
                val second = input.read()
                first == 'P'.code && second == 'K'.code || first == '%'.code && second == 'P'.code
            }
        }.getOrDefault(false)
    }

    private companion object {
        const val DIRECTORY_NAME = "offline-books"
    }
}

data class OfflineBookStorage(
    val bytes: Long = 0,
    val itemCount: Int = 0,
)

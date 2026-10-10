package app.booklore.android.core.offline

import app.booklore.android.core.model.Book
import app.booklore.android.core.model.supportsEpubReader
import app.booklore.android.core.model.supportsPdfReader
import java.io.File
import java.io.RandomAccessFile
import java.util.zip.ZipFile

/** Finds complete book copies; partial downloads and sparse streaming files are never listed. */
class LocalBookFiles(private val filesDirectory: File) {
    fun find(book: Book): File? {
        val fileId = book.primaryFile?.id ?: return null
        val extension = when {
            book.supportsEpubReader() -> "epub"
            book.supportsPdfReader() -> "pdf"
            else -> return null
        }
        return listOf("offline-books", "reader-cache")
            .asSequence()
            .map { directory -> File(filesDirectory, "$directory/${book.id}-$fileId.$extension") }
            .firstOrNull(::isReadableBookFile)
    }

    fun availableBookIds(books: List<Book>): Set<Long> = books
        .filter { find(it) != null }
        .mapTo(mutableSetOf(), Book::id)
}

internal fun isReadableBookFile(file: File): Boolean {
    if (!file.isFile || file.length() < 5L) return false
    return runCatching {
        when (file.extension.lowercase()) {
            "epub" -> ZipFile(file).use { archive ->
                archive.getEntry("META-INF/container.xml") != null
            }
            "pdf" -> RandomAccessFile(file, "r").use { input ->
                val signature = ByteArray(5)
                input.readFully(signature)
                val tail = ByteArray(minOf(file.length(), 4096L).toInt())
                input.seek(file.length() - tail.size)
                input.readFully(tail)
                signature.toString(Charsets.US_ASCII) == "%PDF-" &&
                    tail.toString(Charsets.US_ASCII).contains("%%EOF")
            }
            else -> false
        }
    }.getOrDefault(false)
}

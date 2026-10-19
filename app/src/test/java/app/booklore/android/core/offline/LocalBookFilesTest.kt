package app.booklore.android.core.offline

import app.booklore.android.core.model.Book
import app.booklore.android.core.model.BookFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalBookFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `cover lookup finds both downloaded books and existing reader caches by book ID`() {
        val downloaded = epubFile("offline-books/1-7.epub")
        val cached = pdfFile("reader-cache/2-7.pdf")

        assertEquals(downloaded, store().find(1L))
        assertEquals(cached, store().find(2L))
    }

    @Test
    fun `cover lookup does not pick another book or an unfinished file`() {
        epubFile("offline-books/12-7.epub")
        epubFile("offline-books/1-7.epub.part")
        pdfFile("reader-cache/1-7.pdf.streaming")

        assertNull(store().find(1L))
    }

    @Test
    fun `local library includes downloads and complete reading caches`() {
        val epub = book(1, "EPUB")
        val pdf = book(2, "PDF")
        epubFile("offline-books/1-7.epub")
        pdfFile("reader-cache/2-7.pdf")

        assertEquals(setOf(1L, 2L), store().availableBookIds(listOf(epub, pdf, book(3, "EPUB"))))
    }

    @Test
    fun `durable download is preferred over the reading cache`() {
        val download = epubFile("offline-books/1-7.epub")
        epubFile("reader-cache/1-7.epub")

        assertEquals(download, store().find(book(1, "EPUB")))
    }

    @Test
    fun `valid reading cache is used when the download is corrupt`() {
        file("offline-books/1-7.epub").writeText("PK unfinished download")
        val cached = epubFile("reader-cache/1-7.epub")

        assertEquals(cached, store().find(book(1, "EPUB")))
    }

    @Test
    fun `partial downloads and streaming files are excluded`() {
        epubFile("offline-books/1-7.epub.part")
        pdfFile("reader-cache/2-7.stream.pdf")

        assertEquals(emptySet<Long>(), store().availableBookIds(listOf(book(1, "EPUB"), book(2, "PDF"))))
    }

    @Test
    fun `truncated EPUB archives are excluded even with a ZIP header`() {
        val epub = epubFile("reader-cache/1-7.epub")
        val bytes = epub.readBytes()
        epub.writeBytes(bytes.copyOf(bytes.size / 2))

        assertNull(store().find(book(1, "EPUB")))
    }

    @Test
    fun `PDF without an EOF marker is excluded`() {
        file("reader-cache/1-7.pdf").writeText("%PDF-1.4\n unfinished")

        assertNull(store().find(book(1, "PDF")))
    }

    @Test
    fun `server error documents are excluded`() {
        val pdf = file("reader-cache/1-7.pdf").apply { writeText("<html>Server error</html>%%EOF") }

        assertFalse(isReadableBookFile(pdf))
    }

    @Test
    fun `unsupported books and unknown file IDs are excluded`() {
        epubFile("offline-books/1-7.cbz")

        assertNull(store().find(book(1, "CBZ")))
        assertNull(store().find(Book(id = 2)))
    }

    @Test
    fun `book file identity must match the saved file`() {
        epubFile("offline-books/1-8.epub")

        assertNull(store().find(book(1, "EPUB")))
    }

    @Test
    fun `file extensions from older catalog entries are supported`() {
        val cached = epubFile("reader-cache/1-7.epub")

        assertEquals(cached, store().find(Book(id = 1, primaryFile = BookFile(id = 7, extension = "epub"))))
    }

    private fun store() = LocalBookFiles(temporary.root)

    private fun book(id: Long, type: String) = Book(id = id, primaryFile = BookFile(id = 7, bookType = type))

    private fun file(path: String) = File(temporary.root, path).apply { parentFile!!.mkdirs() }

    private fun pdfFile(path: String) = file(path).apply { writeText("%PDF-1.4\n sample complete PDF\n%%EOF\n") }

    private fun epubFile(path: String) = file(path).apply {
        ZipOutputStream(outputStream()).use { archive ->
            archive.putNextEntry(ZipEntry("META-INF/container.xml"))
            archive.write("<container/>".toByteArray())
            archive.closeEntry()
        }
    }
}

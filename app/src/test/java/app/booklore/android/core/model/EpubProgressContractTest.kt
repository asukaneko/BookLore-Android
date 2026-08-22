package app.booklore.android.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpubProgressContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `maps the server EPUB progress fields for EPUB resume`() {
        val book = json.decodeFromString(
            Book.serializer(),
            """
            {
              "id": 42,
              "title": "A Reader Test",
              "primaryFile": { "id": 12, "bookType": "EPUB" },
              "epubProgress": {
                "cfi": "epubcfi(/6/4!/4/2)",
                "href": "chapter-2.xhtml",
                "percentage": 35.5
              }
            }
            """.trimIndent(),
        )

        assertTrue(book.supportsEpubReader())
        assertEquals("epubcfi(/6/4!/4/2)", book.epubProgress?.cfi)
        assertEquals("chapter-2.xhtml", book.epubProgress?.href)
        assertEquals(35.5f, book.epubProgress?.percentage)
    }

    @Test
    fun `uses independent defaults for reader appearance`() {
        val settings = EpubReaderSettings()

        assertEquals(EpubReaderTheme.SYSTEM, settings.theme)
        assertEquals(1f, settings.fontScale)
        assertEquals(FontOption.SANS, settings.font)
        assertEquals(1f, settings.pageMargins)
    }

    @Test
    fun `defaults PDF reading to paginated mode and keeps the selected mode independent`() {
        assertEquals(PdfReadingMode.PAGINATED, PdfReaderSettings().readingMode)
        assertEquals(
            PdfReadingMode.CONTINUOUS,
            PdfReaderSettings(readingMode = PdfReadingMode.CONTINUOUS).readingMode,
        )
    }

    @Test
    fun `maps the server PDF progress fields for PDF resume`() {
        val book = json.decodeFromString(
            Book.serializer(),
            """
            {
              "id": 42,
              "title": "A PDF Test",
              "primaryFile": { "id": 12, "bookType": "PDF" },
              "pdfProgress": {
                "page": 14,
                "percentage": 46.7
              }
            }
            """.trimIndent(),
        )

        assertTrue(book.supportsPdfReader())
        assertEquals(14, book.pdfProgress?.page)
        assertEquals(46.7f, book.pdfProgress?.percentage)
    }
}

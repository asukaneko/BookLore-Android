package app.booklore.android.core.network

import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.PdfPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class BookLoreApiTest {
    @Test
    fun `normalizes a secure server URL`() {
        assertEquals(
            "https://booklore.example",
            BookLoreApi.normalizeBaseUrl(" https://booklore.example/ ", allowHttp = false),
        )
    }

    @Test
    fun `rejects HTTP unless user explicitly allowed it`() {
        assertInvalid { BookLoreApi.normalizeBaseUrl("http://192.168.1.12:6060", allowHttp = false) }
        assertEquals(
            "http://192.168.1.12:6060",
            BookLoreApi.normalizeBaseUrl("http://192.168.1.12:6060", allowHttp = true),
        )
    }

    @Test
    fun `rejects URLs with credentials or paths`() {
        assertInvalid { BookLoreApi.normalizeBaseUrl("https://reader:secret@booklore.example", allowHttp = false) }
        assertInvalid { BookLoreApi.normalizeBaseUrl("https://booklore.example/booklore", allowHttp = false) }
    }

    @Test
    fun `builds the BookLore EPUB progress contract and clamps progress`() {
        val payload = EpubPosition(
            bookId = 42,
            bookFileId = 12,
            cfi = "epubcfi(/6/4!/4/2)",
            href = "chapter-2.xhtml",
            progressPercent = 124f,
        ).toBookLoreProgressPayload()

        val fileProgress = payload["fileProgress"]!!.jsonObject
        assertEquals(42L, payload["bookId"]!!.jsonPrimitive.long)
        assertEquals(12L, fileProgress["bookFileId"]!!.jsonPrimitive.long)
        assertEquals("epubcfi(/6/4!/4/2)", fileProgress["positionData"]!!.jsonPrimitive.content)
        assertEquals("chapter-2.xhtml", fileProgress["positionHref"]!!.jsonPrimitive.content)
        assertEquals(100f, fileProgress["progressPercent"]!!.jsonPrimitive.float)
    }

    @Test
    fun `omits blank EPUB locators from the progress contract`() {
        val payload = EpubPosition(
            bookId = 42,
            bookFileId = 12,
            cfi = " ",
            href = "",
            progressPercent = -2f,
        ).toBookLoreProgressPayload()
        val fileProgress = payload["fileProgress"]!!.jsonObject

        assertFalse(fileProgress.containsKey("positionData"))
        assertFalse(fileProgress.containsKey("positionHref"))
        assertEquals(0f, fileProgress["progressPercent"]!!.jsonPrimitive.float)
    }

    @Test
    fun `builds the BookLore PDF progress contract and clamps values`() {
        val payload = PdfPosition(
            bookId = 42,
            bookFileId = 12,
            page = 14,
            progressPercent = 124f,
        ).toBookLoreProgressPayload()

        val fileProgress = payload["fileProgress"]!!.jsonObject
        assertEquals(42L, payload["bookId"]!!.jsonPrimitive.long)
        assertEquals(12L, fileProgress["bookFileId"]!!.jsonPrimitive.long)
        assertEquals("14", fileProgress["positionData"]!!.jsonPrimitive.content)
        assertFalse(fileProgress.containsKey("positionHref"))
        assertEquals(100f, fileProgress["progressPercent"]!!.jsonPrimitive.float)
    }

    @Test
    fun `uses the first page and zero percent as the PDF lower bounds`() {
        val payload = PdfPosition(
            bookId = 42,
            bookFileId = 12,
            page = 0,
            progressPercent = -2f,
        ).toBookLoreProgressPayload()
        val fileProgress = payload["fileProgress"]!!.jsonObject

        assertEquals("1", fileProgress["positionData"]!!.jsonPrimitive.content)
        assertEquals(0f, fileProgress["progressPercent"]!!.jsonPrimitive.float)
    }

    @Test
    fun `extracts a stream size from Content-Range`() {
        assertEquals(1_048_576L, contentRangeTotalBytes("bytes 0-262143/1048576"))
        assertNull(contentRangeTotalBytes("bytes */*"))
        assertNull(contentRangeTotalBytes("invalid/1048576"))
        assertNull(contentRangeTotalBytes("invalid"))
    }

    @Test
    fun `only accepts partial responses with a positive total size for streaming`() {
        assertTrue(supportsByteRangeStreaming(206, 1_048_576L))
        assertFalse(supportsByteRangeStreaming(200, 1_048_576L))
        assertFalse(supportsByteRangeStreaming(206, null))
        assertFalse(supportsByteRangeStreaming(206, 0L))
    }

    private fun assertInvalid(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected input validation failure.
        }
    }
}

package app.booklore.android.core.cache

import java.io.File
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EpubCoverExtractorTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test
    fun `EPUB 3 cover properties resolve an encoded relative image path`() {
        val epub = epub(
            "<manifest><item id='front' properties='nav cover-image' media-type='image/png' href='../Images/front%20cover.png'/></manifest>",
            mapOf("Images/front cover.png" to png),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `EPUB 2 metadata identifies a cover whose filename is not cover`() {
        val epub = epub(
            "<metadata><meta name='cover' content='jacket'/></metadata>" +
                "<manifest><item id='jacket' media-type='image/png' href='images/jacket.png'/></manifest>",
            mapOf("OPS/images/jacket.png" to png),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `guide cover pages resolve their own relative image paths`() {
        val epub = epub(
            "<guide><reference type='cover' href='Text/title.xhtml#cover'/></guide>",
            mapOf(
                "OPS/Text/title.xhtml" to "<html xmlns='http://www.w3.org/1999/xhtml'><body><img src='../Images/jacket.png'/></body></html>".toByteArray(),
                "OPS/Images/jacket.png" to png,
            ),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `EPUB 2 cover page DOCTYPE is accepted without fetching its external DTD`() {
        val epub = epub(
            "<guide><reference type='cover' href='Text/title.xhtml'/></guide>",
            mapOf(
                "OPS/Text/title.xhtml" to ("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.1//EN' 'file:///missing-cover-test.dtd'>" +
                    "<html xmlns='http://www.w3.org/1999/xhtml'><body><img src='../Images/jacket.png'/></body></html>").toByteArray(),
                "OPS/Images/jacket.png" to png,
            ),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `SVG cover wrappers can refer to a local raster cover`() {
        val epub = epub(
            "<manifest><item id='cover' media-type='image/svg+xml' href='cover.svg'/></manifest>",
            mapOf(
                "OPS/cover.svg" to "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'><image xlink:href='jacket.png'/></svg>".toByteArray(),
                "OPS/jacket.png" to png,
            ),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `external images and archive path traversal are ignored`() {
        for (href in listOf("https://example.invalid/cover.png", "file:///cover.png", "../../outside.png")) {
            val epub = epub(
                "<manifest><item id='front' properties='cover-image' media-type='image/png' href='$href'/></manifest>",
                mapOf("outside.png" to png),
            )
            assertNull(EpubCoverExtractor.extract(epub))
        }
    }

    @Test
    fun `missing cover metadata falls back to a cover-named manifest image`() {
        val epub = epub(
            "<manifest><item id='picture' media-type='image/png' href='images/cover.png'/></manifest>",
            mapOf("OPS/images/cover.png" to png),
        )
        assertArrayEquals(png, EpubCoverExtractor.extract(epub))
    }

    @Test
    fun `an EPUB without a cover or with a corrupt archive remains readable without an image`() {
        assertNull(EpubCoverExtractor.extract(epub("<manifest/>", emptyMap())))
        val broken = temporary.newFile("broken.epub").apply { writeText("PK incomplete") }
        assertNull(EpubCoverExtractor.extract(broken))
    }

    private fun epub(packageXml: String, entries: Map<String, ByteArray>): File = temporary.newFile().apply {
        ZipOutputStream(outputStream()).use { archive ->
            val documents = mapOf(
                "META-INF/container.xml" to "<container xmlns='urn:oasis:names:tc:opendocument:xmlns:container'><rootfiles><rootfile full-path='OPS/package.opf'/></rootfiles></container>".toByteArray(),
                "OPS/package.opf" to "<package xmlns='http://www.idpf.org/2007/opf'>$packageXml</package>".toByteArray(),
            ) + entries
            for ((path, bytes) in documents) {
                archive.putNextEntry(ZipEntry(path))
                archive.write(bytes)
                archive.closeEntry()
            }
        }
    }

    private val png = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
    )
}

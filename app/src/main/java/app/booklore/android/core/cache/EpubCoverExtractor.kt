package app.booklore.android.core.cache

import java.io.ByteArrayInputStream
import java.io.File
import java.io.StringReader
import java.net.URI
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** Reads EPUB 2/3 cover metadata without unpacking the publication onto the filesystem. */
internal object EpubCoverExtractor {
    fun extract(file: File): ByteArray? = runCatching {
        ZipFile(file).use { archive ->
            val container = archive.xml("META-INF/container.xml") ?: return@use null
            val packagePath = container.elements("rootfile").firstNotNullOfOrNull {
                it.getAttribute("full-path").takeIf(String::isNotBlank)
            } ?: return@use null
            val document = archive.xml(packagePath) ?: return@use null
            val items = document.elements("item")
            val coverId = document.elements("meta").firstOrNull {
                it.getAttribute("name").equals("cover", ignoreCase = true)
            }?.getAttribute("content")
            val images = items.filter { it.getAttribute("media-type").startsWith("image/") &&
                it.getAttribute("media-type") != "image/svg+xml" }
            val preferred = images.filter { "cover-image" in it.getAttribute("properties").split(Regex("\\s+")) } +
                images.filter { coverId != null && it.getAttribute("id") == coverId }
            for (item in preferred) {
                val path = resolvePath(packagePath, item.getAttribute("href")) ?: continue
                archive.bytes(path, MAX_COVER_BYTES)?.let { return@use it }
            }

            // Some EPUB 2 packages designate an XHTML/SVG cover page, rather than its image.
            val coverPages = document.elements("reference").filter {
                it.getAttribute("type").equals("cover", ignoreCase = true)
            }.map { it.getAttribute("href") } + items.filter {
                it.getAttribute("media-type") in listOf("application/xhtml+xml", "image/svg+xml") &&
                    (it.getAttribute("id") == coverId || it.getAttribute("href").contains("cover", ignoreCase = true))
            }.map { it.getAttribute("href") }
            for (href in coverPages) {
                val path = resolvePath(packagePath, href) ?: continue
                val coverPage = archive.xml(path) ?: continue
                val sources = coverPage.elements("img").map { it.getAttribute("src") } +
                    coverPage.elements("image").map {
                        it.getAttribute("href").ifBlank { it.getAttributeNS("http://www.w3.org/1999/xlink", "href") }
                    }
                for (source in sources) {
                    val imagePath = resolvePath(path, source) ?: continue
                    archive.bytes(imagePath, MAX_COVER_BYTES)?.let { return@use it }
                }
            }
            for (item in images.filter {
                it.getAttribute("id").contains("cover", ignoreCase = true) ||
                    it.getAttribute("href").contains("cover", ignoreCase = true)
            }) {
                val path = resolvePath(packagePath, item.getAttribute("href")) ?: continue
                archive.bytes(path, MAX_COVER_BYTES)?.let { return@use it }
            }
            null
        }
    }.getOrNull()

    private fun ZipFile.bytes(path: String, limit: Long): ByteArray? {
        val entry = getEntry(path)?.takeIf { !it.isDirectory && it.size <= limit } ?: return null
        return getInputStream(entry).use { input ->
            input.takeIf { entry.size >= 0 }?.readBytesBounded(limit.toInt())
        }
    }

    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray? {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = read(buffer)
        while (count != -1) {
            if (output.size() + count > limit) return null
            output.write(buffer, 0, count)
            count = read(buffer)
        }
        return output.toByteArray()
    }

    private fun ZipFile.xml(path: String): Document? = runCatching {
        val bytes = bytes(path, 1024 * 1024) ?: return null
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            // Android and the JVM support different parser flags; the resolver below also blocks external reads.
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }.parse(ByteArrayInputStream(bytes))
    }.getOrNull()

    private fun Document.elements(name: String): List<Element> = getElementsByTagNameNS("*", name).let { nodes ->
        (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
    }

    private fun resolvePath(parent: String, href: String): String? = runCatching {
        val uri = URI(href.replace(" ", "%20"))
        if (uri.isAbsolute || uri.authority != null || uri.path.isNullOrBlank()) return null
        val path = if (uri.path.startsWith('/')) uri.path else parent.substringBeforeLast('/', "") + "/" + uri.path
        val parts = mutableListOf<String>()
        for (part in path.split('/')) when (part) {
            "", "." -> Unit
            ".." -> if (parts.isEmpty()) return null else parts.removeAt(parts.lastIndex)
            else -> parts.add(part)
        }
        parts.joinToString("/")
    }.getOrNull()
}

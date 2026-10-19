package app.booklore.android.core.cache

import java.io.File
import java.security.MessageDigest

/** Covers belong to the local library, so Android must not evict them with temporary caches. */
internal class BookCoverFiles(filesDirectory: File, private val cacheDirectory: File) {
    val directory = File(filesDirectory, "book-covers")

    fun destination(baseUrl: String?, bookId: Long): File {
        val scope = baseUrl?.let { url ->
            MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
                .take(16).joinToString("") { "%02x".format(it) }
        } ?: "local"
        return File(directory, "$scope-$bookId.cover")
    }

    fun find(baseUrl: String?, bookId: Long): File? {
        val destination = destination(baseUrl, bookId)
        if (destination.isCoverImage()) return destination
        val legacy = baseUrl?.let { File(cacheDirectory, "book-covers/${it.hashCode()}-$bookId.cover") }
            ?.takeIf { it.isCoverImage() } ?: return null
        val temporary = File(directory, "${destination.name}.part")
        directory.mkdirs()
        return try {
            legacy.copyTo(temporary, overwrite = true)
            commit(temporary, destination)
        } finally {
            temporary.delete()
        }
    }

    fun write(baseUrl: String?, bookId: Long, bytes: ByteArray): File? {
        val destination = destination(baseUrl, bookId)
        directory.mkdirs()
        val temporary = File(directory, "${destination.name}.part")
        return try {
            temporary.writeBytes(bytes)
            commit(temporary, destination)
        } finally {
            temporary.delete()
        }
    }

    private fun commit(temporary: File, destination: File): File? {
        if (!temporary.isCoverImage()) return null
        if (!temporary.renameTo(destination)) temporary.copyTo(destination, overwrite = true)
        return destination
    }
}

/** Reject HTML/error bodies and partial files before giving the file to the image loader. */
internal fun File.isCoverImage(): Boolean = isFile && length() in 12L..MAX_COVER_BYTES && runCatching {
    val header = ByteArray(12)
    inputStream().use { it.read(header) }
    val ascii = header.toString(Charsets.ISO_8859_1)
    (header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) ||
        header.copyOfRange(0, 8).contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) ||
        ascii.startsWith("GIF87a") || ascii.startsWith("GIF89a") ||
        (ascii.startsWith("RIFF") && ascii.substring(8, 12) == "WEBP") || ascii.startsWith("BM")
}.getOrDefault(false)

internal const val MAX_COVER_BYTES = 16L * 1024 * 1024

package app.booklore.android.core.cache

import java.io.File
import java.util.Base64
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookCoverFilesTest {
    @get:Rule val temporary = TemporaryFolder()
    private val server = "https://library.example"

    @Test
    fun `legacy covers are migrated and survive Android cache removal`() {
        val files = temporary.newFolder("files")
        val cache = temporary.newFolder("cache")
        val legacy = File(cache, "book-covers/${server.hashCode()}-4.cover")
        legacy.parentFile!!.mkdirs()
        legacy.writeBytes(png)

        val saved = BookCoverFiles(files, cache).find(server, 4)
        assertNotNull(saved)
        cache.deleteRecursively()

        // Recreate the store, as happens after a process restart in local mode.
        assertArrayEquals(png, BookCoverFiles(files, cache).find(server, 4)!!.readBytes())
    }

    @Test
    fun `persisted covers do not depend on a temporary image cache`() {
        val files = temporary.newFolder("files")
        val cache = temporary.newFolder("cache")
        BookCoverFiles(files, cache).write(server, 7, png)
        cache.deleteRecursively()

        assertArrayEquals(png, BookCoverFiles(files, cache).find(server, 7)!!.readBytes())
    }

    @Test
    fun `covers with the same book ID stay separate across servers`() {
        val store = store()
        store.write(server, 7, png)

        assertNotEquals(store.destination(server, 7), store.destination("https://other.example", 7))
        assertNull(store.find("https://other.example", 7))
    }

    @Test
    fun `HTML responses and incomplete image files cannot replace valid covers`() {
        val store = store()
        val saved = store.write(server, 7, png)!!

        assertNull(store.write(server, 7, "<html>Authentication required</html>".toByteArray()))
        assertArrayEquals(png, saved.readBytes())
        assertFalse(File(store.directory, "${saved.name}.part").exists())
    }

    private fun store() = BookCoverFiles(temporary.newFolder("files"), temporary.newFolder("cache"))

    private val png = Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=",
    )
}

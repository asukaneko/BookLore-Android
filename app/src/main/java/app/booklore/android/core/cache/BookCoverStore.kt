package app.booklore.android.core.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.offline.LocalBookFiles
import app.booklore.android.core.security.SessionStore
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class BookCoverStore(context: Context, private val sessionStore: SessionStore = SessionStore(context)) {
    private val files = BookCoverFiles(context.applicationContext.filesDir, context.applicationContext.cacheDir)
    private val localBooks = LocalBookFiles(context.applicationContext.filesDir)

    suspend fun resolve(bookId: Long): File? = withContext(coverDispatcher) {
        val baseUrl = sessionStore.readCatalogBaseUrl()
        val destination = files.destination(baseUrl, bookId)
        locks[(destination.path.hashCode() and Int.MAX_VALUE) % locks.size].withLock {
            runCatching { files.find(baseUrl, bookId) }.getOrNull()?.let { return@withLock it }
            if (!sessionStore.readLocalMode() && sessionStore.read() != null) {
                try {
                    BookLoreApi(sessionStore).downloadCover(bookId, destination)
                    if (destination.isCoverImage()) return@withLock destination
                    destination.delete()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Previously downloaded books can supply a cover without reaching the server.
                }
            }
            val bookFile = localBooks.find(bookId) ?: return@withLock null
            val bytes = when (bookFile.extension.lowercase()) {
                "epub" -> EpubCoverExtractor.extract(bookFile)
                "pdf" -> renderPdfCover(bookFile)
                else -> null
            } ?: return@withLock null
            runCatching { files.write(baseUrl, bookId, bytes) }.getOrNull()
        }
    }

    private fun renderPdfCover(file: File): ByteArray? = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use rendererUse@ { renderer ->
                if (renderer.pageCount == 0) return@rendererUse null
                renderer.openPage(0).use { page ->
                    val scale = minOf(480f / page.width, 800f / page.height)
                    val bitmap = Bitmap.createBitmap((page.width * scale).roundToInt().coerceAtLeast(1),
                        (page.height * scale).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        ByteArrayOutputStream().use { output ->
                            if (bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) output.toByteArray() else null
                        }
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }.getOrNull()

    private companion object {
        // Embedded covers can be large; bound concurrent extraction and PDF bitmap allocation.
        val coverDispatcher = Dispatchers.IO.limitedParallelism(2)
        val locks = Array(32) { Mutex() }
    }
}

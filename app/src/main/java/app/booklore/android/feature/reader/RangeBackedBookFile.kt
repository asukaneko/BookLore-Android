package app.booklore.android.feature.reader

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import app.booklore.android.core.network.BookLoreApi
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.min
import kotlinx.coroutines.runBlocking

/**
 * Gives seek-only Android readers a file descriptor backed by HTTP range requests. The temporary
 * sparse file exists only for the active reader; the normal reader cache is filled separately.
 */
internal class RangeBackedBookFile(
    context: Context,
    private val api: BookLoreApi,
    private val bookId: Long,
    private val bookType: String,
    private val streamFile: File,
) {
    private val storageManager = context.getSystemService(StorageManager::class.java)

    suspend fun open(): ParcelFileDescriptor {
        val contentLength = api.bookContentLength(bookId, bookType)
        require(contentLength > 0) { "Book content is empty" }
        streamFile.parentFile?.mkdirs()
        streamFile.delete()
        // Proxy callbacks perform blocking HTTP range reads. They must not use
        // the main looper or a new PDF block will stall every UI frame.
        val callbackThread = HandlerThread("BookLorePdfRange").apply { start() }
        val callback = Callback(contentLength, callbackThread)
        return try {
            storageManager.openProxyFileDescriptor(
                ParcelFileDescriptor.MODE_READ_ONLY,
                callback,
                Handler(callbackThread.looper),
            )
        } catch (error: Exception) {
            callback.release()
            throw error
        }
    }

    private inner class Callback(
        private val contentLength: Long,
        private val callbackThread: HandlerThread,
    ) : ProxyFileDescriptorCallback() {
        private val file = RandomAccessFile(streamFile, "rw")
        private val loadedBlocks = mutableSetOf<Long>()
        private val releaseLock = Any()
        private var released = false

        override fun onGetSize(): Long = contentLength

        override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
            if (offset >= contentLength || size <= 0) return 0
            val bytesToRead = min(size.toLong(), contentLength - offset).toInt()
            return try {
                loadMissingBlocks(offset, bytesToRead)
                synchronized(file) {
                    file.seek(offset)
                    file.readFully(data, 0, bytesToRead)
                }
                bytesToRead
            } catch (error: Exception) {
                throw ErrnoException("book content read", OsConstants.EIO).also { it.initCause(error) }
            }
        }

        override fun onRelease() {
            release()
        }

        fun release() {
            synchronized(releaseLock) {
                if (released) return
                released = true
                synchronized(file) {
                    runCatching { file.close() }
                }
                streamFile.delete()
                callbackThread.quitSafely()
            }
        }

        @Synchronized
        private fun loadMissingBlocks(offset: Long, size: Int) {
            val endExclusive = offset + size
            var blockStart = offset / STREAM_BLOCK_BYTES * STREAM_BLOCK_BYTES
            while (blockStart < endExclusive) {
                if (loadedBlocks.add(blockStart)) {
                    try {
                        val blockEnd = min(blockStart + STREAM_BLOCK_BYTES - 1, contentLength - 1)
                        val expectedLength = (blockEnd - blockStart + 1).toInt()
                        val response = runBlocking {
                            api.readBookRange(bookId, bookType, blockStart..blockEnd)
                        }
                        val bytes = response.forRequestedRange(blockStart, expectedLength)
                        synchronized(file) {
                            file.seek(blockStart)
                            file.write(bytes)
                        }
                    } catch (error: Exception) {
                        loadedBlocks.remove(blockStart)
                        throw error
                    }
                }
                blockStart += STREAM_BLOCK_BYTES
            }
        }
    }

    private fun ByteArray.forRequestedRange(offset: Long, expectedLength: Int): ByteArray = when {
        size == expectedLength -> this
        offset <= Int.MAX_VALUE && size.toLong() >= offset + expectedLength ->
            copyOfRange(offset.toInt(), offset.toInt() + expectedLength)
        else -> throw IOException("Server returned an incomplete book content range")
    }

    private companion object {
        const val STREAM_BLOCK_BYTES = 256L * 1024L
    }
}

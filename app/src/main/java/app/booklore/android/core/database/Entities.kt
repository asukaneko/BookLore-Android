package app.booklore.android.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Cached library row for offline catalog browsing. */
@Entity(tableName = "libraries")
data class LibraryEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String?,
    val iconType: String?,
)

/** Cached shelf row for offline catalog browsing. */
@Entity(tableName = "shelves")
data class ShelfEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val icon: String?,
    val iconType: String?,
    val bookCount: Int,
    val publicShelf: Boolean,
)

/**
 * Cached book summary row for offline catalog browsing. Nested server objects
 * (metadata, file, progress, shelves) are stored as JSON blobs because they are
 * only read and written together with the whole row.
 */
@Entity(tableName = "books", indices = [Index("libraryId")])
data class BookEntity(
    @PrimaryKey val id: Long,
    val libraryId: Long?,
    val libraryName: String?,
    val title: String?,
    val metadataJson: String?,
    val primaryFileJson: String?,
    val lastReadTime: String?,
    val addedOn: String?,
    val readStatus: String?,
    val personalRating: Int?,
    val shelvesJson: String?,
    val epubProgressJson: String?,
    val pdfProgressJson: String?,
    val isPhysical: Boolean?,
)

/**
 * A mutation that could not reach the server yet and is retried in the
 * background. `dedupeKey` is unique per logical entry: progress updates reuse
 * the book/file key so a newer position replaces the queued older one, while
 * each reading session has its own key.
 */
@Entity(tableName = "pending_sync", indices = [Index(value = ["dedupeKey"], unique = true)])
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val dedupeKey: String,
    val payloadJson: String,
    val createdAtMs: Long,
)

/** Cached annotation row so highlights survive offline reopening of a book. */
@Entity(tableName = "annotations", indices = [Index("bookId")])
data class AnnotationEntity(
    @PrimaryKey val id: Long,
    val bookId: Long,
    val cfi: String?,
    val text: String?,
    val color: String?,
    val style: String?,
    val note: String?,
    val chapterTitle: String?,
    val updatedAt: String?,
)

/** Durable lifecycle state for a user-requested offline book download. */
@Entity(
    tableName = "download_tasks",
    indices = [Index(value = ["bookId", "bookFileId"], unique = true)],
)
data class DownloadTaskEntity(
    @PrimaryKey val taskKey: String,
    val bookId: Long,
    val bookFileId: Long,
    val bookType: String,
    val bookTitle: String,
    val status: String,
    val bytesRead: Long = 0L,
    val totalBytes: Long? = null,
    val errorMessage: String? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

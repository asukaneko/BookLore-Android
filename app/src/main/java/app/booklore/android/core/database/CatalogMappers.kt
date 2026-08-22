package app.booklore.android.core.database

import app.booklore.android.core.model.Book
import app.booklore.android.core.model.BookFile
import app.booklore.android.core.model.BookMetadata
import app.booklore.android.core.model.EpubProgress
import app.booklore.android.core.model.Library
import app.booklore.android.core.model.PdfProgress
import app.booklore.android.core.model.Shelf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Maps cached rows to domain models and back. Nested objects round-trip as JSON. */
object CatalogMappers {
    private val json = Json { ignoreUnknownKeys = true }

    fun Library.toEntity() = LibraryEntity(
        id = id,
        name = name,
        icon = icon,
        iconType = iconType,
    )

    fun Shelf.toEntity() = ShelfEntity(
        id = id,
        name = name,
        icon = icon,
        iconType = iconType,
        bookCount = bookCount,
        publicShelf = publicShelf,
    )

    fun Book.toEntity() = BookEntity(
        id = id,
        libraryId = libraryId,
        libraryName = libraryName,
        title = title,
        metadataJson = metadata.encode(BookMetadata.serializer()),
        primaryFileJson = primaryFile.encode(BookFile.serializer()),
        lastReadTime = lastReadTime,
        addedOn = addedOn,
        readStatus = readStatus,
        personalRating = personalRating,
        shelvesJson = shelves.toList().encode(ListSerializer(Shelf.serializer())),
        epubProgressJson = epubProgress.encode(EpubProgress.serializer()),
        pdfProgressJson = pdfProgress.encode(PdfProgress.serializer()),
        isPhysical = isPhysical,
    )

    fun LibraryEntity.toModel() = Library(
        id = id,
        name = name,
        icon = icon,
        iconType = iconType,
    )

    fun ShelfEntity.toModel() = Shelf(
        id = id,
        name = name,
        icon = icon,
        iconType = iconType,
        bookCount = bookCount,
        publicShelf = publicShelf,
    )

    fun BookEntity.toModel() = Book(
        id = id,
        libraryId = libraryId,
        libraryName = libraryName,
        title = title,
        metadata = metadataJson.decode(BookMetadata.serializer()),
        primaryFile = primaryFileJson.decode(BookFile.serializer()),
        lastReadTime = lastReadTime,
        addedOn = addedOn,
        readStatus = readStatus,
        personalRating = personalRating,
        shelves = shelvesJson.decode(ListSerializer(Shelf.serializer()))?.toSet() ?: emptySet(),
        epubProgress = epubProgressJson.decode(EpubProgress.serializer()),
        pdfProgress = pdfProgressJson.decode(PdfProgress.serializer()),
        isPhysical = isPhysical,
    )

    private fun <T> T?.encode(serializer: KSerializer<T>): String? = when (this) {
        null -> null
        else -> json.encodeToString(serializer, this)
    }

    private fun <T> String?.decode(serializer: KSerializer<T>): T? = when (this) {
        null -> null
        else -> runCatching { json.decodeFromString(serializer, this) }.getOrNull()
    }
}

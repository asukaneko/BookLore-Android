package app.booklore.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CatalogDao {
    @Query("SELECT * FROM libraries ORDER BY name")
    suspend fun libraries(): List<LibraryEntity>

    @Query("SELECT * FROM shelves ORDER BY name")
    suspend fun shelves(): List<ShelfEntity>

    @Query("SELECT * FROM books")
    suspend fun books(): List<BookEntity>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun bookCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraries(libraries: List<LibraryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelves(shelves: List<ShelfEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("DELETE FROM libraries")
    suspend fun clearLibraries()

    @Query("DELETE FROM shelves")
    suspend fun clearShelves()

    @Query("DELETE FROM books")
    suspend fun clearBooks()

    /** Replaces the whole cached catalog in one transaction. */
    @Transaction
    suspend fun replaceCatalog(libraries: List<LibraryEntity>, shelves: List<ShelfEntity>, books: List<BookEntity>) {
        clearLibraries()
        clearShelves()
        clearBooks()
        insertLibraries(libraries)
        insertShelves(shelves)
        insertBooks(books)
    }

    @Transaction
    suspend fun clearCatalog() {
        clearLibraries()
        clearShelves()
        clearBooks()
    }
}

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAtMs")
    suspend fun all(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun count(): Int

    @Query("SELECT * FROM pending_sync WHERE type = :type ORDER BY createdAtMs")
    suspend fun byType(type: String): List<PendingSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PendingSyncEntity)

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_sync WHERE dedupeKey = :dedupeKey")
    suspend fun deleteByDedupeKey(dedupeKey: String)

    @Query("DELETE FROM pending_sync")
    suspend fun clear()
}

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId")
    suspend fun forBook(bookId: Long): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(annotations: List<AnnotationEntity>)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM annotations WHERE bookId = :bookId")
    suspend fun clearBook(bookId: Long)

    @Query("DELETE FROM annotations")
    suspend fun clear()

    @Transaction
    suspend fun replaceBook(bookId: Long, annotations: List<AnnotationEntity>) {
        clearBook(bookId)
        insertAll(annotations)
    }
}

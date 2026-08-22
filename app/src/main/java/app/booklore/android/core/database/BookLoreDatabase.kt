package app.booklore.android.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LibraryEntity::class,
        ShelfEntity::class,
        BookEntity::class,
        PendingSyncEntity::class,
        AnnotationEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class BookLoreDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun annotationDao(): AnnotationDao

    companion object {
        @Volatile
        private var instance: BookLoreDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `annotations` (" +
                        "`id` INTEGER NOT NULL, `bookId` INTEGER NOT NULL, `cfi` TEXT, " +
                        "`text` TEXT, `color` TEXT, `style` TEXT, `note` TEXT, " +
                        "`chapterTitle` TEXT, `updatedAt` TEXT, PRIMARY KEY(`id`))",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_annotations_bookId` ON `annotations` (`bookId`)")
            }
        }

        fun get(context: Context): BookLoreDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookLoreDatabase::class.java,
                    "booklore.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

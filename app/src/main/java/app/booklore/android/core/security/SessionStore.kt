package app.booklore.android.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import app.booklore.android.core.model.DisplaySettings
import app.booklore.android.core.model.EpubReaderSettings
import app.booklore.android.core.model.EpubReaderTheme
import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.PdfPosition
import app.booklore.android.core.model.PdfReaderSettings
import app.booklore.android.core.model.PdfReadingMode
import app.booklore.android.core.model.FontOption
import app.booklore.android.core.model.SavedLogin
import app.booklore.android.core.model.Session
import app.booklore.android.core.model.ThemeColorOption
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class SessionStore(context: Context) {
    private val preferences = obtainPreferences(context.applicationContext)
    private val savedLoginJson = Json { ignoreUnknownKeys = true }

    fun read(): Session? = synchronized(Companion) {
        if (!sessionLoaded) {
            val baseUrl = preferences.getString(KEY_BASE_URL, null)
            val accessToken = preferences.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null)
            sessionCache = if (baseUrl != null && accessToken != null && refreshToken != null) {
                Session(baseUrl, accessToken, refreshToken)
            } else {
                null
            }
            sessionLoaded = true
        }
        sessionCache
    }

    fun write(session: Session) = synchronized(Companion) {
        preferences.edit()
            .putString(KEY_BASE_URL, session.baseUrl)
            .putString(KEY_CATALOG_BASE_URL, session.baseUrl)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .apply()
        sessionCache = session
        sessionLoaded = true
        Unit
    }

    fun clear(preserveReadingPositions: Boolean = false) = synchronized(Companion) {
        val editor = preferences.edit()
            .remove(KEY_BASE_URL)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
        if (preserveReadingPositions) {
            preferences.getString(KEY_BASE_URL, null)?.let { editor.putString(KEY_CATALOG_BASE_URL, it) }
        } else {
            preferences.all.keys
                .filter {
                    it.startsWith(KEY_EPUB_POSITION_PREFIX) ||
                        it.startsWith(KEY_PDF_POSITION_PREFIX)
                }
                .forEach(editor::remove)
        }
        editor.apply()
        sessionCache = null
        sessionLoaded = true
        Unit
    }

    fun readLocalMode(): Boolean = preferences.getBoolean(KEY_LOCAL_MODE, false)

    fun writeLocalMode(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LOCAL_MODE, enabled).apply()
    }

    fun readCatalogBaseUrl(): String? = preferences.getString(KEY_CATALOG_BASE_URL, null)
        ?: read()?.baseUrl

    @Synchronized
    fun readSavedLogins(): List<SavedLogin> {
        val raw = preferences.getString(KEY_SAVED_LOGINS, null) ?: return emptyList()
        return runCatching {
            savedLoginJson.decodeFromString(ListSerializer(SavedLogin.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveLogin(login: SavedLogin) {
        val updated = readSavedLogins()
            .filterNot { it.baseUrl == login.baseUrl && it.username == login.username } + login
        writeSavedLogins(updated)
    }

    @Synchronized
    fun removeLogin(baseUrl: String, username: String) {
        writeSavedLogins(readSavedLogins().filterNot { it.baseUrl == baseUrl && it.username == username })
    }

    private fun writeSavedLogins(logins: List<SavedLogin>) {
        val encoded = savedLoginJson.encodeToString(ListSerializer(SavedLogin.serializer()), logins)
        preferences.edit().putString(KEY_SAVED_LOGINS, encoded).apply()
    }

    /** Timestamp of the last successful background flush (0 = never). */
    @Synchronized
    fun readLastSyncTimeMs(): Long = preferences.getLong(KEY_LAST_SYNC_TIME_MS, 0L)

    @Synchronized
    fun writeLastSyncTimeMs(timeMs: Long) {
        preferences.edit().putLong(KEY_LAST_SYNC_TIME_MS, timeMs).apply()
    }

    @Synchronized
    fun readCompactBookGrid(): Boolean = preferences.getBoolean(KEY_COMPACT_BOOK_GRID, false)

    @Synchronized
    fun writeCompactBookGrid(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_COMPACT_BOOK_GRID, enabled).apply()
    }

    /** Library tab sort selection; the option is stored as its enum name. */
    @Synchronized
    fun readLibrarySortSettings(): Pair<String?, Boolean> = Pair(
        preferences.getString(KEY_LIBRARY_SORT_OPTION, null),
        preferences.getBoolean(KEY_LIBRARY_SORT_ASCENDING, true),
    )

    @Synchronized
    fun writeLibrarySortSettings(option: String, ascending: Boolean) {
        preferences.edit()
            .putString(KEY_LIBRARY_SORT_OPTION, option)
            .putBoolean(KEY_LIBRARY_SORT_ASCENDING, ascending)
            .apply()
    }

    @Synchronized
    fun readDisplaySettings(): DisplaySettings = DisplaySettings(
        themeColor = preferences.getString(KEY_THEME_COLOR, null)
            ?.let { value -> ThemeColorOption.entries.firstOrNull { it.name == value } }
            ?: ThemeColorOption.FOREST,
        font = preferences.getString(KEY_FONT, null)
            ?.let { value -> FontOption.entries.firstOrNull { it.name == value } }
            ?: FontOption.SANS,
        fontScale = preferences.getFloat(KEY_FONT_SCALE, DEFAULT_FONT_SCALE)
            .coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
    )

    @Synchronized
    fun writeDisplaySettings(settings: DisplaySettings) {
        preferences.edit()
            .putString(KEY_THEME_COLOR, settings.themeColor.name)
            .putString(KEY_FONT, settings.font.name)
            .putFloat(KEY_FONT_SCALE, settings.fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE))
            .apply()
    }

    @Synchronized
    fun readEpubReaderSettings(): EpubReaderSettings = EpubReaderSettings(
        theme = preferences.getString(KEY_EPUB_READER_THEME, null)
            ?.let { value -> EpubReaderTheme.entries.firstOrNull { it.name == value } }
            ?: EpubReaderTheme.SYSTEM,
        fontScale = preferences.getFloat(KEY_EPUB_READER_FONT_SCALE, DEFAULT_EPUB_READER_FONT_SCALE)
            .coerceIn(MIN_EPUB_READER_FONT_SCALE, MAX_EPUB_READER_FONT_SCALE),
        font = preferences.getString(KEY_EPUB_READER_FONT, null)
            ?.let { value -> FontOption.entries.firstOrNull { it.name == value } }
            ?: FontOption.SANS,
        pageMargins = preferences.getFloat(KEY_EPUB_READER_PAGE_MARGINS, DEFAULT_EPUB_READER_PAGE_MARGINS)
            .coerceIn(MIN_EPUB_READER_PAGE_MARGINS, MAX_EPUB_READER_PAGE_MARGINS),
    )

    @Synchronized
    fun writeEpubReaderSettings(settings: EpubReaderSettings) {
        preferences.edit()
            .putString(KEY_EPUB_READER_THEME, settings.theme.name)
            .putFloat(
                KEY_EPUB_READER_FONT_SCALE,
                settings.fontScale.coerceIn(MIN_EPUB_READER_FONT_SCALE, MAX_EPUB_READER_FONT_SCALE),
            )
            .putString(KEY_EPUB_READER_FONT, settings.font.name)
            .putFloat(
                KEY_EPUB_READER_PAGE_MARGINS,
                settings.pageMargins.coerceIn(MIN_EPUB_READER_PAGE_MARGINS, MAX_EPUB_READER_PAGE_MARGINS),
            )
            .apply()
    }

    @Synchronized
    fun readPdfReaderSettings(): PdfReaderSettings = PdfReaderSettings(
        readingMode = preferences.getString(KEY_PDF_READER_MODE, null)
            ?.let { value -> PdfReadingMode.entries.firstOrNull { it.name == value } }
            ?: PdfReadingMode.PAGINATED,
    )

    @Synchronized
    fun writePdfReaderSettings(settings: PdfReaderSettings) {
        preferences.edit().putString(KEY_PDF_READER_MODE, settings.readingMode.name).apply()
    }

    @Synchronized
    fun readEpubPosition(bookId: Long, bookFileId: Long): EpubPosition? {
        val key = epubPositionKey(bookId, bookFileId)
        val cfi = preferences.getString("${key}_cfi", null)
        val href = preferences.getString("${key}_href", null)
        if (cfi.isNullOrBlank() && href.isNullOrBlank()) return null
        return EpubPosition(
            bookId = bookId,
            bookFileId = bookFileId,
            cfi = cfi,
            href = href,
            progression = preferences.getFloat("${key}_progression", -1f)
                .takeIf { it in 0f..1f },
            progressPercent = preferences.getFloat("${key}_progress", 0f).coerceIn(0f, 100f),
        )
    }

    @Synchronized
    fun writeEpubPosition(position: EpubPosition) {
        val key = epubPositionKey(position.bookId, position.bookFileId)
        preferences.edit()
            .putString("${key}_cfi", position.cfi)
            .putString("${key}_href", position.href)
            .putFloat("${key}_progression", position.progression?.coerceIn(0f, 1f) ?: -1f)
            .putFloat("${key}_progress", position.progressPercent.coerceIn(0f, 100f))
            .apply()
    }

    @Synchronized
    fun readPdfPosition(bookId: Long, bookFileId: Long): PdfPosition? {
        val key = pdfPositionKey(bookId, bookFileId)
        if (!preferences.contains("${key}_page")) return null
        return PdfPosition(
            bookId = bookId,
            bookFileId = bookFileId,
            page = preferences.getInt("${key}_page", 1).coerceAtLeast(1),
            progressPercent = preferences.getFloat("${key}_progress", 0f).coerceIn(0f, 100f),
        )
    }

    @Synchronized
    fun writePdfPosition(position: PdfPosition) {
        val key = pdfPositionKey(position.bookId, position.bookFileId)
        preferences.edit()
            .putInt("${key}_page", position.page.coerceAtLeast(1))
            .putFloat("${key}_progress", position.progressPercent.coerceIn(0f, 100f))
            .apply()
    }

    private fun epubPositionKey(bookId: Long, bookFileId: Long): String =
        "$KEY_EPUB_POSITION_PREFIX${bookId}_${bookFileId}"

    private fun pdfPositionKey(bookId: Long, bookFileId: Long): String =
        "$KEY_PDF_POSITION_PREFIX${bookId}_${bookFileId}"

    private companion object {
        // Creating EncryptedSharedPreferences is expensive (Keystore key derivation),
        // so every instance shares one underlying store and an in-memory session cache.
        // This keeps per-cover lookups off the encrypted storage during composition.
        @Volatile
        private var preferencesSingleton: SharedPreferences? = null

        @Volatile
        private var sessionCache: Session? = null

        @Volatile
        private var sessionLoaded = false

        private fun obtainPreferences(appContext: Context): SharedPreferences =
            preferencesSingleton ?: synchronized(this) {
                preferencesSingleton ?: EncryptedSharedPreferences.create(
                    appContext,
                    "booklore_session",
                    MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                ).also { preferencesSingleton = it }
            }

        const val KEY_BASE_URL = "base_url"
        const val KEY_CATALOG_BASE_URL = "catalog_base_url"
        const val KEY_LOCAL_MODE = "local_mode"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_SAVED_LOGINS = "saved_logins"
        const val KEY_COMPACT_BOOK_GRID = "compact_book_grid"
        const val KEY_LAST_SYNC_TIME_MS = "last_sync_time_ms"
        const val KEY_LIBRARY_SORT_OPTION = "library_sort_option"
        const val KEY_LIBRARY_SORT_ASCENDING = "library_sort_ascending"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_FONT = "font"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_EPUB_READER_THEME = "epub_reader_theme"
        const val KEY_EPUB_READER_FONT_SCALE = "epub_reader_font_scale"
        const val KEY_EPUB_READER_FONT = "epub_reader_font"
        const val KEY_EPUB_READER_PAGE_MARGINS = "epub_reader_page_margins"
        const val KEY_PDF_READER_MODE = "pdf_reader_mode"
        const val KEY_EPUB_POSITION_PREFIX = "epub_position_"
        const val KEY_PDF_POSITION_PREFIX = "pdf_position_"
        const val DEFAULT_FONT_SCALE = 1f
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.30f
        const val DEFAULT_EPUB_READER_FONT_SCALE = 1f
        const val MIN_EPUB_READER_FONT_SCALE = 0.80f
        const val MAX_EPUB_READER_FONT_SCALE = 1.40f
        const val DEFAULT_EPUB_READER_PAGE_MARGINS = 1f
        const val MIN_EPUB_READER_PAGE_MARGINS = 0f
        const val MAX_EPUB_READER_PAGE_MARGINS = 3f
    }
}

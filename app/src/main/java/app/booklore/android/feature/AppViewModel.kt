package app.booklore.android.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.booklore.android.core.cache.CacheManager
import app.booklore.android.core.cache.CacheSnapshot
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.database.CatalogMappers.toEntity
import app.booklore.android.core.database.CatalogMappers.toModel
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.DisplaySettings
import app.booklore.android.core.model.EpubProgress
import app.booklore.android.core.model.PdfProgress
import app.booklore.android.core.model.FontOption
import app.booklore.android.core.model.Library
import app.booklore.android.core.model.ReadingStatistics
import app.booklore.android.core.model.SavedLogin
import app.booklore.android.core.model.ServerConnection
import app.booklore.android.core.model.Shelf
import app.booklore.android.core.model.ThemeColorOption
import app.booklore.android.core.model.supportsEpubReader
import app.booklore.android.core.model.supportsPdfReader
import app.booklore.android.core.network.ApiException
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.network.NetworkException
import app.booklore.android.core.offline.OfflineBookStore
import app.booklore.android.core.offline.OfflineDownloadQueue
import app.booklore.android.core.offline.OfflineDownloadTask
import app.booklore.android.core.offline.toModel
import app.booklore.android.core.security.SessionStore
import app.booklore.android.sync.PendingSyncQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Year

class AppViewModel(
    private val api: BookLoreApi,
    private val sessionStore: SessionStore,
    private val database: BookLoreDatabase,
    private val pendingSyncQueue: PendingSyncQueue,
    private val cacheManager: CacheManager,
    private val offlineBookStore: OfflineBookStore,
    private val offlineDownloadQueue: OfflineDownloadQueue,
    private val isNetworkAvailable: () -> Boolean = { true },
) : ViewModel() {
    private val _state = MutableStateFlow(newUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    private var catalogJob: Job? = null

    init {
        viewModelScope.launch {
            offlineDownloadQueue.observeTasks().collect { entities ->
                val tasks = entities.map { it.toModel() }
                val books = _state.value.books
                val downloaded = io { offlineBookStore.downloadedBookIds(books) }
                val local = io { offlineBookStore.localBookIds(books) }
                _state.update {
                    it.copy(
                        offlineDownloadTasks = tasks,
                        downloadedBookIds = if (it.books == books) downloaded else it.downloadedBookIds,
                        localBookIds = if (it.books == books) local else it.localBookIds,
                    )
                }
            }
        }
        if (sessionStore.readLocalMode() || (sessionStore.read() != null && !isNetworkAvailable())) {
            enterLocalMode()
        } else if (sessionStore.read() != null) {
            // Opportunistically flush any progress queued while offline.
            if (isNetworkAvailable()) pendingSyncQueue.schedule()
            refreshCatalog()
            refreshSyncStatus()
        }
    }

    fun testConnection(url: String, allowHttp: Boolean) = runRequest {
        sessionStore.writeLocalMode(false)
        _state.update { it.copy(isBusy = true, error = null, connection = null) }
        val connection = io { api.probe(url, allowHttp) }
        _state.update { it.copy(isBusy = false, connection = connection) }
    }

    fun login(username: String, password: String, rememberAccount: Boolean) {
        viewModelScope.launch {
            val connection = _state.value.connection ?: return@launch
            try {
                _state.update { it.copy(isBusy = true, error = null) }
                val trimmedUsername = username.trim()
                io { api.login(connection.baseUrl, trimmedUsername, password) }
                if (rememberAccount) {
                    sessionStore.saveLogin(SavedLogin(connection.baseUrl, trimmedUsername, password))
                } else {
                    sessionStore.removeLogin(connection.baseUrl, trimmedUsername)
                }
                _state.update {
                    it.copy(
                        isBusy = false,
                        authenticated = true,
                        savedLogins = sessionStore.readSavedLogins(),
                        loginPrefill = null,
                    )
                }
                pendingSyncQueue.schedule()
                loadCatalog()
            } catch (error: Exception) {
                val uiError = if (error is ApiException && error.statusCode == 401) {
                    UiError.InvalidCredentials
                } else {
                    error.toUiError()
                }
                _state.update { it.copy(isBusy = false, error = uiError) }
            }
        }
    }

    /** Signs in with a saved account directly; falls back to the login form with a notice when expired. */
    fun quickLogin(saved: SavedLogin) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isBusy = true, error = null, loginPrefill = null) }
                val allowHttp = saved.baseUrl.startsWith("http://")
                val connection = io { api.probe(saved.baseUrl, allowHttp) }
                try {
                    val password = saved.password ?: ""
                    io { api.login(saved.baseUrl, saved.username, password) }
                    _state.update { it.copy(isBusy = false, authenticated = true) }
                    pendingSyncQueue.schedule()
                    loadCatalog()
                } catch (error: Exception) {
                    if (error is ApiException && error.statusCode == 401) {
                        // Saved credentials no longer work: return to the login form and ask again.
                        _state.update {
                            it.copy(
                                isBusy = false,
                                connection = connection,
                                loginPrefill = saved,
                                error = UiError.SavedLoginExpired,
                            )
                        }
                    } else {
                        throw error
                    }
                }
            } catch (error: Exception) {
                _state.update { it.copy(isBusy = false, error = error.toUiError()) }
            }
        }
    }

    fun removeSavedLogin(saved: SavedLogin) {
        sessionStore.removeLogin(saved.baseUrl, saved.username)
        _state.update { it.copy(savedLogins = sessionStore.readSavedLogins()) }
    }

    fun refreshCatalog() {
        catalogJob?.cancel()
        catalogJob = runRequest {
            if (_state.value.localMode) {
                loadCachedCatalog()
            } else if (sessionStore.read() != null) {
                _state.update { it.copy(isBusy = true, authenticated = true, error = null) }
                // Display the saved catalog immediately while checking the server.
                loadCachedCatalog(localOnly = false)
                _state.update { it.copy(isBusy = true) }
                loadCatalog()
            }
        }
    }

    fun setLocalMode(enabled: Boolean) {
        if (enabled) {
            enterLocalMode()
        } else {
            catalogJob?.cancel()
            sessionStore.writeLocalMode(false)
            _state.update {
                it.copy(localMode = false, offline = false, bookDetail = null, error = null)
            }
            if (sessionStore.read() != null) {
                pendingSyncQueue.schedule(replaceExisting = true)
                runRequest { offlineDownloadQueue.resumePending() }
                refreshCatalog()
            } else {
                _state.value = newUiState()
            }
        }
    }

    fun enterLocalMode() {
        catalogJob?.cancel()
        sessionStore.writeLocalMode(true)
        _state.update {
            it.copy(localMode = true, offline = true, isBusy = true, bookDetail = null, error = null)
        }
        catalogJob = runRequest { loadCachedCatalog() }
    }

    /** Runs the server-side scan for the selected library, or every visible library when null. */
    fun rescanLibraries(selectedLibraryId: Long?) = runRequest {
        if (_state.value.localMode) return@runRequest
        val librariesToScan = _state.value.libraries.filter { library ->
            selectedLibraryId == null || library.id == selectedLibraryId
        }
        _state.update { it.copy(isBusy = true, authenticated = sessionStore.read() != null, error = null, offline = false) }
        librariesToScan.forEach { library -> io { api.rescanLibrary(library.id) } }
        loadCatalog()
        refreshOfflineBooks()
    }

    fun refreshStatistics(year: Int = Year.now().value) {
        if (_state.value.localMode) return
        viewModelScope.launch {
            _state.update { it.copy(isStatisticsLoading = true, statisticsError = null) }
            try {
                val statistics = io { api.readingStatistics(year) }
                _state.update {
                    it.copy(
                        isStatisticsLoading = false,
                        readingStatistics = statistics,
                    )
                }
            } catch (error: Exception) {
                val uiError = error.toUiError()
                if (uiError == UiError.Unauthorized && sessionStore.read() != null) {
                    sessionStore.clear(preserveReadingPositions = true)
                    _state.value = newUiState(error = UiError.Unauthorized)
                    return@launch
                }
                _state.update {
                    it.copy(
                        isStatisticsLoading = false,
                        statisticsError = uiError,
                    )
                }
            }
        }
    }

    fun selectBook(bookId: Long) = runRequest {
        if (!_state.value.localMode && !isNetworkAvailable()) {
            catalogJob?.cancel()
            sessionStore.writeLocalMode(true)
            loadCachedCatalog()
        }
        val cachedBook = _state.value.books.firstOrNull { it.id == bookId }
        if (_state.value.localMode) {
            _state.update {
                it.copy(
                    bookDetail = cachedBook,
                    isDetailLoading = false,
                    error = if (cachedBook == null) UiError.LocalBookUnavailable else null,
                )
            }
            return@runRequest
        }
        _state.update {
            it.copy(
                bookDetail = cachedBook,
                isDetailLoading = true,
                error = null,
            )
        }
        val book = try {
            io { api.book(bookId) }
        } catch (error: NetworkException) {
            // Offline: fall back to the cached book summary (no full description).
            cachedBook ?: throw error
        }
        if (_state.value.localMode) return@runRequest
        io { database.catalogDao().insertBooks(listOf(book.toEntity())) }
        _state.update { it.copy(bookDetail = book, isDetailLoading = false) }
    }

    fun clearBookSelection() {
        _state.update { it.copy(bookDetail = null, isDetailLoading = false) }
    }

    fun clearConnection() {
        _state.update { it.copy(connection = null, loginPrefill = null, error = null) }
    }

    fun updateReadStatus(bookId: Long, status: String) = runRequest {
        _state.update { it.copy(isActionRunning = true, error = null, notice = null) }
        io { api.updateReadStatus(bookId, status) }
        _state.update {
            it.copy(
                isActionRunning = false,
                notice = Notice.StatusUpdated,
                bookDetail = it.bookDetail?.takeIf { book -> book.id != bookId }?.let { book -> book }
                    ?: it.bookDetail?.copy(readStatus = status),
                books = it.books.map { book -> if (book.id == bookId) book.copy(readStatus = status) else book },
            )
        }
    }

    fun saveInitialProgress(book: Book) = runRequest {
        val fileId = book.primaryFile?.id ?: return@runRequest
        _state.update { it.copy(isActionRunning = true, error = null, notice = null) }
        io { api.updateProgress(book.id, fileId, 1f) }
        _state.update { it.copy(isActionRunning = false, notice = Notice.ProgressSaved) }
    }

    fun updatePersonalRating(bookId: Long, rating: Int) = runRequest {
        _state.update { it.copy(isActionRunning = true, error = null, notice = null) }
        io { api.updatePersonalRating(bookId, rating) }
        _state.update {
            it.copy(
                isActionRunning = false,
                notice = Notice.RatingUpdated,
                bookDetail = it.bookDetail?.let { book -> if (book.id == bookId) book.copy(personalRating = rating) else book },
                books = it.books.map { book -> if (book.id == bookId) book.copy(personalRating = rating) else book },
            )
        }
    }

    fun assignShelves(bookId: Long, shelvesToAssign: Set<Long>, shelvesToUnassign: Set<Long>) = runRequest {
        _state.update { it.copy(isActionRunning = true, error = null, notice = null) }
        io { api.assignShelves(bookId, shelvesToAssign, shelvesToUnassign) }

        // The detail endpoint is not needed to reflect a successful mutation. Apply the
        // requested membership to every in-memory/cache representation immediately.
        val snapshot = _state.value
        val currentBook = snapshot.bookDetail?.takeIf { it.id == bookId }
            ?: snapshot.books.firstOrNull { it.id == bookId }
        val updatedBook = currentBook?.withShelfChanges(snapshot.shelves, shelvesToAssign, shelvesToUnassign)
        val updatedShelves = snapshot.shelves.withBookShelfCounts(currentBook, updatedBook)
        if (updatedBook != null) {
            io {
                database.catalogDao().insertBooks(listOf(updatedBook.toEntity()))
                database.catalogDao().insertShelves(updatedShelves.map { it.toEntity() })
            }
        }
        _state.update {
            it.copy(
                isActionRunning = false,
                notice = Notice.ShelvesUpdated,
                bookDetail = it.bookDetail?.let { book ->
                    if (book.id == bookId) updatedBook ?: book else book
                },
                books = it.books.map { book -> if (book.id == bookId) updatedBook ?: book else book },
                shelves = updatedShelves,
            )
        }
    }

    /** Downloads a supported book into durable private storage for offline reading. */
    fun downloadBook(book: Book) {
        if (_state.value.localMode) return
        if (offlineBookStore.isDownloaded(book)) return
        if (book.primaryFile?.id == null || (!book.supportsEpubReader() && !book.supportsPdfReader())) return
        viewModelScope.launch {
            runCatching { offlineDownloadQueue.enqueue(book) }
                .onFailure { error -> _state.update { it.copy(error = (error as? Exception ?: Exception(error)).toUiError()) } }
        }
    }

    fun cancelDownload(book: Book) {
        val task = _state.value.offlineDownloadTasks.firstOrNull { it.bookId == book.id } ?: return
        cancelDownload(task)
    }

    fun cancelDownload(task: OfflineDownloadTask) {
        viewModelScope.launch {
            runCatching { offlineDownloadQueue.cancel(task) }
                .onFailure { error -> _state.update { it.copy(error = (error as? Exception ?: Exception(error)).toUiError()) } }
        }
    }

    fun retryDownload(book: Book) {
        val task = _state.value.offlineDownloadTasks.firstOrNull { it.bookId == book.id }
        if (task == null) downloadBook(book) else retryDownload(task)
    }

    fun retryDownload(task: OfflineDownloadTask) {
        if (_state.value.localMode) return
        viewModelScope.launch {
            runCatching { offlineDownloadQueue.retry(task) }
                .onFailure { error -> _state.update { it.copy(error = (error as? Exception ?: Exception(error)).toUiError()) } }
        }
    }

    fun deleteDownloadedBook(book: Book) {
        val task = _state.value.offlineDownloadTasks.firstOrNull { it.bookId == book.id }
        if (task != null) {
            deleteDownloadedTask(task)
            return
        }
        viewModelScope.launch {
            io { offlineBookStore.delete(book) }
            _state.update { it.copy(downloadedBookIds = it.downloadedBookIds - book.id, notice = Notice.DownloadDeleted) }
            refreshCatalogIfLocal()
        }
    }

    fun deleteDownloadedTask(task: OfflineDownloadTask) {
        viewModelScope.launch {
            io { offlineDownloadQueue.delete(task) }
            _state.update { it.copy(downloadedBookIds = it.downloadedBookIds - task.bookId, notice = Notice.DownloadDeleted) }
            refreshCatalogIfLocal()
        }
    }

    fun refreshOfflineBooks() = runRequest {
        val downloaded = io { offlineBookStore.downloadedBookIds(_state.value.books) }
        val local = io { offlineBookStore.localBookIds(_state.value.books) }
        _state.update { it.copy(downloadedBookIds = downloaded, localBookIds = local) }
    }

    private fun refreshCatalogIfLocal() {
        if (_state.value.localMode) refreshCatalog() else refreshOfflineBooks()
    }

    fun dismissMessage() {
        _state.update { it.copy(error = null, notice = null) }
    }

    /** Re-reads the offline queue size and last successful sync timestamp. */
    fun refreshSyncStatus() = runRequest {
        val status = io {
            SyncStatus(
                pendingCount = pendingSyncQueue.pendingCount(),
                lastSyncTimeMs = sessionStore.readLastSyncTimeMs().takeIf { it > 0 },
            )
        }
        _state.update { it.copy(syncStatus = status) }
    }

    /** Requests an immediate background flush of the offline queue. */
    fun syncNow() = runRequest {
        if (_state.value.localMode) return@runRequest
        pendingSyncQueue.schedule()
        // The flush runs in WorkManager; re-read the queue after it had a chance to drain.
        kotlinx.coroutines.delay(3_000)
        refreshSyncStatus()
    }

    fun refreshCache() {
        runRequest {
            _state.update { it.copy(isCacheLoading = true) }
            val snapshot = io { cacheManager.snapshot() }
            _state.update { it.copy(cacheSnapshot = snapshot, isCacheLoading = false) }
        }
    }

    fun clearCache() {
        runRequest {
            _state.update { it.copy(isCacheLoading = true, error = null, notice = null) }
            io { offlineDownloadQueue.clear() }
            io { cacheManager.clear() }
            val snapshot = io { cacheManager.snapshot() }
            _state.update {
                it.copy(
                    books = emptyList(),
                    libraries = emptyList(),
                    shelves = emptyList(),
                    bookDetail = null,
                    downloadedBookIds = emptySet(),
                    localBookIds = emptySet(),
                    cacheSnapshot = snapshot,
                    isCacheLoading = false,
                    notice = Notice.CacheCleared,
                )
            }
        }
    }

    fun setCompactBookGrid(enabled: Boolean) {
        sessionStore.writeCompactBookGrid(enabled)
        _state.update { it.copy(compactBookGrid = enabled) }
    }

    fun setThemeColor(themeColor: ThemeColorOption) {
        updateDisplaySettings { it.copy(themeColor = themeColor) }
    }

    fun setFont(font: FontOption) {
        updateDisplaySettings { it.copy(font = font) }
    }

    fun setFontScale(fontScale: Float) {
        updateDisplaySettings { it.copy(fontScale = fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)) }
    }

    fun signOut() = runRequest {
        _state.update { it.copy(isBusy = true, error = null) }
        io { offlineDownloadQueue.clear() }
        io { api.logout() }
        io { pendingSyncQueue.clear() }
        io { database.catalogDao().clearCatalog() }
        io { offlineBookStore.clear() }
        sessionStore.writeLocalMode(false)
        _state.value = newUiState()
    }

    fun coverUrl(bookId: Long): String? = if (_state.value.localMode) null else api.coverUrl(bookId)

    fun accessToken(): String? = sessionStore.read()?.accessToken

    fun serverUrl(): String? = sessionStore.read()?.baseUrl

    private suspend fun loadCatalog() {
        if (!isNetworkAvailable()) {
            sessionStore.writeLocalMode(true)
            loadCachedCatalog()
            return
        }
        val libraries: List<Library>
        val shelves: List<Shelf>
        val books: List<Book>
        try {
            libraries = io { api.libraries() }
            shelves = io { api.shelves() }
            books = io { api.books() }
        } catch (error: NetworkException) {
            sessionStore.writeLocalMode(true)
            loadCachedCatalog()
            return
        }
        if (sessionStore.readLocalMode()) return
        io {
            // Keep metadata for local copies even if the book disappears from the server catalog.
            val dao = database.catalogDao()
            val serverIds = books.mapTo(mutableSetOf(), Book::id)
            val savedBooks = dao.books().map { it.toModel() }.filter { it.id !in serverIds }
            val savedIds = offlineBookStore.localBookIds(savedBooks)
            database.catalogDao().replaceCatalog(
                libraries.map { it.toEntity() },
                shelves.map { it.toEntity() },
                (books + savedBooks.filter { it.id in savedIds }).map { it.toEntity() },
            )
        }
        val downloaded = io { offlineBookStore.downloadedBookIds(books) }
        val local = io { offlineBookStore.localBookIds(books) }
        _state.update {
            it.copy(
                isBusy = false,
                authenticated = true,
                libraries = libraries,
                shelves = shelves,
                books = books,
                downloadedBookIds = downloaded,
                localBookIds = local,
                localMode = false,
                offline = false,
            )
        }
    }

    /** Local mode only exposes complete files that the built-in readers can open. */
    private suspend fun loadCachedCatalog(localOnly: Boolean = true) {
        val dao = database.catalogDao()
        val libraries = io { dao.libraries().map { it.toModel() } }
        val shelves = io { dao.shelves().map { it.toModel() } }
        val catalog = io { dao.books().map { it.toModel() } }
        val local = io { offlineBookStore.localBookIds(catalog) }
        val books = io {
            if (localOnly) catalog.filter { it.id in local }.map(::withLocalProgress) else catalog
        }
        val downloaded = io { offlineBookStore.downloadedBookIds(books) }
        _state.update {
            it.copy(
                isBusy = false,
                authenticated = sessionStore.read() != null,
                offline = localOnly,
                localMode = localOnly,
                libraries = if (localOnly) libraries.filter { library -> books.any { it.libraryId == library.id } } else libraries,
                shelves = if (localOnly) shelves.filter { shelf -> books.any { book -> book.shelves.any { it.id == shelf.id } } } else shelves,
                books = books,
                downloadedBookIds = downloaded,
                localBookIds = local,
                bookDetail = it.bookDetail?.let { detail -> books.firstOrNull { it.id == detail.id } },
                isDetailLoading = false,
            )
        }
    }

    private fun withLocalProgress(book: Book): Book {
        val fileId = book.primaryFile?.id ?: return book
        val epub = sessionStore.readEpubPosition(book.id, fileId).takeIf { book.supportsEpubReader() }
        val pdf = sessionStore.readPdfPosition(book.id, fileId).takeIf { book.supportsPdfReader() }
        val progress = epub?.progressPercent ?: pdf?.progressPercent ?: return book
        return book.copy(
            epubProgress = epub?.let { EpubProgress(it.cfi, it.href, it.progressPercent) } ?: book.epubProgress,
            pdfProgress = pdf?.let { PdfProgress(it.page, it.progressPercent) } ?: book.pdfProgress,
            readStatus = if (progress >= 99f) "READ" else if (progress > 0f) "READING" else book.readStatus,
        )
    }

    private fun runRequest(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val uiError = error.toUiError()
            if (uiError == UiError.Unauthorized && sessionStore.read() != null) {
                sessionStore.clear(preserveReadingPositions = true)
                _state.value = newUiState(error = UiError.Unauthorized)
                return@launch
            }
            _state.update {
                it.copy(
                    isBusy = false,
                    isDetailLoading = false,
                    isActionRunning = false,
                    isCacheLoading = false,
                    error = uiError,
                )
            }
        }
    }

    private fun updateDisplaySettings(transform: (DisplaySettings) -> DisplaySettings) {
        val settings = transform(_state.value.displaySettings)
        sessionStore.writeDisplaySettings(settings)
        _state.update { it.copy(displaySettings = settings) }
    }

    private fun newUiState(error: UiError? = null): AppUiState = AppUiState(
        localMode = sessionStore.readLocalMode(),
        offline = sessionStore.readLocalMode(),
        compactBookGrid = sessionStore.readCompactBookGrid(),
        displaySettings = sessionStore.readDisplaySettings(),
        savedLogins = sessionStore.readSavedLogins(),
        error = error,
    )

    private companion object {
        const val MIN_FONT_SCALE = 0.85f
        const val MAX_FONT_SCALE = 1.30f
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }
}

/** Snapshot of the offline sync queue shown on the settings screen. */
data class SyncStatus(
    val pendingCount: Int = 0,
    val lastSyncTimeMs: Long? = null,
)

data class AppUiState(
    val isBusy: Boolean = false,
    val isActionRunning: Boolean = false,
    val isDetailLoading: Boolean = false,
    val authenticated: Boolean = false,
    /** True while the catalog is served from the offline cache. */
    val offline: Boolean = false,
    val localMode: Boolean = false,
    val connection: ServerConnection? = null,
    val savedLogins: List<SavedLogin> = emptyList(),
    val loginPrefill: SavedLogin? = null,
    val libraries: List<Library> = emptyList(),
    val shelves: List<Shelf> = emptyList(),
    val books: List<Book> = emptyList(),
    val bookDetail: Book? = null,
    val compactBookGrid: Boolean = false,
    val displaySettings: DisplaySettings = DisplaySettings(),
    val syncStatus: SyncStatus = SyncStatus(),
    val cacheSnapshot: CacheSnapshot = CacheSnapshot(),
    val isCacheLoading: Boolean = false,
    val isStatisticsLoading: Boolean = false,
    val readingStatistics: ReadingStatistics? = null,
    val statisticsError: UiError? = null,
    val error: UiError? = null,
    val notice: Notice? = null,
    val downloadedBookIds: Set<Long> = emptySet(),
    val localBookIds: Set<Long> = emptySet(),
    val offlineDownloadTasks: List<OfflineDownloadTask> = emptyList(),
)

sealed interface UiError {
    data object InvalidCredentials : UiError
    data object SavedLoginExpired : UiError
    data object Unauthorized : UiError
    data object Forbidden : UiError
    data object NotFound : UiError
    data object Network : UiError
    data object LocalBookUnavailable : UiError
    data object Generic : UiError
}

sealed interface Notice {
    data object StatusUpdated : Notice
    data object RatingUpdated : Notice
    data object ProgressSaved : Notice
    data object ShelvesUpdated : Notice
    data object CacheCleared : Notice
    data object Downloaded : Notice
    data object DownloadDeleted : Notice
}

private fun Exception.toUiError(): UiError = when (this) {
    is NetworkException -> UiError.Network
    is ApiException -> when (statusCode) {
        401 -> UiError.Unauthorized
        403 -> UiError.Forbidden
        404 -> UiError.NotFound
        else -> UiError.Generic
    }
    else -> UiError.Generic
}

private fun Book.withShelfChanges(
    allShelves: List<Shelf>,
    shelvesToAssign: Set<Long>,
    shelvesToUnassign: Set<Long>,
): Book {
    val updatedShelves = shelves
        .filterNot { it.id in shelvesToUnassign }
        .toMutableSet()
    updatedShelves += allShelves.filter { it.id in shelvesToAssign }
    return copy(shelves = updatedShelves)
}

private fun List<Shelf>.withBookShelfCounts(before: Book?, after: Book?): List<Shelf> {
    if (before == null || after == null) return this
    val beforeIds = before.shelves.mapTo(mutableSetOf(), Shelf::id)
    val afterIds = after.shelves.mapTo(mutableSetOf(), Shelf::id)
    return map { shelf ->
        val delta = (if (shelf.id in afterIds && shelf.id !in beforeIds) 1 else 0) -
            (if (shelf.id in beforeIds && shelf.id !in afterIds) 1 else 0)
        if (delta == 0) shelf else shelf.copy(bookCount = (shelf.bookCount + delta).coerceAtLeast(0))
    }
}

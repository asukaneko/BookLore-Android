package app.booklore.android.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.booklore.android.R
import app.booklore.android.core.cache.CacheBucketId
import app.booklore.android.core.cache.CacheSnapshot
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.DisplaySettings
import app.booklore.android.core.model.FontOption
import app.booklore.android.core.model.Library
import app.booklore.android.core.model.ReadingStatistics
import app.booklore.android.core.model.SavedLogin
import app.booklore.android.core.model.ServerConnection
import app.booklore.android.core.model.Shelf
import app.booklore.android.core.model.ThemeColorOption
import app.booklore.android.core.model.displayAuthors
import app.booklore.android.core.model.displayTitle
import app.booklore.android.core.model.formatName
import app.booklore.android.core.model.supportsEpubReader
import app.booklore.android.core.model.supportsPdfReader
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.security.SessionStore
import app.booklore.android.feature.AppUiState
import app.booklore.android.feature.AppViewModel
import app.booklore.android.feature.Notice
import app.booklore.android.feature.SyncStatus
import app.booklore.android.feature.UiError
import app.booklore.android.feature.reader.EpubReaderActivity
import app.booklore.android.feature.reader.PdfReaderActivity
import app.booklore.android.ui.theme.themeColorPreview
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val InputFieldShape = RoundedCornerShape(12.dp)
private val TitleTransitionDistance = 72.dp
private val PageTitleFontWeight = FontWeight.SemiBold
private const val ToolbarTitleFadeStart = 0.78f
private const val MIN_FONT_SCALE = 0.85f
private const val MAX_FONT_SCALE = 1.30f
private const val NAVIGATION_ANIMATION_MILLIS = 280
private const val FADE_ANIMATION_MILLIS = 180

/** Sort orders available in the library tab and the search screen. */
private enum class BookSortOption(val labelRes: Int) {
    DEFAULT(R.string.sort_default),
    TITLE(R.string.sort_title),
    AUTHOR(R.string.sort_author),
    LAST_READ(R.string.sort_last_read),
    ADDED(R.string.sort_added),
    RATING(R.string.sort_rating),
}

private fun List<Book>.sortedByOption(option: BookSortOption, ascending: Boolean): List<Book> {
    val comparator: Comparator<Book> = when (option) {
        BookSortOption.DEFAULT -> return this
        BookSortOption.TITLE -> compareBy { it.displayTitle().lowercase(Locale.getDefault()) }
        BookSortOption.AUTHOR -> compareBy { it.displayAuthors().lowercase(Locale.getDefault()) }
        BookSortOption.LAST_READ -> compareBy { it.lastReadTime.orEmpty() }
        BookSortOption.ADDED -> compareBy { it.addedOn.orEmpty() }
        BookSortOption.RATING -> compareBy { it.personalRating ?: 0 }
    }
    return sortedWith(if (ascending) comparator else comparator.reversed())
}

@Composable
fun BookLoreApp(viewModel: AppViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedBook = state.bookDetail
    val context = LocalContext.current
    var catalogTab by rememberSaveable { mutableStateOf(0) }
    var readerOpen by rememberSaveable { mutableStateOf(false) }
    var openReaderWhenBookSelected by rememberSaveable { mutableStateOf<Long?>(null) }
    val readerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshCatalog()
    }

    LaunchedEffect(state.bookDetail?.id) {
        if (state.bookDetail == null) readerOpen = false
    }
    LaunchedEffect(selectedBook?.id, openReaderWhenBookSelected) {
        val bookId = openReaderWhenBookSelected ?: return@LaunchedEffect
        val book = selectedBook?.takeIf { it.id == bookId } ?: return@LaunchedEffect
        if (book.supportsEpubReader()) {
            EpubReaderActivity.intent(context, book)?.let(readerLauncher::launch)
        } else if (book.supportsPdfReader()) {
            PdfReaderActivity.intent(context, book)?.let(readerLauncher::launch)
        } else {
            readerOpen = true
        }
        openReaderWhenBookSelected = null
    }

    BackHandler(enabled = readerOpen) {
        readerOpen = false
    }
    BackHandler(enabled = selectedBook != null && !readerOpen) {
        viewModel.clearBookSelection()
    }

    when {
        !state.authenticated -> ConnectionAndLoginScreen(
            state = state,
            onTestConnection = viewModel::testConnection,
            onBackToConnection = viewModel::clearConnection,
            onLogin = viewModel::login,
            onQuickLogin = viewModel::quickLogin,
            onRemoveSavedLogin = viewModel::removeSavedLogin,
            onDismissMessage = viewModel::dismissMessage,
        )

        else -> Box(modifier = Modifier.fillMaxSize()) {
            // Keep the catalog composed underneath the detail screen so scroll
            // positions, filters and search state survive the round trip and
            // returning lands back on the book that was opened.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (selectedBook != null) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                CatalogScreen(
                    state = state,
                    selectedTab = catalogTab,
                    onTabSelected = { catalogTab = it },
                    onRefresh = viewModel::refreshCatalog,
                    onBookSelected = viewModel::selectBook,
                    onContinueReading = { bookId ->
                        openReaderWhenBookSelected = bookId
                        viewModel.selectBook(bookId)
                    },
                    onCompactBookGridChanged = viewModel::setCompactBookGrid,
                    onThemeColorChanged = viewModel::setThemeColor,
                    onFontChanged = viewModel::setFont,
                    onFontScaleChanged = viewModel::setFontScale,
                    onSignOut = viewModel::signOut,
                    onRefreshSyncStatus = viewModel::refreshSyncStatus,
                    onSyncNow = viewModel::syncNow,
                    onRefreshCache = viewModel::refreshCache,
                    onClearCache = viewModel::clearCache,
                    onRefreshStatistics = viewModel::refreshStatistics,
                    coverUrl = viewModel::coverUrl,
                    accessToken = viewModel.accessToken(),
                    serverUrl = viewModel.serverUrl(),
                    onDismissMessage = viewModel::dismissMessage,
                )
            }
            if (selectedBook != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Consume every event of the gesture so touches on
                                // the overlay never leak into the catalog below.
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                            }
                        },
                ) {
                    if (readerOpen) {
                        ReaderPreparationScreen(
                            book = selectedBook,
                            isSaving = state.isActionRunning,
                            onBack = { readerOpen = false },
                            onSaveInitialProgress = viewModel::saveInitialProgress,
                        )
                    } else {
                        BookDetailScreen(
                            book = selectedBook,
                            isLoading = state.isDetailLoading,
                            isActionRunning = state.isActionRunning,
                            shelves = state.shelves,
                            onBack = viewModel::clearBookSelection,
                            onUpdateStatus = viewModel::updateReadStatus,
                            onUpdateRating = viewModel::updatePersonalRating,
                            onAssignShelves = viewModel::assignShelves,
                            onOpenReader = {
                                if (selectedBook.supportsEpubReader()) {
                                    EpubReaderActivity.intent(context, selectedBook)?.let(readerLauncher::launch)
                                } else if (selectedBook.supportsPdfReader()) {
                                    PdfReaderActivity.intent(context, selectedBook)?.let(readerLauncher::launch)
                                } else {
                                    readerOpen = true
                                }
                            },
                            coverUrl = viewModel.coverUrl(selectedBook.id),
                            accessToken = viewModel.accessToken(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionAndLoginScreen(
    state: AppUiState,
    onTestConnection: (String, Boolean) -> Unit,
    onBackToConnection: () -> Unit,
    onLogin: (String, String, Boolean) -> Unit,
    onQuickLogin: (SavedLogin) -> Unit,
    onRemoveSavedLogin: (SavedLogin) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var allowHttp by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rememberAccount by rememberSaveable { mutableStateOf(true) }

    // A quick login whose saved credentials expired returns here with the username prefilled.
    LaunchedEffect(state.loginPrefill) {
        state.loginPrefill?.let { prefill ->
            username = prefill.username
            rememberAccount = true
        }
    }

    if (state.connection != null) {
        BackHandler(onBack = onBackToConnection)
    }

    Scaffold(snackbarHost = { MessageHost(state, onDismissMessage) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (state.connection == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.connect_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.connect_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy,
                        singleLine = true,
                        shape = InputFieldShape,
                        label = { Text(stringResource(R.string.server_url)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.allow_http),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(checked = allowHttp, onCheckedChange = { allowHttp = it }, enabled = !state.isBusy)
                    }
                    Button(
                        onClick = { onTestConnection(serverUrl, allowHttp) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = serverUrl.isNotBlank() && !state.isBusy,
                    ) {
                        BusyLabel(state.isBusy, R.string.test_connection)
                    }
                    if (state.savedLogins.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = stringResource(R.string.saved_accounts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.savedLogins.forEach { saved ->
                            SavedLoginCard(
                                saved = saved,
                                isBusy = state.isBusy,
                                onClick = { onQuickLogin(saved) },
                                onRemove = { onRemoveSavedLogin(saved) },
                            )
                        }
                    }
                }
            } else {
                LoginForm(
                    connection = state.connection,
                    username = username,
                    onUsernameChange = { username = it },
                    password = password,
                    onPasswordChange = { password = it },
                    rememberAccount = rememberAccount,
                    onRememberAccountChange = { rememberAccount = it },
                    isBusy = state.isBusy,
                    onLogin = { onLogin(username, password, rememberAccount) },
                )
            }
        }
    }
}

@Composable
private fun SavedLoginCard(
    saved: SavedLogin,
    isBusy: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBusy, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = saved.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = saved.baseUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onRemove, enabled = !isBusy) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.remove_saved_account),
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    connection: ServerConnection,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    rememberAccount: Boolean,
    onRememberAccountChange: (Boolean) -> Unit,
    isBusy: Boolean,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(R.string.connection_ready), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        connection.version?.let {
            Text(
                stringResource(R.string.server_version, it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
            singleLine = true,
            shape = InputFieldShape,
            label = { Text(stringResource(R.string.username)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy,
            singleLine = true,
            shape = InputFieldShape,
            label = { Text(stringResource(R.string.password)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isBusy) { onRememberAccountChange(!rememberAccount) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = rememberAccount,
                onCheckedChange = onRememberAccountChange,
                enabled = !isBusy,
            )
            Text(
                text = stringResource(R.string.remember_account),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth(),
            enabled = username.isNotBlank() && password.isNotBlank() && !isBusy,
        ) {
            BusyLabel(isBusy, R.string.sign_in)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogScreen(
    state: AppUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRefresh: () -> Unit,
    onBookSelected: (Long) -> Unit,
    onContinueReading: (Long) -> Unit,
    onCompactBookGridChanged: (Boolean) -> Unit,
    onThemeColorChanged: (ThemeColorOption) -> Unit,
    onFontChanged: (FontOption) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onSignOut: () -> Unit,
    onRefreshSyncStatus: () -> Unit,
    onSyncNow: () -> Unit,
    onRefreshCache: () -> Unit,
    onClearCache: () -> Unit,
    onRefreshStatistics: (Int) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    serverUrl: String?,
    onDismissMessage: () -> Unit,
) {
    val tab = selectedTab
    var appearanceEditorOpen by rememberSaveable { mutableStateOf(false) }
    var statisticsOpen by rememberSaveable { mutableStateOf(false) }
    var cacheOpen by rememberSaveable { mutableStateOf(false) }
    val compactBookGrid = state.compactBookGrid
    val readingGridState = rememberLazyGridState()
    val collectionGridState = rememberLazyGridState()
    val searchGridState = rememberLazyGridState()
    val settingsGridState = rememberLazyGridState()
    val activeGridState = when (tab) {
        0 -> readingGridState
        1 -> collectionGridState
        2 -> searchGridState
        else -> settingsGridState
    }
    val pageTitle = when (tab) {
        0 -> stringResource(R.string.reading_now)
        1 -> stringResource(R.string.libraries)
        2 -> stringResource(R.string.navigation_search)
        else -> stringResource(R.string.settings)
    }
    val titleTransitionDistancePx = with(LocalDensity.current) { TitleTransitionDistance.toPx() }
    val titleScrollProgress by remember(activeGridState, titleTransitionDistancePx) {
        derivedStateOf {
            when {
                activeGridState.firstVisibleItemIndex > 0 -> 1f
                else -> (activeGridState.firstVisibleItemScrollOffset / titleTransitionDistancePx).coerceIn(0f, 1f)
            }
        }
    }
    val contentTitleAlpha = 1f - titleScrollProgress
    val toolbarTitleAlpha = ((titleScrollProgress - ToolbarTitleFadeStart) / (1f - ToolbarTitleFadeStart))
        .coerceIn(0f, 1f)

    BackHandler(enabled = appearanceEditorOpen || statisticsOpen || cacheOpen) {
        appearanceEditorOpen = false
        statisticsOpen = false
        cacheOpen = false
    }

    AnimatedContent(
        targetState = when {
            cacheOpen -> 3
            statisticsOpen -> 2
            appearanceEditorOpen -> 1
            else -> 0
        },
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                initialOffsetX = { direction * it },
            ) + fadeIn(animationSpec = tween(FADE_ANIMATION_MILLIS))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetX = { -direction * it },
                ) + fadeOut(animationSpec = tween(FADE_ANIMATION_MILLIS)))
        },
        label = "settings subpage",
    ) { catalogOverlay ->
        when (catalogOverlay) {
            3 -> CacheScreen(
                snapshot = state.cacheSnapshot,
                isLoading = state.isCacheLoading,
                onBack = { cacheOpen = false },
                onRefresh = onRefreshCache,
                onClear = onClearCache,
            )
            2 -> StatisticsScreen(
                books = state.books,
                isLoading = state.isStatisticsLoading,
                readingStatistics = state.readingStatistics,
                error = state.statisticsError,
                onBack = { statisticsOpen = false },
                onRefresh = onRefreshStatistics,
            )
            1 -> {
            AppearanceEditorScreen(
                displaySettings = state.displaySettings,
                onThemeColorChanged = onThemeColorChanged,
                onFontChanged = onFontChanged,
                onFontScaleChanged = onFontScaleChanged,
                onBack = { appearanceEditorOpen = false },
            )
            }
            else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = pageTitle,
                                modifier = Modifier.graphicsLayer { alpha = toolbarTitleAlpha },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = PageTitleFontWeight,
                            )
                        },
                        actions = {
                            if (tab == 0) {
                                IconButton(onClick = { statisticsOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.BarChart,
                                        contentDescription = stringResource(R.string.content_description_open_statistics),
                                    )
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    NavigationBar(modifier = Modifier.height(80.dp)) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { onTabSelected(0) },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = stringResource(R.string.navigation_reading),
                                )
                            },
                            label = { Text(stringResource(R.string.navigation_reading)) },
                            alwaysShowLabel = false,
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { onTabSelected(1) },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.LibraryBooks,
                                    contentDescription = stringResource(R.string.navigation_libraries),
                                )
                            },
                            label = { Text(stringResource(R.string.navigation_libraries)) },
                            alwaysShowLabel = false,
                        )
                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { onTabSelected(2) },
                            icon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.navigation_search),
                                )
                            },
                            label = { Text(stringResource(R.string.navigation_search)) },
                            alwaysShowLabel = false,
                        )
                        NavigationBarItem(
                            selected = tab == 3,
                            onClick = { onTabSelected(3) },
                            icon = {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.content_description_settings),
                                )
                            },
                            label = { Text(stringResource(R.string.settings)) },
                            alwaysShowLabel = false,
                        )
                    }
                },
                snackbarHost = { MessageHost(state, onDismissMessage) },
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    AnimatedVisibility(visible = state.offline) {
                        OfflineBanner(onRefresh = onRefresh)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (state.isBusy && state.books.isEmpty()) {
                            LoadingState()
                        } else {
                        AnimatedContent(
                            targetState = tab,
                            modifier = Modifier.fillMaxSize(),
                            transitionSpec = {
                                val direction = if (targetState > initialState) 1 else -1
                                (slideInHorizontally(
                                    animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                                    initialOffsetX = { direction * it },
                                ) + fadeIn(animationSpec = tween(FADE_ANIMATION_MILLIS))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -direction * it },
                                    ) + fadeOut(animationSpec = tween(FADE_ANIMATION_MILLIS)))
                            },
                            label = "catalog destination",
                        ) { destination ->
                            when (destination) {
                                0 -> ReadingNowScreen(
                                    books = state.books,
                                    onBookSelected = onBookSelected,
                                    onContinueReading = onContinueReading,
                                    coverUrl = coverUrl,
                                    accessToken = accessToken,
                                    gridState = readingGridState,
                                    pageTitle = stringResource(R.string.reading_now),
                                    pageTitleAlpha = contentTitleAlpha,
                                    compactBookGrid = compactBookGrid,
                                )
                                1 -> LibraryCatalog(
                                    libraries = state.libraries,
                                    shelves = state.shelves,
                                    books = state.books,
                                    onRefresh = onRefresh,
                                    onBookSelected = onBookSelected,
                                    coverUrl = coverUrl,
                                    accessToken = accessToken,
                                    gridState = collectionGridState,
                                    pageTitle = stringResource(R.string.libraries),
                                    pageTitleAlpha = contentTitleAlpha,
                                    compactBookGrid = compactBookGrid,
                                )
                                2 -> SearchCatalog(
                                    books = state.books,
                                    libraries = state.libraries,
                                    shelves = state.shelves,
                                    onRefresh = onRefresh,
                                    onBookSelected = onBookSelected,
                                    coverUrl = coverUrl,
                                    accessToken = accessToken,
                                    gridState = searchGridState,
                                    pageTitle = stringResource(R.string.navigation_search),
                                    pageTitleAlpha = contentTitleAlpha,
                                    compactBookGrid = compactBookGrid,
                                )
                                else -> SettingsCatalog(
                                    serverUrl = serverUrl,
                                    compactBookGrid = compactBookGrid,
                                    syncStatus = state.syncStatus,
                                    onCompactBookGridChanged = onCompactBookGridChanged,
                                    onOpenAppearanceEditor = { appearanceEditorOpen = true },
                                    onSignOut = onSignOut,
                                    onRefreshSyncStatus = onRefreshSyncStatus,
                                    onSyncNow = onSyncNow,
                                    onOpenCache = { cacheOpen = true },
                                    gridState = settingsGridState,
                                    pageTitle = stringResource(R.string.settings),
                                    pageTitleAlpha = contentTitleAlpha,
                                )
                            }
                        }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun OfflineBanner(onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRefresh),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private data class StatisticMetric(
    val label: String,
    val value: String,
)

private data class StatisticDistribution(
    val label: String,
    val count: Long,
)

private data class LibraryStatistics(
    val bookCount: Int,
    val authorCount: Int,
    val seriesCount: Int,
    val publisherCount: Int,
    val totalSizeKb: Long,
    val pageCount: Int,
    val formats: List<StatisticDistribution>,
    val statuses: List<StatisticDistribution>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsScreen(
    books: List<Book>,
    isLoading: Boolean,
    readingStatistics: ReadingStatistics?,
    error: UiError?,
    onBack: () -> Unit,
    onRefresh: (Int) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var selectedYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }

    LaunchedEffect(selectedYear) { onRefresh(selectedYear) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = PageTitleFontWeight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { selectedYear -= 1 }, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_previous_year),
                        )
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = PageTitleFontWeight,
                    )
                    IconButton(onClick = { selectedYear += 1 }, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.content_description_next_year),
                        )
                    }
                    IconButton(
                        onClick = { onRefresh(selectedYear) },
                        enabled = !isLoading,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.content_description_refresh_statistics),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.library_statistics)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.reading_statistics)) },
                )
            }
            when (selectedTab) {
                0 -> LibraryStatisticsContent(books)
                else -> ReadingStatisticsContent(
                    isLoading = isLoading,
                    statistics = readingStatistics?.takeIf { it.year == selectedYear },
                    error = error,
                    onRefresh = { onRefresh(selectedYear) },
                    books = books,
                )
            }
        }
    }
}

@Composable
private fun LibraryStatisticsContent(books: List<Book>) {
    val statistics = remember(books) { books.toLibraryStatistics() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatisticsMetricGrid(
                listOf(
                    StatisticMetric(stringResource(R.string.books), statistics.bookCount.toString()),
                    StatisticMetric(stringResource(R.string.statistics_authors), statistics.authorCount.toString()),
                    StatisticMetric(stringResource(R.string.statistics_series), statistics.seriesCount.toString()),
                    StatisticMetric(stringResource(R.string.statistics_publishers), statistics.publisherCount.toString()),
                    StatisticMetric(stringResource(R.string.statistics_library_size), formatLibrarySize(statistics.totalSizeKb)),
                ),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_formats),
                entries = statistics.formats,
                total = statistics.bookCount.toLong(),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_read_status),
                entries = statistics.statuses.map { status ->
                    status.copy(label = readingStatusLabel(status.label))
                },
                total = statistics.bookCount.toLong(),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_languages),
                entries = books.metadataDistribution { it.language },
                total = books.size.toLong(),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_metadata_score),
                entries = books.metadataScoreEntries(),
                total = books.size.toLong(),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_page_count_distribution),
                entries = books.pageCountEntries(),
                total = books.count { it.metadata?.pageCount != null }.toLong(),
            )
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_publication_timeline),
                entries = books.publicationYearEntries(),
                total = books.count { it.metadata?.publishedDate != null }.toLong(),
            )
        }
        item {
            ReadingJourneyPanel(books)
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_top_authors),
                entries = books.metadataListDistribution { it.authors },
                total = books.size.toLong(),
            )
        }
        item {
            AuthorUniversePanel(books)
        }
        item {
            StatisticsDistributionPanel(
                title = stringResource(R.string.statistics_publication_trend),
                entries = books.publicationEraEntries(),
                total = books.count { it.metadata?.publishedDate != null }.toLong(),
            )
        }
    }
}

@Composable
private fun ReadingStatisticsContent(
    isLoading: Boolean,
    statistics: ReadingStatistics?,
    error: UiError?,
    onRefresh: () -> Unit,
    books: List<Book>,
) {
    when {
        statistics == null && isLoading -> LoadingState()
        statistics == null && error != null -> StatisticsErrorState(error, onRefresh)
        statistics == null -> LoadingState()
        else -> {
            val sessions = statistics.heatmap.sumOf { it.count }
            val readingDays = statistics.heatmap.count { it.count > 0 }
            val durationSeconds = statistics.peakHours.sumOf { it.totalDurationSeconds }
            val favoriteDay = statistics.favoriteDays.maxByOrNull { it.sessionCount }
            val topGenres = statistics.genres
                .filter { it.genre.isNotBlank() }
                .sortedByDescending { it.totalDurationSeconds }
                .take(5)
                .map { genre ->
                    StatisticDistribution(
                        label = genre.genre,
                        count = genre.totalSessions,
                    )
                }
            val favoriteDayEntries = statistics.favoriteDays
                .sortedByDescending { it.sessionCount }
                .map { day -> StatisticDistribution(day.dayName, day.sessionCount) }
            val ratingEntries = statistics.bookDistributions.ratingDistribution
                .sortedBy { it.rating }
                .map { rating ->
                    StatisticDistribution(
                        label = stringResource(R.string.statistics_rating_value, rating.rating),
                        count = rating.count,
                    )
                }
            val progressEntries = statistics.bookDistributions.progressDistribution
                .map { progress -> StatisticDistribution(progress.range, progress.count) }
            val totalStatusBooks = statistics.bookDistributions.statusDistribution.sumOf { it.count }
            val statusEntries = statistics.bookDistributions.statusDistribution
                .sortedByDescending { it.count }
                .map { status ->
                    StatisticDistribution(
                        label = readingStatusLabel(status.status),
                        count = status.count,
                    )
                }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    StatisticsMetricGrid(
                        listOf(
                            StatisticMetric(stringResource(R.string.statistics_reading_days), readingDays.toString()),
                            StatisticMetric(stringResource(R.string.statistics_sessions), sessions.toString()),
                            StatisticMetric(
                                stringResource(R.string.statistics_reading_time),
                                formatStatisticsDuration(durationSeconds),
                            ),
                            StatisticMetric(
                                stringResource(R.string.statistics_current_streak),
                                stringResource(R.string.statistics_days_value, statistics.streak.currentStreak),
                            ),
                        ),
                    )
                }
                item {
                    ReadingSessionHeatmapPanel(
                        year = statistics.year,
                        days = statistics.heatmap,
                        currentStreak = statistics.streak.currentStreak,
                        longestStreak = statistics.streak.longestStreak,
                        totalReadingDays = statistics.streak.totalReadingDays,
                    )
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_favorite_days),
                        entries = favoriteDayEntries,
                        total = favoriteDayEntries.sumOf { it.count },
                        valueLabel = { entry -> stringResource(R.string.statistics_sessions_value, entry.count) },
                    )
                }
                item {
                    PeakHoursPanel(statistics.peakHours)
                }
                item {
                    ReadingTimelinePanel(statistics.timeline)
                }
                item {
                    CompletionHeatmapPanel(statistics.completionHeatmap)
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_personal_ratings),
                        entries = ratingEntries,
                        total = ratingEntries.sumOf { it.count },
                    )
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_reading_progress),
                        entries = progressEntries,
                        total = progressEntries.sumOf { it.count },
                    )
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_read_status),
                        entries = statusEntries,
                        total = totalStatusBooks,
                    )
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_genres),
                        entries = topGenres,
                        total = topGenres.sumOf { it.count },
                        valueLabel = { entry -> stringResource(R.string.statistics_sessions_value, entry.count) },
                    )
                }
                item {
                    CompletionTimelinePanel(statistics.completionTimeline)
                }
                item {
                    ReadingClockPanel(
                        entries = statistics.peakHours,
                        longestStreak = statistics.streak.longestStreak,
                        favoriteDay = favoriteDay?.dayName,
                    )
                }
                item {
                    PageTurnerPanel(statistics.pageTurnerScores)
                }
                item {
                    CompletionRacePanel(statistics.completionRace)
                }
                item {
                    ReadingSurvivalPanel(books.readingSurvivalEntries())
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_book_length),
                        entries = books.pageCountEntries(),
                        total = books.count { it.metadata?.pageCount != null }.toLong(),
                    )
                }
                item {
                    RatingTastePanel(books)
                }
                item {
                    SeriesProgressPanel(books)
                }
                item {
                    ReadingDnaPanel(books)
                }
                item {
                    ReadingHabitsPanel(statistics.speed)
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_book_flow),
                        entries = statusEntries,
                        total = totalStatusBooks,
                    )
                }
                item {
                    ReadingDebtPanel(books, statistics.completionTimeline)
                }
                item {
                    StatisticsDistributionPanel(
                        title = stringResource(R.string.statistics_publication_era),
                        entries = books.publicationEraEntries(),
                        total = books.count { it.metadata?.publishedDate != null }.toLong(),
                    )
                }
                item {
                    SessionArchetypesPanel(statistics.sessionScatter)
                }
            }
        }
    }
}

@Composable
private fun StatisticsPanel(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
            content()
        }
    }
}

@Composable
private fun ReadingSessionHeatmapPanel(
    year: Int,
    days: List<app.booklore.android.core.model.ReadingSessionHeatmapDay>,
    currentStreak: Int,
    longestStreak: Int,
    totalReadingDays: Int,
) {
    val counts = remember(days) { days.associate { it.date to it.count } }
    val maxCount = remember(days) { days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1L }
    val start = remember(year) {
        LocalDate.of(year, 1, 1).let { date -> date.minusDays((date.dayOfWeek.value - 1).toLong()) }
    }
    val end = remember(year) { LocalDate.of(year, 12, 31) }
    val totalCells = remember(start, end) {
        val dayCount = end.toEpochDay() - start.toEpochDay() + 1
        (((dayCount + 6) / 7) * 7).toInt()
    }
    val primary = MaterialTheme.colorScheme.primary

    StatisticsPanel(stringResource(R.string.statistics_session_heatmap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    stringResource(R.string.statistics_weekday_mon),
                    stringResource(R.string.statistics_weekday_tue),
                    stringResource(R.string.statistics_weekday_wed),
                    stringResource(R.string.statistics_weekday_thu),
                    stringResource(R.string.statistics_weekday_fri),
                    stringResource(R.string.statistics_weekday_sat),
                    stringResource(R.string.statistics_weekday_sun),
                ).forEach { day ->
                    Text(day, modifier = Modifier.height(11.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(totalCells / 7) { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(7) { day ->
                            val date = start.plusDays((week * 7 + day).toLong())
                val count = counts[date.toString()] ?: 0L
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (date.year != year || count == 0L) {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        } else {
                                            primary.copy(alpha = 0.25f + (count.toFloat() / maxCount) * 0.75f)
                                        },
                                    ),
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.statistics_current_streak_value, currentStreak),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.statistics_longest_streak_value, longestStreak),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.statistics_reading_days_value, totalReadingDays),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ReadingTimelinePanel(timeline: List<app.booklore.android.core.model.ReadingSessionTimeline>) {
    StatisticsPanel(stringResource(R.string.statistics_session_timeline)) {
        if (timeline.isEmpty()) {
            StatisticsNoData()
        } else {
            timeline.sortedByDescending { it.totalDurationSeconds }.take(6).forEach { entry ->
                StatisticHighlightRow(
                    label = entry.bookTitle.ifBlank { stringResource(R.string.unknown_author) },
                    value = stringResource(
                        R.string.statistics_session_duration_value,
                        entry.totalSessions,
                        formatStatisticsDuration(entry.totalDurationSeconds),
                    ),
                )
            }
        }
    }
}

@Composable
private fun CompletionHeatmapPanel(entries: List<app.booklore.android.core.model.BookCompletionHeatmap>) {
    val rows = remember(entries) { entries.groupBy { it.year }.toSortedMap().toList().takeLast(4).reversed() }
    StatisticsPanel(stringResource(R.string.statistics_reading_activity_heatmap)) {
        if (rows.isEmpty()) {
            StatisticsNoData()
        } else {
            val maxCount = entries.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1L
            val primary = MaterialTheme.colorScheme.primary
            rows.forEach { (year, months) ->
                val counts = months.associate { it.month to it.count }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(year.toString(), modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall)
                    (1..12).forEach { month ->
                val count = counts[month] ?: 0L
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (count == 0L) MaterialTheme.colorScheme.surfaceVariant
                                    else primary.copy(alpha = 0.25f + (count.toFloat() / maxCount) * 0.75f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PeakHoursPanel(entries: List<app.booklore.android.core.model.PeakReadingHour>) {
    val hours = remember(entries) {
        List(24) { hour -> entries.firstOrNull { it.hourOfDay == hour } }
    }
    val maxSessions = hours.maxOfOrNull { it?.sessionCount ?: 0L }?.coerceAtLeast(1L) ?: 1L
    val peak = hours.filterNotNull().maxByOrNull { it.sessionCount }
    StatisticsPanel(stringResource(R.string.statistics_peak_hours)) {
        if (entries.isEmpty()) {
            StatisticsNoData()
        } else {
            StatisticsLineChart(hours.map { (it?.sessionCount ?: 0L).toFloat() / maxSessions })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("00", style = MaterialTheme.typography.labelSmall)
                Text("06", style = MaterialTheme.typography.labelSmall)
                Text("12", style = MaterialTheme.typography.labelSmall)
                Text("18", style = MaterialTheme.typography.labelSmall)
                Text("23", style = MaterialTheme.typography.labelSmall)
            }
            peak?.let { hour ->
                val averageMinutes = if (hour.sessionCount > 0) {
                    hour.totalDurationSeconds / 60 / hour.sessionCount
                } else {
                    0
                }
                StatisticHighlightRow(
                    stringResource(R.string.statistics_peak_hour),
                    stringResource(R.string.statistics_hour_value, hour.hourOfDay),
                )
                StatisticHighlightRow(
                    stringResource(R.string.statistics_average_session_length),
                    stringResource(R.string.statistics_duration_minutes, averageMinutes),
                )
            }
        }
    }
}

@Composable
private fun CompletionTimelinePanel(entries: List<app.booklore.android.core.model.ReadingCompletionTimeline>) {
    val distributions = remember(entries) {
        entries.filter { it.month in 1..12 }
            .map { entry -> StatisticDistribution("${entry.month}", entry.finishedBooks) }
    }
    StatisticsPanel(stringResource(R.string.statistics_completion_timeline)) {
        if (distributions.isEmpty()) {
            StatisticsNoData()
        } else {
            distributions.forEach { entry ->
                StatisticDistributionRow(
                    entry = entry.copy(label = stringResource(R.string.statistics_month_value, entry.label)),
                    total = distributions.maxOf { it.count }.coerceAtLeast(1),
                    value = stringResource(R.string.statistics_books_value, entry.count),
                )
            }
        }
    }
}

@Composable
private fun PageTurnerPanel(entries: List<app.booklore.android.core.model.PageTurnerScore>) {
    StatisticsPanel(stringResource(R.string.statistics_page_turner)) {
        if (entries.isEmpty()) {
            StatisticsNoData()
        } else {
            entries.sortedByDescending { it.gripScore }.take(6).forEach { entry ->
                StatisticHighlightRow(
                    label = entry.bookTitle,
                    value = stringResource(R.string.statistics_grip_score_value, entry.gripScore),
                )
            }
        }
    }
}

@Composable
private fun CompletionRacePanel(entries: List<app.booklore.android.core.model.CompletionRaceEntry>) {
    val races = remember(entries) {
        entries.groupBy { it.bookId }
            .mapNotNull { (_, sessions) ->
                val datedSessions = sessions.mapNotNull { session ->
                    session.sessionDate.toEpochDayOrNull()?.let { day -> RacePoint(day, session.endProgress) }
                }.sortedBy { it.day }
                if (datedSessions.isEmpty()) null
                else RaceSeries(
                    title = sessions.first().bookTitle,
                    points = datedSessions.map { point ->
                        point.copy(day = point.day - datedSessions.first().day)
                    },
                )
            }
            .sortedBy { race -> race.points.last().day }
            .take(6)
    }
    StatisticsPanel(stringResource(R.string.statistics_completion_race)) {
        if (races.isEmpty()) {
            StatisticsNoData()
        } else {
            CompletionRaceChart(races)
            val days = races.map { it.points.last().day }
            StatisticsMetricGrid(
                listOf(
                    StatisticMetric(
                        stringResource(R.string.statistics_average_completion_days),
                        stringResource(R.string.statistics_days_value, days.average().roundToInt()),
                    ),
                    StatisticMetric(
                        stringResource(R.string.statistics_completed_books),
                        races.size.toString(),
                    ),
                ),
            )
        }
    }
}

@Composable
private fun RatingTastePanel(books: List<Book>) {
    val rated = remember(books) {
        books.mapNotNull { book ->
            val personal = book.personalRating ?: return@mapNotNull null
            val external = book.metadata?.rating ?: book.metadata?.goodreadsRating ?: return@mapNotNull null
            personal.toDouble() to external
        }
    }
    StatisticsPanel(stringResource(R.string.statistics_rating_taste)) {
        if (rated.isEmpty()) {
            StatisticsNoData()
        } else {
            val personalAverage = rated.map { it.first }.average()
            val externalAverage = rated.map { it.second }.average()
            StatisticHighlightRow(
                stringResource(R.string.statistics_your_average_rating),
                String.format(Locale.getDefault(), "%.1f", personalAverage),
            )
            StatisticHighlightRow(
                stringResource(R.string.statistics_external_average_rating),
                String.format(Locale.getDefault(), "%.1f", externalAverage),
            )
            StatisticHighlightRow(
                stringResource(R.string.statistics_rated_books),
                rated.size.toString(),
            )
        }
    }
}

@Composable
private fun SeriesProgressPanel(books: List<Book>) {
    val series = remember(books) {
        books.filter { !it.metadata?.seriesName.isNullOrBlank() }
            .groupBy { it.metadata?.seriesName.orEmpty() }
            .map { (name, volumes) ->
                StatisticDistribution(name, volumes.count { it.readStatus == "READ" }.toLong()) to volumes.size.toLong()
            }
            .sortedByDescending { (entry, total) -> if (total == 0L) 0f else entry.count.toFloat() / total }
    }
    StatisticsPanel(stringResource(R.string.statistics_series_progress)) {
        if (series.isEmpty()) {
            StatisticsNoData()
        } else {
            series.take(6).forEach { (entry, total) ->
                StatisticDistributionRow(
                    entry = entry,
                    total = total,
                    value = stringResource(R.string.statistics_books_read_value, entry.count, total),
                )
            }
        }
    }
}

@Composable
private fun ReadingDnaPanel(books: List<Book>) {
    val scores = remember(books) { books.readingDnaScores() }
    val labels = listOf(
        stringResource(R.string.statistics_dna_adventurous),
        stringResource(R.string.statistics_dna_completionist),
        stringResource(R.string.statistics_dna_intellectual),
        stringResource(R.string.statistics_dna_emotional),
        stringResource(R.string.statistics_dna_patient),
        stringResource(R.string.statistics_dna_ambitious),
    )
    StatisticsPanel(stringResource(R.string.statistics_reading_dna)) {
        if (books.isEmpty()) {
            StatisticsNoData()
        } else {
            StatisticsRadarChart(scores)
            labels.zip(scores).forEach { (label, score) ->
                StatisticDistributionRow(
                    entry = StatisticDistribution(label, score.toLong()),
                    total = 100,
                    value = stringResource(R.string.statistics_percent_value, score),
                )
            }
        }
    }
}

@Composable
private fun ReadingHabitsPanel(entries: List<app.booklore.android.core.model.ReadingSpeed>) {
    val speeds = remember(entries) {
        entries.filter { it.date.isNotBlank() }
            .sortedByDescending { it.avgProgressPerMinute }
            .take(6)
            .map { entry -> StatisticDistribution(entry.date, (entry.avgProgressPerMinute * 100).roundToInt().toLong()) }
    }
    StatisticsDistributionPanel(
        title = stringResource(R.string.statistics_reading_habits),
        entries = speeds,
        total = speeds.maxOfOrNull { it.count } ?: 0,
        valueLabel = { entry -> stringResource(R.string.statistics_progress_per_minute_value, entry.count / 100.0) },
    )
}

@Composable
private fun ReadingDebtPanel(
    books: List<Book>,
    completions: List<app.booklore.android.core.model.ReadingCompletionTimeline>,
) {
    val added = remember(books) { books.count { it.addedOn != null }.toLong() }
    val finished = remember(completions) { completions.sumOf { it.finishedBooks } }
    val backlog = (added - finished).coerceAtLeast(0)
    StatisticsPanel(stringResource(R.string.statistics_reading_debt)) {
        StatisticHighlightRow(stringResource(R.string.statistics_books_added), added.toString())
        StatisticHighlightRow(stringResource(R.string.statistics_books_finished), finished.toString())
        StatisticHighlightRow(stringResource(R.string.statistics_current_backlog), backlog.toString())
    }
}

@Composable
private fun SessionArchetypesPanel(entries: List<app.booklore.android.core.model.ReadingSessionScatter>) {
    val archetypes = remember(entries) {
        entries.groupingBy { session ->
            when (session.hourOfDay.toInt()) {
                in 5..10 -> "morning"
                in 11..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }
        }.eachCount().map { (period, count) -> StatisticDistribution(period, count.toLong()) }
    }
    val labels = mapOf(
        "morning" to stringResource(R.string.statistics_archetype_morning),
        "afternoon" to stringResource(R.string.statistics_archetype_afternoon),
        "evening" to stringResource(R.string.statistics_archetype_evening),
        "night" to stringResource(R.string.statistics_archetype_night),
    )
    StatisticsDistributionPanel(
        title = stringResource(R.string.statistics_session_archetypes),
        entries = archetypes.map { entry -> entry.copy(label = labels[entry.label].orEmpty()) },
        total = archetypes.sumOf { it.count },
        valueLabel = { entry -> stringResource(R.string.statistics_sessions_value, entry.count) },
    )
}

@Composable
private fun StatisticsNoData() {
    Text(
        text = stringResource(R.string.statistics_no_data),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StatisticsMetricGrid(metrics: List<StatisticMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowMetrics.forEach { metric ->
                    StatisticsMetricCard(metric, Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatisticsMetricCard(metric: StatisticMetric, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = metric.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = PageTitleFontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = metric.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReadingClockPanel(
    entries: List<app.booklore.android.core.model.PeakReadingHour>,
    longestStreak: Int,
    favoriteDay: String?,
) {
    val minutes = remember(entries) {
        List(24) { hour ->
            entries.firstOrNull { it.hourOfDay == hour }?.totalDurationSeconds?.div(60) ?: 0L
        }
    }
    val peakHour = minutes.indices.maxByOrNull { minutes[it] }
    val nightMinutes = listOf(20, 21, 22, 23, 0, 1, 2).sumOf { minutes[it] }
    val morningMinutes = (5..11).sumOf { minutes[it] }
    val readerType = when {
        nightMinutes > morningMinutes * 1.2f -> stringResource(R.string.statistics_night_reader)
        morningMinutes > nightMinutes * 1.2f -> stringResource(R.string.statistics_early_reader)
        else -> stringResource(R.string.statistics_balanced_reader)
    }
    StatisticsPanel(stringResource(R.string.statistics_reading_clock)) {
        if (entries.isEmpty()) {
            StatisticsNoData()
            return@StatisticsPanel
        }
        ReadingClockGraphic(minutes)
        StatisticsMetricGrid(
            listOf(
                StatisticMetric(
                    stringResource(R.string.statistics_peak_reading_time),
                    peakHour?.let { stringResource(R.string.statistics_hour_value, it) }.orEmpty(),
                ),
                StatisticMetric(
                    stringResource(R.string.statistics_reader_type),
                    readerType,
                ),
            ),
        )
        StatisticHighlightRow(
            stringResource(R.string.statistics_longest_streak),
            stringResource(R.string.statistics_days_value, longestStreak),
        )
        StatisticHighlightRow(
            stringResource(R.string.statistics_favorite_day),
            favoriteDay?.takeIf(String::isNotBlank) ?: stringResource(R.string.statistics_no_data),
        )
    }
}

@Composable
private fun ReadingSurvivalPanel(entries: List<StatisticDistribution>) {
    val started = entries.firstOrNull()?.count ?: 0L
    StatisticsPanel(stringResource(R.string.statistics_reading_survival)) {
        if (started == 0L) {
            StatisticsNoData()
        } else {
            val percentages = entries.map { it.count.toFloat() / started }
            StatisticsLineChart(percentages, color = MaterialTheme.colorScheme.tertiary)
            StatisticsMetricGrid(
                listOf(
                    StatisticMetric(
                        stringResource(R.string.statistics_started_books),
                        started.toString(),
                    ),
                    StatisticMetric(
                        stringResource(R.string.statistics_completion_rate),
                        stringResource(
                            R.string.statistics_percent_value,
                            (percentages.lastOrNull() ?: 0f).times(100).roundToInt(),
                        ),
                    ),
                ),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                entries.forEach { entry ->
                    Text(entry.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun ReadingClockGraphic(minutes: List<Long>) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val max = minutes.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
    ) {
        val radius = minOf(size.width, size.height) * 0.42f
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        minutes.forEachIndexed { hour, duration ->
            val radians = ((hour * 15f - 90f) * PI / 180.0).toFloat()
            val direction = androidx.compose.ui.geometry.Offset(cos(radians), sin(radians))
            val outer = center + direction * radius
            val length = radius * (0.18f + 0.82f * duration.toFloat() / max)
            val inner = center + direction * (radius - length)
            drawLine(track, center, outer, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
            if (duration > 0) {
                drawLine(
                    primary.copy(alpha = 0.35f + 0.65f * duration.toFloat() / max),
                    inner,
                    outer,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        drawCircle(surface, radius = radius * 0.38f, center = center)
        drawCircle(primary.copy(alpha = 0.25f), radius = radius * 0.38f, center = center, style = Stroke(1.dp.toPx()))
    }
}

@Composable
private fun StatisticsLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val lineColor = color ?: MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val horizontalPadding = 12.dp.toPx()
        val verticalPadding = 14.dp.toPx()
        val graphWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
        val graphHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)
        repeat(4) { index ->
            val y = verticalPadding + graphHeight * index / 3f
            drawLine(track, androidx.compose.ui.geometry.Offset(horizontalPadding, y), androidx.compose.ui.geometry.Offset(size.width - horizontalPadding, y), 1.dp.toPx())
        }
        if (values.isEmpty()) return@Canvas
        val points = values.mapIndexed { index, value ->
            val x = if (values.size == 1) size.width / 2f else horizontalPadding + graphWidth * index / (values.size - 1)
            val y = verticalPadding + graphHeight * (1f - value.coerceIn(0f, 1f))
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { point -> drawCircle(lineColor, radius = 3.dp.toPx(), center = point) }
    }
}

@Composable
private fun StatisticsRadarChart(values: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) * 0.38f
        fun point(index: Int, multiplier: Float): androidx.compose.ui.geometry.Offset {
            val radians = ((index * 360f / values.size - 90f) * PI / 180.0).toFloat()
            return center + androidx.compose.ui.geometry.Offset(cos(radians), sin(radians)) * radius * multiplier
        }
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { multiplier ->
            val path = Path().apply {
                moveTo(point(0, multiplier).x, point(0, multiplier).y)
                (1 until values.size).forEach { index -> lineTo(point(index, multiplier).x, point(index, multiplier).y) }
                close()
            }
            drawPath(path, color = track, style = Stroke(1.dp.toPx()))
        }
        values.indices.forEach { index -> drawLine(track, center, point(index, 1f), 1.dp.toPx()) }
        val profile = Path().apply {
            moveTo(point(0, values[0].coerceIn(0, 100) / 100f).x, point(0, values[0].coerceIn(0, 100) / 100f).y)
            values.drop(1).forEachIndexed { offset, value ->
                val profilePoint = point(offset + 1, value.coerceIn(0, 100) / 100f)
                lineTo(profilePoint.x, profilePoint.y)
            }
            close()
        }
        drawPath(profile, color = primary.copy(alpha = 0.22f))
        drawPath(profile, color = primary, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}

private data class RacePoint(val day: Long, val progress: Float)

private data class RaceSeries(val title: String, val points: List<RacePoint>)

@Composable
private fun CompletionRaceChart(races: List<RaceSeries>) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFDC5F45),
        Color(0xFF348D86),
        Color(0xFF8B6BA6),
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val totalDays = races.maxOfOrNull { it.points.last().day }?.coerceAtLeast(1L) ?: 1L
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        val padding = 14.dp.toPx()
        val graphWidth = (size.width - padding * 2).coerceAtLeast(1f)
        val graphHeight = (size.height - padding * 2).coerceAtLeast(1f)
        repeat(5) { index ->
            val y = padding + graphHeight * index / 4f
            drawLine(track, androidx.compose.ui.geometry.Offset(padding, y), androidx.compose.ui.geometry.Offset(size.width - padding, y), 1.dp.toPx())
        }
        races.forEachIndexed { index, race ->
            val path = Path()
            race.points.forEachIndexed { pointIndex, point ->
                val x = padding + graphWidth * point.day.toFloat() / totalDays
                val y = padding + graphHeight * (1f - point.progress.coerceIn(0f, 100f) / 100f)
                if (pointIndex == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val color = colors[index % colors.size]
            drawPath(path, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
    }
    races.forEachIndexed { index, race ->
        StatisticHighlightRow(
            race.title,
            stringResource(R.string.statistics_days_value, race.points.last().day.toInt()),
        )
    }
}

@Composable
private fun StatisticHighlightRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = PageTitleFontWeight)
    }
}

@Composable
private fun StatisticsDistributionPanel(
    title: String,
    entries: List<StatisticDistribution>,
    total: Long,
    valueLabel: @Composable (StatisticDistribution) -> String = { it.count.toString() },
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.statistics_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                entries.take(6).forEach { entry ->
                    StatisticDistributionRow(entry, total, valueLabel(entry))
                }
            }
        }
    }
}

@Composable
private fun StatisticDistributionRow(entry: StatisticDistribution, total: Long, value: String) {
    val fraction = if (total > 0) (entry.count.toFloat() / total).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = entry.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(12.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun StatisticsErrorState(error: UiError, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(error.message(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
    }
}

private fun List<Book>.readingSurvivalEntries(): List<StatisticDistribution> {
    val started = filter { it.readingProgressPercent() > 0f }
    if (started.isEmpty()) return emptyList()
    return listOf(0, 10, 25, 50, 75, 90, 100).map { threshold ->
        StatisticDistribution("$threshold%", started.count { it.readingProgressPercent() >= threshold }.toLong())
    }
}

private fun List<Book>.pageCountEntries(): List<StatisticDistribution> = mapNotNull { it.metadata?.pageCount }
    .groupingBy { pages ->
        when {
            pages < 200 -> "< 200"
            pages < 400 -> "200-399"
            pages < 600 -> "400-599"
            else -> "600+"
        }
    }
    .eachCount()
    .map { (range, count) -> StatisticDistribution(range, count.toLong()) }

private fun List<Book>.publicationEraEntries(): List<StatisticDistribution> = mapNotNull { book ->
    book.metadata?.publishedDate?.take(4)?.toIntOrNull()
}.groupingBy { year -> "${(year / 10) * 10}s" }
    .eachCount()
    .map { (decade, count) -> StatisticDistribution(decade, count.toLong()) }
    .sortedBy { it.label }

private fun List<Book>.readingDnaScores(): List<Int> {
    if (isEmpty()) return List(6) { 0 }
    val categories = flatMap { it.metadata?.categories.orEmpty() }.map(String::lowercase).toSet()
    val languages = mapNotNull { it.metadata?.language }.toSet()
    val completed = count { it.readStatus == "READ" }
    val rated = filter { it.personalRating != null }
    val longBooks = count { (it.metadata?.pageCount ?: 0) > 400 }
    val emotional = count { book ->
        book.metadata?.categories.orEmpty().any { category ->
            listOf("romance", "poetry", "drama", "memoir").any { keyword ->
                category.lowercase().contains(keyword)
            }
        }
    }
    val seriesBooks = count { !it.metadata?.seriesName.isNullOrBlank() }
    val classicBooks = count { book -> book.metadata?.publishedDate?.take(4)?.toIntOrNull()?.let { it < LocalDate.now().year - 30 } == true }
    val total = size.toFloat().coerceAtLeast(1f)
    return listOf(
        ((categories.size / (total * 0.4f)).coerceAtMost(1f) * 75 + ((languages.size - 1).coerceAtLeast(0) * 12.5f).coerceAtMost(25f)).roundToInt(),
        ((completed / total) * 60 + (rated.count { it.personalRating ?: 0 >= 4 } / rated.size.coerceAtLeast(1).toFloat()) * 40).roundToInt(),
        ((longBooks / total) * 65 + (categories.count { it.contains("history") || it.contains("science") } / total) * 35).roundToInt(),
        ((emotional / total) * 70 + (rated.size / total) * 30).roundToInt(),
        ((longBooks / total) * 45 + (seriesBooks / total) * 55).roundToInt(),
        (size.coerceAtMost(100) * 0.4f + (classicBooks / total) * 30 + (completed / total) * 30).roundToInt(),
    ).map { it.coerceIn(0, 100) }
}

private fun Book.readingProgressPercent(): Float = maxOf(
    epubProgress?.percentage ?: 0f,
    pdfProgress?.percentage ?: 0f,
)

private fun List<Book>.metadataDistribution(selector: (app.booklore.android.core.model.BookMetadata) -> String?): List<StatisticDistribution> =
    mapNotNull { book -> book.metadata?.let(selector)?.trim()?.takeIf(String::isNotBlank) }
        .groupingBy { it }
        .eachCount()
        .map { (label, count) -> StatisticDistribution(label, count.toLong()) }
        .sortedByDescending { it.count }

private fun List<Book>.metadataListDistribution(
    selector: (app.booklore.android.core.model.BookMetadata) -> List<String>,
): List<StatisticDistribution> = flatMap { book -> book.metadata?.let(selector).orEmpty() }
    .map(String::trim)
    .filter(String::isNotBlank)
    .groupingBy { it }
    .eachCount()
    .map { (label, count) -> StatisticDistribution(label, count.toLong()) }
    .sortedByDescending { it.count }

private fun List<Book>.metadataScoreEntries(): List<StatisticDistribution> = groupingBy { book ->
    val metadata = book.metadata
    val available = listOf(
        metadata?.title,
        metadata?.description,
        metadata?.publisher,
        metadata?.publishedDate,
        metadata?.language,
        metadata?.seriesName,
        metadata?.pageCount?.toString(),
    ).count { !it.isNullOrBlank() } + if (metadata?.authors.orEmpty().isNotEmpty()) 1 else 0
    when ((available * 100) / 8) {
        in 90..100 -> "90-100%"
        in 70..89 -> "70-89%"
        in 50..69 -> "50-69%"
        in 25..49 -> "25-49%"
        else -> "0-24%"
    }
}.eachCount()
    .map { (score, count) -> StatisticDistribution(score, count.toLong()) }
    .sortedByDescending { it.label }

private fun List<Book>.publicationYearEntries(): List<StatisticDistribution> = mapNotNull { book ->
    book.metadata?.publishedDate?.take(4)?.toIntOrNull()
}.groupingBy { it.toString() }
    .eachCount()
    .map { (year, count) -> StatisticDistribution(year, count.toLong()) }
    .sortedBy { it.label }
    .takeLast(8)

@Composable
private fun ReadingJourneyPanel(books: List<Book>) {
    val added = remember(books) { books.count { it.addedOn != null } }
    val finished = remember(books) { books.count { it.dateFinished != null || it.readStatus == "READ" } }
    val backlog = (added - finished).coerceAtLeast(0)
    StatisticsPanel(stringResource(R.string.statistics_reading_journey)) {
        StatisticHighlightRow(stringResource(R.string.statistics_books_added), added.toString())
        StatisticHighlightRow(stringResource(R.string.statistics_books_finished), finished.toString())
        StatisticHighlightRow(stringResource(R.string.statistics_current_backlog), backlog.toString())
    }
}

@Composable
private fun AuthorUniversePanel(books: List<Book>) {
    val entries = remember(books) { books.metadataListDistribution { it.authors }.take(6) }
    StatisticsDistributionPanel(
        title = stringResource(R.string.statistics_author_universe),
        entries = entries,
        total = entries.sumOf { it.count },
        valueLabel = { entry -> stringResource(R.string.statistics_books_value, entry.count) },
    )
}

private fun List<Book>.toLibraryStatistics(): LibraryStatistics {
    val formats = groupingBy { it.formatName().uppercase(Locale.getDefault()) }
        .eachCount()
        .map { (format, count) -> StatisticDistribution(format, count.toLong()) }
        .sortedByDescending { it.count }
    val statuses = groupingBy { it.readStatus?.uppercase(Locale.getDefault()).orEmpty().ifBlank { "UNREAD" } }
        .eachCount()
        .map { (status, count) -> StatisticDistribution(status, count.toLong()) }
        .sortedByDescending { it.count }
    val authors = flatMap { it.metadata?.authors.orEmpty() }
        .map(String::trim)
        .filter(String::isNotBlank)
        .toSet()
    val series = mapNotNull { it.metadata?.seriesName?.trim()?.takeIf(String::isNotBlank) }.toSet()
    val publishers = mapNotNull { it.metadata?.publisher?.trim()?.takeIf(String::isNotBlank) }.toSet()
    return LibraryStatistics(
        bookCount = size,
        authorCount = authors.size,
        seriesCount = series.size,
        publisherCount = publishers.size,
        totalSizeKb = sumOf { it.primaryFile?.fileSizeKb ?: 0L },
        pageCount = sumOf { it.metadata?.pageCount ?: 0 },
        formats = formats,
        statuses = statuses,
    )
}

@Composable
private fun readingStatusLabel(status: String): String = when (status.uppercase(Locale.getDefault())) {
    "UNREAD", "NOT_STARTED" -> stringResource(R.string.not_started)
    "READING", "RE_READING", "PARTIALLY_READ", "PAUSED" -> stringResource(R.string.reading)
    "READ" -> stringResource(R.string.read)
    else -> status.replace('_', ' ').lowercase(Locale.getDefault())
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
private fun formatStatisticsDuration(totalSeconds: Long): String = when {
    totalSeconds >= 3_600 -> stringResource(
        R.string.statistics_duration_hours_minutes,
        totalSeconds / 3_600,
        (totalSeconds % 3_600) / 60,
    )
    totalSeconds >= 60 -> stringResource(R.string.statistics_duration_minutes, totalSeconds / 60)
    else -> stringResource(R.string.statistics_duration_seconds, totalSeconds)
}

private fun formatLibrarySize(sizeKb: Long): String = when {
    sizeKb >= 1_048_576 -> String.format(Locale.getDefault(), "%.1f GB", sizeKb / 1_048_576.0)
    sizeKb >= 1_024 -> String.format(Locale.getDefault(), "%.1f MB", sizeKb / 1_024.0)
    else -> "$sizeKb KB"
}

private fun String.toEpochDayOrNull(): Long? = runCatching {
    Instant.parse(this).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
}.recoverCatching {
    LocalDate.parse(this).toEpochDay()
}.getOrNull()

@Composable
private fun ReadingNowScreen(
    books: List<Book>,
    onBookSelected: (Long) -> Unit,
    onContinueReading: (Long) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
    compactBookGrid: Boolean,
) {
    val currentBook = remember(books) {
        books
            .asSequence()
            .filter { it.readStatus == "READING" }
            .sortedByDescending { it.lastReadTime.orEmpty() }
            .firstOrNull()
    }
    val recentBooks = remember(books, currentBook?.id) {
        books
            .asSequence()
            .filter { it.lastReadTime != null && it.id != currentBook?.id }
            .sortedByDescending { it.lastReadTime }
            .take(8)
            .toList()
    }

    LazyVerticalGrid(
        // Same grid metrics as the library catalog; header/hero items span the full row.
        columns = GridCells.Adaptive(minSize = if (compactBookGrid) 104.dp else 144.dp),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compactBookGrid) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageTitle(pageTitle, pageTitleAlpha)
        }
        if (currentBook != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.continue_reading),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = PageTitleFontWeight,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    BookCover(
                        bookId = currentBook.id,
                        title = currentBook.displayTitle(),
                        url = coverUrl(currentBook.id),
                        accessToken = accessToken,
                        modifier = Modifier.width(120.dp).aspectRatio(0.68f),
                    )
                    Spacer(Modifier.width(20.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            currentBook.displayTitle(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = PageTitleFontWeight,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        currentBook.displayAuthors().takeIf(String::isNotBlank)?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        LastReadText(currentBook)
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = { onContinueReading(currentBook.id) }) {
                            Text(stringResource(R.string.continue_reading))
                        }
                    }
                }
            }
        } else {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.no_current_reading),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (recentBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(R.string.recently_read),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = PageTitleFontWeight,
                )
            }
            items(recentBooks, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    coverUrl = coverUrl(book.id),
                    accessToken = accessToken,
                    compact = compactBookGrid,
                    onClick = { onBookSelected(book.id) },
                )
            }
        }
    }
}

@Composable
private fun LastReadText(book: Book) {
    val date = book.lastReadDate() ?: return
    Text(
        text = stringResource(R.string.last_read, date),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SearchCatalog(
    books: List<Book>,
    libraries: List<Library>,
    shelves: List<Shelf>,
    onRefresh: () -> Unit,
    onBookSelected: (Long) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
    compactBookGrid: Boolean,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedLibraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedShelfId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedStatus by rememberSaveable { mutableStateOf("all") }
    var selectedFormat by rememberSaveable { mutableStateOf("all") }
    var sortOption by rememberSaveable { mutableStateOf(BookSortOption.DEFAULT) }
    var sortAscending by rememberSaveable { mutableStateOf(true) }
    var resultsOpen by rememberSaveable { mutableStateOf(false) }

    val formats = remember(books) {
        books.asSequence()
            .mapNotNull { book -> book.primaryFile?.bookType ?: book.primaryFile?.extension }
            .map { it.uppercase() }
            .distinct()
            .sorted()
            .toList()
    }
    val results = remember(books, query, selectedLibraryId, selectedShelfId, selectedStatus, selectedFormat, sortOption, sortAscending) {
        books.asSequence()
            .filter { book ->
                (selectedLibraryId == null || book.libraryId == selectedLibraryId) &&
                    (selectedShelfId == null || book.shelves.any { it.id == selectedShelfId }) &&
                    (selectedStatus == "all" || book.readStatus == selectedStatus) &&
                    (selectedFormat == "all" || book.formatName().equals(selectedFormat, ignoreCase = true)) &&
                    (query.isBlank() || book.displayTitle().contains(query, ignoreCase = true) || book.displayAuthors().contains(query, ignoreCase = true))
            }
            .toList()
            .sortedByOption(sortOption, sortAscending)
    }

    BackHandler(enabled = resultsOpen) { resultsOpen = false }

    AnimatedContent(
        targetState = resultsOpen,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (slideInHorizontally(
                animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                initialOffsetX = { direction * it },
            ) + fadeIn(animationSpec = tween(FADE_ANIMATION_MILLIS))) togetherWith
                (slideOutHorizontally(
                    animationSpec = tween(NAVIGATION_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
                    targetOffsetX = { -direction * it },
                ) + fadeOut(animationSpec = tween(FADE_ANIMATION_MILLIS)))
        },
        label = "search results",
    ) { showResults ->
        if (showResults) {
            SearchResultScreen(
                results = results,
                catalogEmpty = books.isEmpty(),
                onBack = { resultsOpen = false },
                onRefresh = onRefresh,
                onBookSelected = onBookSelected,
                coverUrl = coverUrl,
                accessToken = accessToken,
                compactBookGrid = compactBookGrid,
            )
        } else {
            SearchFilterForm(
                query = query,
                onQueryChange = { query = it },
                libraries = libraries,
                shelves = shelves,
                selectedLibraryId = selectedLibraryId,
                onLibrarySelected = { selectedLibraryId = it },
                selectedShelfId = selectedShelfId,
                onShelfSelected = { selectedShelfId = it },
                selectedStatus = selectedStatus,
                onStatusSelected = { selectedStatus = it },
                formats = formats,
                selectedFormat = selectedFormat,
                onFormatSelected = { selectedFormat = it },
                sortOption = sortOption,
                onSortSelected = { sortOption = it },
                sortAscending = sortAscending,
                onToggleSortOrder = { sortAscending = !sortAscending },
                onSearch = { resultsOpen = true },
                onReset = {
                    query = ""
                    selectedLibraryId = null
                    selectedShelfId = null
                    selectedStatus = "all"
                    selectedFormat = "all"
                    sortOption = BookSortOption.DEFAULT
                    sortAscending = true
                },
                gridState = gridState,
                pageTitle = pageTitle,
                pageTitleAlpha = pageTitleAlpha,
            )
        }
    }
}

@Composable
private fun SearchFilterForm(
    query: String,
    onQueryChange: (String) -> Unit,
    libraries: List<Library>,
    shelves: List<Shelf>,
    selectedLibraryId: Long?,
    onLibrarySelected: (Long?) -> Unit,
    selectedShelfId: Long?,
    onShelfSelected: (Long?) -> Unit,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    formats: List<String>,
    selectedFormat: String,
    onFormatSelected: (String) -> Unit,
    sortOption: BookSortOption,
    onSortSelected: (BookSortOption) -> Unit,
    sortAscending: Boolean,
    onToggleSortOrder: () -> Unit,
    onSearch: () -> Unit,
    onReset: () -> Unit,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            PageTitle(pageTitle, pageTitleAlpha)
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true,
                    shape = InputFieldShape,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(stringResource(R.string.search_keyword)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
                if (libraries.isNotEmpty()) {
                    FilterGroupLabel(stringResource(R.string.libraries))
                    FilterRow(
                        selectedId = selectedLibraryId,
                        allLabel = stringResource(R.string.filter_all),
                        items = libraries.map { it.id to it.name },
                        onSelected = onLibrarySelected,
                    )
                }
                if (shelves.isNotEmpty()) {
                    FilterGroupLabel(stringResource(R.string.shelves))
                    FilterRow(
                        selectedId = selectedShelfId,
                        allLabel = stringResource(R.string.filter_all),
                        items = shelves.map { it.id to it.name },
                        onSelected = onShelfSelected,
                    )
                }
                FilterGroupLabel(stringResource(R.string.reading_status))
                OptionChipRow(
                    selectedKey = selectedStatus,
                    items = listOf(
                        "all" to stringResource(R.string.filter_all),
                        "UNREAD" to stringResource(R.string.not_started),
                        "READING" to stringResource(R.string.reading),
                        "READ" to stringResource(R.string.read),
                    ),
                    onSelected = onStatusSelected,
                )
                if (formats.isNotEmpty()) {
                    FilterGroupLabel(stringResource(R.string.filter_format))
                    OptionChipRow(
                        selectedKey = selectedFormat,
                        items = listOf("all" to stringResource(R.string.filter_all)) + formats.map { it to it },
                        onSelected = onFormatSelected,
                    )
                }
                FilterGroupLabel(stringResource(R.string.filter_sort))
                SortChipRow(
                    selected = sortOption,
                    ascending = sortAscending,
                    onSelected = onSortSelected,
                    onToggleOrder = onToggleSortOrder,
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.search_action))
                }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.reset_filters))
                }
            }
        }
    }
}

@Composable
private fun SearchResultScreen(
    results: List<Book>,
    catalogEmpty: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onBookSelected: (Long) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    compactBookGrid: Boolean,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
            }
            Text(
                text = stringResource(R.string.search_results),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = PageTitleFontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.book_count, results.size),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (catalogEmpty) {
                    EmptyState(onRefresh)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.search_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (compactBookGrid) 124.dp else 144.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(if (compactBookGrid) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactBookGrid) 12.dp else 16.dp),
            ) {
                items(results, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        coverUrl = coverUrl(book.id),
                        accessToken = accessToken,
                        compact = compactBookGrid,
                        onClick = { onBookSelected(book.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterGroupLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = PageTitleFontWeight,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OptionChipRow(
    selectedKey: String,
    items: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        lazyListItems(items, key = { it.first }) { (key, label) ->
            FilterChip(
                selected = selectedKey == key,
                onClick = { onSelected(key) },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = InputFieldShape,
            )
        }
    }
}

@Composable
private fun SortChipRow(
    selected: BookSortOption,
    ascending: Boolean,
    onSelected: (BookSortOption) -> Unit,
    onToggleOrder: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            lazyListItems(BookSortOption.entries, key = { it.name }) { option ->
                CatalogFilterChip(
                    selected = selected == option,
                    label = stringResource(option.labelRes),
                    onClick = { onSelected(option) },
                )
            }
        }
        SortOrderButton(
            ascending = ascending,
            enabled = selected != BookSortOption.DEFAULT,
            onClick = onToggleOrder,
        )
    }
}

/** Round ascending/descending toggle that matches the catalog filter chips. */
@Composable
private fun SortOrderButton(
    ascending: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.padding(end = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
        contentColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)),
    ) {
        Box(
            modifier = Modifier.size(width = 40.dp, height = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = stringResource(R.string.content_description_toggle_sort_order),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun LibraryCatalog(
    libraries: List<Library>,
    shelves: List<Shelf>,
    books: List<Book>,
    onRefresh: () -> Unit,
    onBookSelected: (Long) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
    compactBookGrid: Boolean,
) {
    var selectedLibraryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedShelfId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Restore the persisted sort selection; write back on every change.
    val context = LocalContext.current
    val sessionStore = remember { SessionStore(context.applicationContext) }
    val savedSort = remember { sessionStore.readLibrarySortSettings() }
    var sortOption by rememberSaveable {
        mutableStateOf(
            savedSort.first
                ?.let { value -> BookSortOption.entries.firstOrNull { it.name == value } }
                ?: BookSortOption.DEFAULT,
        )
    }
    var sortAscending by rememberSaveable { mutableStateOf(savedSort.second) }
    LaunchedEffect(sortOption, sortAscending) {
        sessionStore.writeLibrarySortSettings(sortOption.name, sortAscending)
    }
    val shownBooks = remember(books, selectedLibraryId, selectedShelfId, sortOption, sortAscending) {
        books.filter { book ->
            (selectedLibraryId == null || book.libraryId == selectedLibraryId) &&
                (selectedShelfId == null || book.shelves.any { it.id == selectedShelfId })
        }.sortedByOption(sortOption, sortAscending)
    }
    BookGrid(
        books = shownBooks,
        onRefresh = onRefresh,
        onBookSelected = onBookSelected,
        coverUrl = coverUrl,
        accessToken = accessToken,
        gridState = gridState,
        pageTitle = pageTitle,
        pageTitleAlpha = pageTitleAlpha,
        compactBookGrid = compactBookGrid,
        filters = {
            if (libraries.isNotEmpty()) {
                CatalogFilterGroup(
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    title = stringResource(R.string.libraries),
                ) {
                    FilterRow(
                        selectedId = selectedLibraryId,
                        allLabel = stringResource(R.string.filter_all),
                        items = libraries.map { it.id to it.name },
                        onSelected = { selectedLibraryId = it },
                    )
                }
            }
            if (shelves.isNotEmpty()) {
                CatalogFilterGroup(
                    icon = Icons.Filled.CollectionsBookmark,
                    title = stringResource(R.string.shelves),
                ) {
                    FilterRow(
                        selectedId = selectedShelfId,
                        allLabel = stringResource(R.string.filter_all),
                        items = shelves.map { it.id to it.name },
                        onSelected = { selectedShelfId = it },
                    )
                }
            }
            CatalogFilterGroup(
                icon = Icons.AutoMirrored.Filled.Sort,
                title = stringResource(R.string.filter_sort),
            ) {
                SortChipRow(
                    selected = sortOption,
                    ascending = sortAscending,
                    onSelected = { sortOption = it },
                    onToggleOrder = { sortAscending = !sortAscending },
                )
            }
        },
    )
}

/**
 * One labeled filter section on the library screen: a small tinted icon plus a
 * group title above the chip row it wraps.
 */
@Composable
private fun CatalogFilterGroup(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        content()
    }
}

/** Shared chip style for the catalog filters: a soft filled pill with a check mark when active. */
@Composable
private fun CatalogFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = RoundedCornerShape(20.dp),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f),
        ),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun FilterRow(
    selectedId: Long?,
    allLabel: String,
    items: List<Pair<Long, String>>,
    onSelected: (Long?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            CatalogFilterChip(
                selected = selectedId == null,
                label = allLabel,
                onClick = { onSelected(null) },
            )
        }
        lazyListItems(items, key = { it.first }) { (id, label) ->
            CatalogFilterChip(
                selected = selectedId == id,
                label = label,
                onClick = { onSelected(id) },
            )
        }
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onRefresh: () -> Unit,
    onBookSelected: (Long) -> Unit,
    coverUrl: (Long) -> String?,
    accessToken: String?,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
    compactBookGrid: Boolean,
    filters: @Composable () -> Unit,
) {
    LazyVerticalGrid(
        // Compact cells (104dp) fit three columns on common phone widths.
        columns = GridCells.Adaptive(minSize = if (compactBookGrid) 104.dp else 144.dp),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compactBookGrid) 8.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactBookGrid) 12.dp else 16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            PageTitle(pageTitle, pageTitleAlpha)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                filters()
            }
        }
        if (books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(onRefresh)
            }
        } else {
            items(books, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    coverUrl = coverUrl(book.id),
                    accessToken = accessToken,
                    compact = compactBookGrid,
                    onClick = { onBookSelected(book.id) },
                )
            }
        }
    }
}

@Composable
private fun PageTitle(title: String, alpha: Float) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer { this.alpha = alpha },
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = PageTitleFontWeight,
    )
}

@Composable
private fun BookCard(book: Book, coverUrl: String?, accessToken: String?, compact: Boolean, onClick: () -> Unit) {
    val title = book.displayTitle()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "open $title" },
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 6.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
        ) {
            BookCover(
                bookId = book.id,
                title = title,
                url = coverUrl,
                accessToken = accessToken,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
            )
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val authors = book.displayAuthors()
            if (authors.isNotBlank()) {
                Text(
                    authors,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailScreen(
    book: Book,
    isLoading: Boolean,
    isActionRunning: Boolean,
    shelves: List<Shelf>,
    onBack: () -> Unit,
    onUpdateStatus: (Long, String) -> Unit,
    onUpdateRating: (Long, Int) -> Unit,
    onAssignShelves: (Long, Set<Long>, Set<Long>) -> Unit,
    onOpenReader: () -> Unit,
    coverUrl: String?,
    accessToken: String?,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.book_details), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        if (isLoading) {
            LoadingState(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(verticalAlignment = Alignment.Top) {
                        BookCover(
                            bookId = book.id,
                            title = book.displayTitle(),
                            url = coverUrl,
                            accessToken = accessToken,
                            modifier = Modifier.width(132.dp).aspectRatio(0.68f),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(book.displayTitle(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                book.displayAuthors().ifBlank { stringResource(R.string.unknown_author) },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            AssistChip(onClick = {}, label = { Text(book.formatName()) })
                            book.metadata?.seriesName?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                item {
                    Text(stringResource(R.string.reading_status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    StatusChips(selected = book.readStatus, enabled = !isActionRunning) { status -> onUpdateStatus(book.id, status) }
                }
                item {
                    Text(stringResource(R.string.personal_rating), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    RatingChips(selected = book.personalRating, enabled = !isActionRunning) { rating -> onUpdateRating(book.id, rating) }
                }
                if (shelves.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.shelves), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        ShelfChips(
                            allShelves = shelves,
                            bookShelfIds = book.shelves.map(Shelf::id).toSet(),
                            enabled = !isActionRunning,
                        ) { shelfId, selected ->
                            val assign = if (selected) setOf(shelfId) else emptySet()
                            val unassign = if (selected) emptySet() else setOf(shelfId)
                            onAssignShelves(book.id, assign, unassign)
                        }
                    }
                }
                item {
                    Text(
                        book.metadata?.description?.takeIf(String::isNotBlank) ?: stringResource(R.string.no_description),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                item {
                    Button(
                        onClick = onOpenReader,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !book.isPhysical.orFalse(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.open_reader))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChips(selected: String?, enabled: Boolean, onSelected: (String) -> Unit) {
    val statuses = listOf(
        "UNREAD" to R.string.not_started,
        "READING" to R.string.reading,
        "READ" to R.string.read,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        statuses.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                enabled = enabled,
                label = { Text(stringResource(label)) },
            )
        }
    }
}

@Composable
private fun RatingChips(selected: Int?, enabled: Boolean, onSelected: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(5) { index ->
            val rating = index + 1
            FilterChip(
                selected = selected == rating,
                onClick = { onSelected(rating) },
                enabled = enabled,
                label = { Text(rating.toString()) },
            )
        }
    }
}

/** Multi-select shelf membership chips shown on the book detail screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShelfChips(
    allShelves: List<Shelf>,
    bookShelfIds: Set<Long>,
    enabled: Boolean,
    onShelfToggled: (shelfId: Long, selected: Boolean) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        allShelves.forEach { shelf ->
            FilterChip(
                selected = shelf.id in bookShelfIds,
                onClick = { onShelfToggled(shelf.id, shelf.id !in bookShelfIds) },
                enabled = enabled,
                label = { Text(shelf.name) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderPreparationScreen(
    book: Book,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSaveInitialProgress: (Book) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(book.displayTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(Icons.Filled.Book, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.reader_preparing_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.reader_preparing_body, book.formatName()), style = MaterialTheme.typography.bodyLarge)
            if (book.primaryFile?.id != null) {
                Button(onClick = { onSaveInitialProgress(book) }, enabled = !isSaving) {
                    BusyLabel(isSaving, R.string.save_progress)
                }
            }
        }
    }
}

@Composable
private fun SettingsCatalog(
    serverUrl: String?,
    compactBookGrid: Boolean,
    syncStatus: SyncStatus,
    onCompactBookGridChanged: (Boolean) -> Unit,
    onOpenAppearanceEditor: () -> Unit,
    onOpenCache: () -> Unit,
    onSignOut: () -> Unit,
    onRefreshSyncStatus: () -> Unit,
    onSyncNow: () -> Unit,
    gridState: LazyGridState,
    pageTitle: String,
    pageTitleAlpha: Float,
) {
    // Re-reads the offline queue whenever the settings tab becomes visible.
    LaunchedEffect(Unit) { onRefreshSyncStatus() }
    val lastSyncLabel = syncStatus.lastSyncTimeMs?.let { timeMs ->
        remember(timeMs) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
                .format(Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault()))
        }
    }
    val queueLabel = if (syncStatus.pendingCount == 0) {
        stringResource(R.string.sync_queue_empty)
    } else {
        stringResource(R.string.sync_queue_pending, syncStatus.pendingCount)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PageTitle(pageTitle, pageTitleAlpha)
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.about_server), style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
                serverUrl?.let { url ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.server_address), style = MaterialTheme.typography.labelLarge)
                        Text(url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(R.string.storage), style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenCache)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.cache), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.cache_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = stringResource(R.string.content_description_open_cache),
                    )
                }
                HorizontalDivider()
                Text(stringResource(R.string.sync), style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSyncNow)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.sync_now), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            queueLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.sync_last_time, lastSyncLabel ?: stringResource(R.string.sync_never)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.sync_now),
                    )
                }
                HorizontalDivider()
                Text(stringResource(R.string.display), style = MaterialTheme.typography.titleMedium, fontWeight = PageTitleFontWeight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenAppearanceEditor)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.appearance_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.content_description_open_appearance),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.compact_book_grid), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.compact_book_grid_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = compactBookGrid, onCheckedChange = onCompactBookGridChanged)
                }
                HorizontalDivider()
                OutlinedButton(onClick = onSignOut) { Text(stringResource(R.string.sign_out)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CacheScreen(
    snapshot: CacheSnapshot,
    isLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onClear: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val hasCache = snapshot.totalBytes > 0

    LaunchedEffect(Unit) { onRefresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cache)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.content_description_refresh_cache),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.cache_total_size, formatCacheSize(snapshot.totalBytes)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = PageTitleFontWeight,
                    )
                    Text(
                        stringResource(R.string.cache_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            lazyListItems(snapshot.buckets, key = { it.id }) { bucket ->
                CacheBucketRow(bucket)
            }
            item {
                Button(
                    onClick = { confirmClear = true },
                    enabled = hasCache && !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_cache))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_cache_title)) },
            text = { Text(stringResource(R.string.clear_cache_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClear()
                    },
                ) {
                    Text(stringResource(R.string.clear_cache))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CacheBucketRow(bucket: app.booklore.android.core.cache.CacheBucket) {
    val titleRes = when (bucket.id) {
        CacheBucketId.COVER_IMAGES -> R.string.cache_cover_images
        CacheBucketId.READER_FILES -> R.string.cache_reader_files
        CacheBucketId.OFFLINE_CATALOG -> R.string.cache_offline_catalog
    }
    val summaryRes = when (bucket.id) {
        CacheBucketId.COVER_IMAGES -> R.string.cache_cover_images_summary
        CacheBucketId.READER_FILES -> R.string.cache_reader_files_summary
        CacheBucketId.OFFLINE_CATALOG -> R.string.cache_offline_catalog_summary
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(titleRes),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatCacheSize(bucket.bytes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(summaryRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.cache_item_count, bucket.itemCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private fun formatCacheSize(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(Locale.getDefault(), bytes / 1024.0)
    bytes < 1024L * 1024L * 1024L -> "%.1f MB".format(Locale.getDefault(), bytes / (1024.0 * 1024.0))
    else -> "%.2f GB".format(Locale.getDefault(), bytes / (1024.0 * 1024.0 * 1024.0))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceEditorScreen(
    displaySettings: DisplaySettings,
    onThemeColorChanged: (ThemeColorOption) -> Unit,
    onFontChanged: (FontOption) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val fontScalePercent = (displaySettings.fontScale * 100).roundToInt()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.appearance), fontWeight = PageTitleFontWeight) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.theme_color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = PageTitleFontWeight,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    ThemeColorOption.entries.forEach { option ->
                        ThemeColorSwatch(
                            option = option,
                            label = themeColorLabel(option),
                            selected = displaySettings.themeColor == option,
                            onClick = { onThemeColorChanged(option) },
                        )
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text(
                    text = stringResource(R.string.font),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = PageTitleFontWeight,
                )
            }
            lazyListItems(FontOption.entries, key = { it.name }) { option ->
                FontOptionRow(
                    label = fontLabel(option),
                    selected = displaySettings.font == option,
                    onClick = { onFontChanged(option) },
                )
            }
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.font_size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = PageTitleFontWeight,
                    )
                    Text(
                        text = stringResource(R.string.font_size_value, fontScalePercent),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            item {
                Slider(
                    value = displaySettings.fontScale,
                    onValueChange = onFontScaleChanged,
                    valueRange = MIN_FONT_SCALE..MAX_FONT_SCALE,
                    steps = 8,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.font_size_small),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.font_size_large),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeColorSwatch(
    option: ThemeColorOption,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(themeColorPreview(option))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FontOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun themeColorLabel(option: ThemeColorOption): String = when (option) {
    ThemeColorOption.FOREST -> stringResource(R.string.theme_color_forest)
    ThemeColorOption.OCEAN -> stringResource(R.string.theme_color_ocean)
    ThemeColorOption.CORAL -> stringResource(R.string.theme_color_coral)
}

@Composable
private fun fontLabel(option: FontOption): String = when (option) {
    FontOption.SANS -> stringResource(R.string.font_sans)
    FontOption.SERIF -> stringResource(R.string.font_serif)
    FontOption.MONOSPACE -> stringResource(R.string.font_monospace)
}

@Composable
@Suppress("ProduceStateDoesNotAssignValue") // Lint does not resolve ProduceStateScope's K2 receiver assignment.
private fun BookCover(bookId: Long, title: String, url: String?, accessToken: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cachedCover by produceState<File?>(initialValue = null, bookId, url) {
        this.value = null
        this.value = withContext(Dispatchers.IO) {
            val sessionStore = SessionStore(context.applicationContext)
            val session = sessionStore.read()
            if (session == null) {
                null
            } else {
                val cacheDirectory = File(context.cacheDir, "book-covers")
                val destination = File(cacheDirectory, "${session.baseUrl.hashCode()}-$bookId.cover")
                if (destination.isFile && destination.length() > 0L) {
                    destination
                } else {
                    runCatching { BookLoreApi(sessionStore).downloadCover(bookId, destination) }.getOrNull()
                }
            }
        }
    }
    val request = remember(cachedCover, url, accessToken) {
        (cachedCover ?: url)?.let {
            ImageRequest.Builder(context)
                .data(it)
                .apply {
                    if (cachedCover == null) {
                        accessToken?.let { token -> addHeader("Authorization", "Bearer $token") }
                    }
                }
                .crossfade(true)
                .build()
        }
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (request != null) {
            var imageState by remember(request) { mutableStateOf<AsyncImagePainter.State?>(null) }
            if (imageState is AsyncImagePainter.State.Error) {
                BookCoverFallback(title)
            } else {
                AsyncImage(
                    model = request,
                    contentDescription = stringResource(R.string.content_description_cover, title),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onState = { imageState = it },
                )
                if (imageState !is AsyncImagePainter.State.Success) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else {
            BookCoverFallback(title)
        }
    }
}

@Composable
private fun BookCoverFallback(title: String) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val compactCover = maxWidth < 80.dp || maxHeight < 120.dp
        val titleFontSize = fallbackCoverTitleSize(title.length, compactCover)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (!compactCover) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = LocalContentColor.current,
                )
            }
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = titleFontSize,
                    lineHeight = titleFontSize * 1.15f,
                ),
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun fallbackCoverTitleSize(titleLength: Int, compactCover: Boolean) = when {
    compactCover && titleLength > 80 -> 4.sp
    compactCover && titleLength > 50 -> 5.sp
    compactCover && titleLength > 30 -> 6.sp
    compactCover && titleLength > 18 -> 7.sp
    compactCover -> 8.sp
    titleLength > 100 -> 6.sp
    titleLength > 70 -> 7.sp
    titleLength > 45 -> 8.sp
    titleLength > 25 -> 9.sp
    else -> 10.sp
}

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.no_books), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun BusyLabel(isBusy: Boolean, label: Int) {
    if (isBusy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        Spacer(Modifier.width(8.dp))
    }
    Text(stringResource(label))
}

@Composable
private fun MessageHost(state: AppUiState, onDismiss: () -> Unit) {
    val hostState = remember { SnackbarHostState() }
    val message = state.error?.message() ?: state.notice?.message()
    LaunchedEffect(message) {
        if (message != null) {
            hostState.showSnackbar(message)
            onDismiss()
        }
    }
    SnackbarHost(hostState = hostState)
}

@Composable
private fun UiError.message(): String = when (this) {
    UiError.InvalidCredentials -> stringResource(R.string.invalid_credentials)
    UiError.SavedLoginExpired -> stringResource(R.string.saved_login_expired)
    UiError.Unauthorized -> stringResource(R.string.error_unauthorized)
    UiError.Forbidden -> stringResource(R.string.error_forbidden)
    UiError.NotFound -> stringResource(R.string.error_not_found)
    UiError.Network -> stringResource(R.string.error_network)
    UiError.Generic -> stringResource(R.string.error_generic)
}

@Composable
private fun Notice.message(): String = when (this) {
    Notice.StatusUpdated -> stringResource(R.string.status_updated)
    Notice.RatingUpdated -> stringResource(R.string.rating_updated)
    Notice.ProgressSaved -> stringResource(R.string.progress_saved)
    Notice.ShelvesUpdated -> stringResource(R.string.shelves_updated)
    Notice.CacheCleared -> stringResource(R.string.cache_cleared)
}

private fun Book.lastReadDate(): String? {
    val timestamp = lastReadTime ?: return null
    return runCatching {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(timestamp))
    }.getOrNull()
}

private fun Boolean?.orFalse(): Boolean = this ?: false

package app.booklore.android.feature.reader

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.booklore.android.R
import app.booklore.android.core.model.ANNOTATION_COLORS
import app.booklore.android.core.model.ANNOTATION_DEFAULT_COLOR
import app.booklore.android.core.model.ANNOTATION_STYLE_HIGHLIGHT
import app.booklore.android.core.model.ANNOTATION_STYLE_UNDERLINE
import app.booklore.android.core.model.Annotation
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.BookFile
import app.booklore.android.core.model.CreateAnnotationRequest
import app.booklore.android.core.model.EpubReaderSettings
import app.booklore.android.core.model.EpubReaderTheme
import app.booklore.android.core.model.EpubPosition
import app.booklore.android.core.model.FontOption
import app.booklore.android.core.model.PendingAnnotationUpdate
import app.booklore.android.core.model.ThemeColorOption
import app.booklore.android.core.model.UpdateAnnotationRequest
import app.booklore.android.core.offline.OfflineBookStore
import app.booklore.android.core.model.displayTitle
import app.booklore.android.core.model.supportsEpubReader
import app.booklore.android.core.model.toArgbColorOrNull
import app.booklore.android.core.database.AnnotationEntity
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.network.ApiException
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.network.NetworkException
import app.booklore.android.core.security.SessionStore
import app.booklore.android.sync.PendingSyncQueue
import app.booklore.android.ui.theme.BookLoreTheme
import app.booklore.android.ui.theme.bookLoreColorScheme
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.Selection
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme as ReadiumTheme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpRequest
import org.readium.r2.shared.util.mediatype.MediaType
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

class EpubReaderActivity : FragmentActivity() {
    private val args by lazy { EpubReaderArgs.fromIntent(intent) }
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val viewModel by viewModels<EpubReaderViewModel> {
        EpubReaderViewModel.Factory(application, args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // A restored Readium fragment has no publication until the protected file is reopened.
        super.onCreate(null)
        if (args == null) {
            finish()
            return
        }

        setContent {
            val displaySettings = sessionStore.readDisplaySettings()
            BookLoreTheme(displaySettings = displaySettings) {
                EpubReaderScreen(
                    title = args!!.title,
                    viewModel = viewModel,
                    themeColor = displaySettings.themeColor,
                    onClose = ::finishReader,
                )
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishReader()
            }
        })
    }

    override fun onPause() {
        if (args != null) viewModel.flushProgress()
        super.onPause()
    }

    private fun finishReader() {
        viewModel.flushProgress()
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        fun intent(context: Context, book: Book): Intent? {
            val bookFile = book.primaryFile ?: return null
            val bookFileId = bookFile.id ?: return null
            if (!book.supportsEpubReader()) return null
            return Intent(context, EpubReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, book.id)
                .putExtra(EXTRA_BOOK_FILE_ID, bookFileId)
                .putExtra(EXTRA_TITLE, book.displayTitle())
                .putExtra(EXTRA_BOOK_TYPE, bookFile.bookType ?: "EPUB")
                .putExtra(EXTRA_CFI, book.epubProgress?.cfi)
                .putExtra(EXTRA_HREF, book.epubProgress?.href)
                .putExtra(EXTRA_PROGRESS, book.epubProgress?.percentage ?: 0f)
        }

        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_BOOK_FILE_ID = "book_file_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BOOK_TYPE = "book_type"
        private const val EXTRA_CFI = "cfi"
        private const val EXTRA_HREF = "href"
        private const val EXTRA_PROGRESS = "progress"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Suppress("DEPRECATION")
@Composable
private fun EpubReaderScreen(
    title: String,
    viewModel: EpubReaderViewModel,
    themeColor: ThemeColorOption,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    val footerState by viewModel.footerState.collectAsStateWithLifecycle()
    val annotations by viewModel.annotations.collectAsStateWithLifecycle()
    val systemBars = rememberSystemBarsInsets()
    var activePanel by remember { mutableStateOf<ReaderPanel?>(null) }
    var navigator by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    // Pending text selection waiting for the annotation editor.
    var pendingSelection by remember { mutableStateOf<Selection?>(null) }
    // Annotation whose detail sheet is open.
    var activeAnnotation by remember { mutableStateOf<Annotation?>(null) }
    val scope = rememberCoroutineScope()
    val systemDark = isSystemInDarkTheme()
    val resolvedTheme = readerSettings.theme.resolve(systemDark)
    val chrome = readerChrome(resolvedTheme)

    // Reader panels follow the reader theme (not the system theme) so sheets
    // always match the reading chrome.
    MaterialTheme(
        colorScheme = bookLoreColorScheme(themeColor, dark = resolvedTheme == EpubReaderTheme.DARK),
        typography = MaterialTheme.typography,
    ) {
        when (val readerState = state) {
            EpubReaderUiState.Loading -> ReaderLoadingState(chrome)
            is EpubReaderUiState.Ready -> ReaderShell(
                title = title,
                chrome = chrome,
                controlsVisible = controlsVisible,
                onControlsVisibleChanged = { controlsVisible = it },
                onClose = onClose,
                onPrevious = {
                    navigator?.goBackward(true)
                },
                onNext = {
                    navigator?.goForward(true)
                },
                // Tap zones turn pages; Readium's WebView already handles swipe paging.
                enablePageTurns = true,
                enableSwipeTurns = false,
                // Keep the WebView bounds constant while bars toggle to avoid reflow jitter.
                stableContentLayout = true,
                footer = {
                    EpubReaderFooter(
                        state = footerState,
                        chrome = chrome,
                        navigationBarBottom = systemBars.navigationBarBottom,
                    )
                },
                bottomBar = {
                    EpubReaderControls(
                        navigatorAvailable = navigator != null,
                        onOpenContents = { activePanel = ReaderPanel.Contents },
                        onOpenAppearance = { activePanel = ReaderPanel.Appearance },
                        onOpenAnnotations = { activePanel = ReaderPanel.Annotations },
                    )
                },
            ) { readerModifier ->
                ReadiumEpubHost(
                    publication = readerState.publication,
                    initialLocator = readerState.initialLocator,
                    readerSettings = readerSettings,
                    resolvedTheme = resolvedTheme,
                    systemBars = systemBars,
                    annotations = annotations,
                    onHighlightRequested = {
                        scope.launch {
                            val selection = navigator?.currentSelection() ?: return@launch
                            pendingSelection = selection
                        }
                    },
                    onAnnotationActivated = { annotationId ->
                        activeAnnotation = annotations.firstOrNull { it.id == annotationId }
                    },
                    onLocatorChanged = viewModel::onLocatorChanged,
                    onPageChanged = viewModel::onPageInfoChanged,
                    onNavigatorAvailable = { navigator = it },
                    modifier = readerModifier,
                )
            }
            is EpubReaderUiState.Error -> EpubErrorState(
                error = readerState.error,
                onRetry = viewModel::retry,
                chrome = chrome,
            )
        }

        val readyState = state as? EpubReaderUiState.Ready
        if (readyState != null && activePanel != null) {
            ModalBottomSheet(onDismissRequest = { activePanel = null }) {
                when (activePanel) {
                    ReaderPanel.Contents -> ReaderContentsSheet(
                        items = remember(readyState.publication) { readyState.publication.tableOfContentsItems() },
                        onItemSelected = { item ->
                            navigator?.go(item.link, true)
                            activePanel = null
                        },
                    )
                    ReaderPanel.Appearance -> ReaderAppearanceSheet(
                        settings = readerSettings,
                        onThemeChanged = viewModel::setReaderTheme,
                        onFontScaleChanged = viewModel::setReaderFontScale,
                        onFontChanged = viewModel::setReaderFont,
                        onPageMarginsChanged = viewModel::setReaderPageMargins,
                    )
                    ReaderPanel.Annotations -> AnnotationsListSheet(
                        annotations = annotations,
                        onAnnotationSelected = { annotation ->
                            activePanel = null
                            val locator = annotation.cfi?.toReadiumLocator()
                            if (locator != null) navigator?.go(locator, true)
                            activeAnnotation = annotation
                        },
                    )
                    null -> Unit
                }
            }
        }

        // Annotation editor for a fresh text selection.
        val selection = pendingSelection
        if (selection != null) {
            AnnotationEditorSheet(
                selectedText = selection.locator.text.highlight.orEmpty(),
                onSave = { color, style, note ->
                    viewModel.createAnnotation(
                        locatorJson = selection.locator.toJsonString(),
                        text = selection.locator.text.highlight.orEmpty(),
                        chapterTitle = selection.locator.title?.takeIf(String::isNotBlank),
                        color = color,
                        style = style,
                        note = note,
                    )
                    navigator?.clearSelection()
                    pendingSelection = null
                },
                onDismiss = {
                    navigator?.clearSelection()
                    pendingSelection = null
                },
            )
        }

        // Annotation detail / edit / delete sheet.
        val annotation = activeAnnotation
        if (annotation != null) {
            AnnotationDetailSheet(
                annotation = annotation,
                onSave = { color, style, note ->
                    viewModel.updateAnnotation(
                        annotationId = annotation.id,
                        cfi = annotation.cfi,
                        request = UpdateAnnotationRequest(color = color, style = style, note = note),
                    )
                    activeAnnotation = null
                },
                onDelete = {
                    viewModel.deleteAnnotation(annotation.id, annotation.cfi)
                    activeAnnotation = null
                },
                onDismiss = { activeAnnotation = null },
            )
        }
    }
}

@Composable
private fun EpubReaderControls(
    navigatorAvailable: Boolean,
    onOpenContents: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAnnotations: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = onOpenContents, enabled = navigatorAvailable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = stringResource(R.string.content_description_reader_contents),
            )
        }
        IconButton(onClick = onOpenAnnotations) {
            Icon(
                imageVector = Icons.Filled.Notes,
                contentDescription = stringResource(R.string.content_description_reader_annotations),
            )
        }
        IconButton(onClick = onOpenAppearance) {
            Icon(
                imageVector = Icons.Filled.TextFields,
                contentDescription = stringResource(R.string.content_description_reader_appearance),
            )
        }
    }
}

@Composable
private fun EpubReaderFooter(
    state: EpubReaderFooterState,
    chrome: ReaderChrome,
    navigationBarBottom: Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(chrome.background)
            // Lift the footer above the gesture/nav area; the value is captured once
            // because system bars are hidden while the footer is visible.
            .padding(bottom = navigationBarBottom + 10.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.chapterTitle.orEmpty(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = chrome.content.copy(alpha = 0.60f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.totalPages > 0) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(
                    R.string.reader_page_progress,
                    (state.pageIndex + 1).coerceIn(1, state.totalPages),
                    state.totalPages,
                    state.progressPercent,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = chrome.content.copy(alpha = 0.60f),
            )
        }
    }
}

@Composable
private fun ReaderLoadingState(chrome: ReaderChrome) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chrome.background),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = chrome.content)
    }
}

@Composable
private fun EpubErrorState(
    error: EpubReaderError,
    onRetry: () -> Unit,
    chrome: ReaderChrome,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chrome.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = error.message(),
                style = MaterialTheme.typography.bodyLarge,
                color = chrome.content,
            )
            error.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = chrome.content.copy(alpha = 0.72f),
                )
            }
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@OptIn(ExperimentalReadiumApi::class)
@Composable
private fun ReadiumEpubHost(
    publication: Publication,
    initialLocator: Locator?,
    readerSettings: EpubReaderSettings,
    resolvedTheme: EpubReaderTheme,
    systemBars: SystemBarsInsets,
    annotations: List<Annotation>,
    onHighlightRequested: () -> Unit,
    onAnnotationActivated: (Long) -> Unit,
    onLocatorChanged: (Locator) -> Unit,
    onPageChanged: (pageIndex: Int, totalPages: Int, locator: Locator) -> Unit,
    onNavigatorAvailable: (EpubNavigatorFragment?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current as? EpubReaderActivity ?: return
    val fragmentManager = activity.supportFragmentManager
    val containerId = remember { View.generateViewId() }
    val latestOnLocatorChanged by rememberUpdatedState(onLocatorChanged)
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)
    val latestOnNavigatorAvailable by rememberUpdatedState(onNavigatorAvailable)
    val latestOnHighlightRequested by rememberUpdatedState(onHighlightRequested)
    val latestOnAnnotationActivated by rememberUpdatedState(onAnnotationActivated)
    val fragmentFactory = remember(publication, initialLocator, readerSettings, resolvedTheme) {
        EpubNavigatorFactory(publication).createFragmentFactory(
            initialLocator = initialLocator,
            initialPreferences = readerSettings.toReadiumPreferences(resolvedTheme),
            configuration = EpubNavigatorFragment.Configuration(
                // Insets are applied by the Compose wrapper below; Readium's own
                // insets handling relies on rootWindowInsets which is not ready
                // on first layout, causing content to overlap the status bar.
                shouldApplyInsetsPadding = false,
                // Adds a "highlight" item to the text selection menu.
                selectionActionModeCallback = object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                        menu.add(
                            Menu.NONE,
                            MENU_ITEM_ID_ANNOTATE,
                            Menu.NONE,
                            activity.getString(R.string.annotation_menu_highlight),
                        )
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

                    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                        if (item.itemId == MENU_ITEM_ID_ANNOTATE) {
                            mode.finish()
                            latestOnHighlightRequested()
                            return true
                        }
                        return false
                    }

                    override fun onDestroyActionMode(mode: ActionMode) {}
                },
            ),
            paginationListener = object : EpubNavigatorFragment.PaginationListener {
                override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                    latestOnLocatorChanged(locator)
                    latestOnPageChanged(pageIndex, totalPages, locator)
                }
            },
        )
    }

    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply { id = containerId }
        },
        // Fixed paddings derived from insets captured once, so the WebView bounds
        // never change when system bars hide/show — no EPUB re-pagination jitter.
        // Bottom also reserves room for the floating footer.
        modifier = modifier.padding(
            top = systemBars.statusBarTop + 16.dp,
            bottom = systemBars.navigationBarBottom + 44.dp,
        ),
    )
    LaunchedEffect(fragmentFactory, containerId) {
        if (fragmentManager.findFragmentById(containerId) == null && !fragmentManager.isStateSaved) {
            fragmentManager.fragmentFactory = fragmentFactory
            fragmentManager.commitNow {
                replace(containerId, EpubNavigatorFragment::class.java, null)
            }
        }
        val navigator = fragmentManager.findFragmentById(containerId) as? EpubNavigatorFragment
        navigator?.addDecorationListener(
            DECORATION_GROUP,
            object : DecorableNavigator.Listener {
                override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                    val annotationId = event.decoration.id.toLongOrNull() ?: return false
                    latestOnAnnotationActivated(annotationId)
                    return true
                }
            },
        )
        latestOnNavigatorAvailable(navigator)
    }
    LaunchedEffect(readerSettings, resolvedTheme, containerId) {
        (fragmentManager.findFragmentById(containerId) as? EpubNavigatorFragment)
            ?.submitPreferences(readerSettings.toReadiumPreferences(resolvedTheme))
    }
    // Re-applies highlight decorations whenever the annotation list changes.
    LaunchedEffect(annotations, containerId) {
        val navigator = fragmentManager.findFragmentById(containerId) as? EpubNavigatorFragment
            ?: return@LaunchedEffect
        navigator.applyDecorations(
            annotations.mapNotNull { it.toDecoration() },
            DECORATION_GROUP,
        )
    }
    DisposableEffect(publication) {
        onDispose {
            latestOnNavigatorAvailable(null)
            // The ViewModel owns and closes the publication.
        }
    }
}

private enum class ReaderPanel {
    Contents,
    Appearance,
    Annotations,
}

/** Group id used for highlight decorations rendered by Readium. */
private const val DECORATION_GROUP = "booklore-annotations"
private const val MENU_ITEM_ID_ANNOTATE = 1001

private data class EpubTableOfContentsItem(
    val link: Link,
    val depth: Int,
)

/** Row of selectable color dots used by the annotation editor. */
@Composable
private fun AnnotationColorRow(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ANNOTATION_COLORS.forEach { color ->
            val tint = color.toArgbColorOrNull()?.let { Color(it) } ?: Color.Yellow
            val selected = color == selectedColor
            Box(
                modifier = Modifier
                    .size(if (selected) 36.dp else 32.dp)
                    .clip(CircleShape)
                    .background(tint)
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        },
                        shape = CircleShape,
                    )
                    .clickable { onColorSelected(color) },
            )
        }
    }
}

/** Editor sheet shown right after the user selects text and taps "highlight". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnotationEditorSheet(
    selectedText: String,
    onSave: (color: String, style: String, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember { mutableStateOf(ANNOTATION_DEFAULT_COLOR) }
    var style by remember { mutableStateOf(ANNOTATION_STYLE_HIGHLIGHT) }
    var note by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.annotation_editor_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = selectedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        (color.toArgbColorOrNull()?.let { Color(it) } ?: Color.Yellow).copy(alpha = 0.3f),
                        MaterialTheme.shapes.small,
                    )
                    .padding(12.dp),
            )
            Text(stringResource(R.string.annotation_color), style = MaterialTheme.typography.titleMedium)
            AnnotationColorRow(selectedColor = color, onColorSelected = { color = it })
            Text(stringResource(R.string.annotation_style), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = style == ANNOTATION_STYLE_HIGHLIGHT,
                    onClick = { style = ANNOTATION_STYLE_HIGHLIGHT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.annotation_style_highlight)) },
                )
                SegmentedButton(
                    selected = style == ANNOTATION_STYLE_UNDERLINE,
                    onClick = { style = ANNOTATION_STYLE_UNDERLINE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.annotation_style_underline)) },
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.annotation_note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Button(
                onClick = {
                    onSave(color, style, note.takeIf(String::isNotBlank))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.annotation_save))
            }
        }
    }
}

/** Detail sheet for an existing annotation: edit color/style/note or delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnotationDetailSheet(
    annotation: Annotation,
    onSave: (color: String, style: String, note: String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember(annotation.id) { mutableStateOf(annotation.color ?: ANNOTATION_DEFAULT_COLOR) }
    var style by remember(annotation.id) { mutableStateOf(annotation.style ?: ANNOTATION_STYLE_HIGHLIGHT) }
    var note by remember(annotation.id) { mutableStateOf(annotation.note.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.annotation_detail_title), style = MaterialTheme.typography.titleLarge)
            Text(
                text = annotation.text.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        (color.toArgbColorOrNull()?.let { Color(it) } ?: Color.Yellow).copy(alpha = 0.3f),
                        MaterialTheme.shapes.small,
                    )
                    .padding(12.dp),
            )
            annotation.chapterTitle?.takeIf(String::isNotBlank)?.let { chapter ->
                Text(
                    text = chapter,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(stringResource(R.string.annotation_color), style = MaterialTheme.typography.titleMedium)
            AnnotationColorRow(selectedColor = color, onColorSelected = { color = it })
            Text(stringResource(R.string.annotation_style), style = MaterialTheme.typography.titleMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = style == ANNOTATION_STYLE_HIGHLIGHT,
                    onClick = { style = ANNOTATION_STYLE_HIGHLIGHT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(stringResource(R.string.annotation_style_highlight)) },
                )
                SegmentedButton(
                    selected = style == ANNOTATION_STYLE_UNDERLINE,
                    onClick = { style = ANNOTATION_STYLE_UNDERLINE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(stringResource(R.string.annotation_style_underline)) },
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.annotation_note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onSave(color, style, note.takeIf(String::isNotBlank)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.annotation_save))
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.annotation_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

/** List of all annotations for the current book; tapping one jumps to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnnotationsListSheet(
    annotations: List<Annotation>,
    onAnnotationSelected: (Annotation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.annotation_list_title),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider()
        if (annotations.isEmpty()) {
            Text(
                text = stringResource(R.string.annotation_list_empty),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(annotations) { _, annotation ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAnnotationSelected(annotation) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        annotation.color?.toArgbColorOrNull()?.let { Color(it) } ?: Color.Yellow,
                                    ),
                            )
                        },
                        headlineContent = {
                            Text(
                                text = annotation.text.orEmpty(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = annotation.note?.takeIf(String::isNotBlank)?.let { note ->
                            {
                                Text(
                                    text = note,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/** Maps a synced annotation back to a Readium highlight decoration. */
private fun Annotation.toDecoration(): Decoration? {
    val locator = cfi?.toReadiumLocator() ?: return null
    val tint = color?.toArgbColorOrNull()
        ?: ANNOTATION_DEFAULT_COLOR.toArgbColorOrNull()
        ?: return null
    val decorationStyle = if (style == ANNOTATION_STYLE_UNDERLINE) {
        Decoration.Style.Underline(tint)
    } else {
        Decoration.Style.Highlight(tint)
    }
    return Decoration(id.toString(), locator, decorationStyle)
}

/** Parses the stored locator JSON (in the annotation's cfi field). */
private fun String.toReadiumLocator(): Locator? = runCatching {
    Locator.fromJSON(JSONObject(this))
}.getOrNull()

private fun Locator.toJsonString(): String = toJSON().toString()

private fun Annotation.toEntity(): AnnotationEntity = AnnotationEntity(
    id = id,
    bookId = bookId,
    cfi = cfi,
    text = text,
    color = color,
    style = style,
    note = note,
    chapterTitle = chapterTitle,
    updatedAt = updatedAt,
)

private fun AnnotationEntity.toModel(): Annotation = Annotation(
    id = id,
    bookId = bookId,
    cfi = cfi,
    text = text,
    color = color,
    style = style,
    note = note,
    chapterTitle = chapterTitle,
    updatedAt = updatedAt,
)

@Composable
private fun ReaderContentsSheet(
    items: List<EpubTableOfContentsItem>,
    onItemSelected: (EpubTableOfContentsItem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Text(
            text = stringResource(R.string.reader_contents),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        HorizontalDivider()
        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.reader_contents_empty),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 520.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                itemsIndexed(items) { index, item ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemSelected(item) }
                            .padding(start = (16 + item.depth * 16).dp),
                        headlineContent = {
                            Text(
                                text = item.link.title?.takeIf(String::isNotBlank)
                                    ?: stringResource(R.string.reader_chapter, index + 1),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderAppearanceSheet(
    settings: EpubReaderSettings,
    onThemeChanged: (EpubReaderTheme) -> Unit,
    onFontScaleChanged: (Float) -> Unit,
    onFontChanged: (FontOption) -> Unit,
    onPageMarginsChanged: (Float) -> Unit,
) {
    val fontPercent = (settings.fontScale * 100).roundToInt()
    val marginsPercent = (settings.pageMargins / READER_PAGE_MARGINS_MAX * 100).roundToInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.reader_appearance), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.reader_theme), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EpubReaderTheme.entries.forEachIndexed { index, theme ->
                SegmentedButton(
                    selected = settings.theme == theme,
                    onClick = { onThemeChanged(theme) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = EpubReaderTheme.entries.size,
                    ),
                    label = { Text(readerThemeLabel(theme), maxLines = 1) },
                )
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reader_font_size),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.font_size_value, fontPercent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.fontScale,
            onValueChange = onFontScaleChanged,
            valueRange = READER_FONT_SCALE_MIN..READER_FONT_SCALE_MAX,
            steps = 5,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.font_size_small),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.font_size_large),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider()
        Text(stringResource(R.string.reader_font), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            FontOption.entries.forEachIndexed { index, font ->
                SegmentedButton(
                    selected = settings.font == font,
                    onClick = { onFontChanged(font) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = FontOption.entries.size,
                    ),
                    label = { Text(readerFontLabel(font), maxLines = 1) },
                )
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.reader_margins),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.reader_margin_value, marginsPercent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.pageMargins,
            onValueChange = onPageMarginsChanged,
            valueRange = READER_PAGE_MARGINS_MIN..READER_PAGE_MARGINS_MAX,
            steps = 5,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.reader_margins_narrow),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.reader_margins_wide),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Publication.tableOfContentsItems(): List<EpubTableOfContentsItem> = buildList {
    fun append(link: Link, depth: Int) {
        add(EpubTableOfContentsItem(link = link, depth = depth))
        link.children.forEach { child -> append(child, depth + 1) }
    }
    tableOfContents.forEach { link -> append(link, depth = 0) }
}

@Composable
private fun readerThemeLabel(theme: EpubReaderTheme): String = when (theme) {
    EpubReaderTheme.SYSTEM -> stringResource(R.string.reader_theme_system)
    EpubReaderTheme.LIGHT -> stringResource(R.string.reader_theme_light)
    EpubReaderTheme.SEPIA -> stringResource(R.string.reader_theme_sepia)
    EpubReaderTheme.DARK -> stringResource(R.string.reader_theme_dark)
}

@Composable
private fun readerFontLabel(font: FontOption): String = when (font) {
    FontOption.SANS -> stringResource(R.string.font_sans)
    FontOption.SERIF -> stringResource(R.string.font_serif)
    FontOption.MONOSPACE -> stringResource(R.string.font_monospace)
}

/** SYSTEM maps to LIGHT/DARK according to the current system theme. */
private fun EpubReaderTheme.resolve(systemDark: Boolean): EpubReaderTheme = when (this) {
    EpubReaderTheme.SYSTEM -> if (systemDark) EpubReaderTheme.DARK else EpubReaderTheme.LIGHT
    EpubReaderTheme.LIGHT, EpubReaderTheme.SEPIA, EpubReaderTheme.DARK -> this
}

@OptIn(ExperimentalReadiumApi::class)
private fun EpubReaderSettings.toReadiumPreferences(resolvedTheme: EpubReaderTheme): EpubPreferences = EpubPreferences(
    theme = when (resolvedTheme) {
        EpubReaderTheme.LIGHT, EpubReaderTheme.SYSTEM -> ReadiumTheme.LIGHT
        EpubReaderTheme.SEPIA -> ReadiumTheme.SEPIA
        EpubReaderTheme.DARK -> ReadiumTheme.DARK
    },
    // Readium expects a scale factor; it renders the CSS value as "factor * 100%" internally.
    fontSize = fontScale.coerceIn(READER_FONT_SCALE_MIN, READER_FONT_SCALE_MAX).toDouble(),
    typeScale = fontScale.coerceIn(READER_FONT_SCALE_MIN, READER_FONT_SCALE_MAX).toDouble(),
    fontFamily = when (font) {
        FontOption.SANS -> FontFamily.SANS_SERIF
        FontOption.SERIF -> FontFamily.SERIF
        FontOption.MONOSPACE -> FontFamily.MONOSPACE
    },
    pageMargins = pageMargins.coerceIn(READER_PAGE_MARGINS_MIN, READER_PAGE_MARGINS_MAX).toDouble(),
)

private fun readerChrome(resolvedTheme: EpubReaderTheme): ReaderChrome = when (resolvedTheme) {
    EpubReaderTheme.SYSTEM, EpubReaderTheme.LIGHT -> ReaderChrome(Color.White, Color(0xFF1A1C1A))
    EpubReaderTheme.SEPIA -> ReaderChrome(Color(0xFFFFF8E8), Color(0xFF342B1D))
    EpubReaderTheme.DARK -> ReaderChrome(Color(0xFF1B1B1B), Color(0xFFE5E2DE))
}

private class EpubReaderViewModel(
    application: Application,
    private val args: EpubReaderArgs,
) : AndroidViewModel(application) {
    private val sessionStore = SessionStore(application)
    private val api = BookLoreApi(sessionStore)
    private val database = BookLoreDatabase.get(application)
    private val pendingSyncQueue = PendingSyncQueue(application, database)
    private val offlineBookStore = OfflineBookStore(application)
    private val readerDirectory = File(application.filesDir, "reader-cache")
    private val cacheFile = File(readerDirectory, "${args.bookId}-${args.bookFileId}.epub")
    private val offlineFile = offlineBookStore.fileFor(
        Book(
            id = args.bookId,
            primaryFile = BookFile(id = args.bookFileId, bookType = args.bookType, extension = "epub"),
        ),
    )
    private val sessionTracker = ReadingSessionTracker { report ->
        try {
            api.recordReadingSession(report)
        } catch (error: Exception) {
            // Keep the report for the background sync worker; rethrow so the tracker logs the miss.
            runCatching { pendingSyncQueue.enqueueReadingSession(report) }
            throw error
        }
    }
    private val httpClient = DefaultHttpClient(
        callback = object : DefaultHttpClient.Callback {
            override suspend fun onStartRequest(request: HttpRequest) = Try.success(
                request.copy {
                    sessionStore.read()?.accessToken?.let { token ->
                        setHeader("Authorization", "Bearer $token")
                    }
                },
            )
        },
    )
    private val assetRetriever = AssetRetriever(application.contentResolver, httpClient)
    private val publicationParser = DefaultPublicationParser(application, httpClient, assetRetriever, null)
    private val publicationOpener = PublicationOpener(publicationParser)
    private val _state = MutableStateFlow<EpubReaderUiState>(EpubReaderUiState.Loading)
    val state: StateFlow<EpubReaderUiState> = _state.asStateFlow()
    private val _readerSettings = MutableStateFlow(sessionStore.readEpubReaderSettings())
    val readerSettings: StateFlow<EpubReaderSettings> = _readerSettings.asStateFlow()
    private val _footerState = MutableStateFlow(EpubReaderFooterState())
    val footerState: StateFlow<EpubReaderFooterState> = _footerState.asStateFlow()
    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    private var publication: Publication? = null
    private var latestPosition: EpubPosition? = null
    private var syncJob: Job? = null
    private var cacheJob: Job? = null

    init {
        clearExpiredReaderFiles()
        openPublication()
        loadAnnotations()
    }

    fun retry() = openPublication()

    fun setReaderTheme(theme: EpubReaderTheme) {
        updateReaderSettings { it.copy(theme = theme) }
    }

    fun setReaderFontScale(fontScale: Float) {
        updateReaderSettings {
            it.copy(fontScale = fontScale.coerceIn(READER_FONT_SCALE_MIN, READER_FONT_SCALE_MAX))
        }
    }

    fun setReaderFont(font: FontOption) {
        updateReaderSettings { it.copy(font = font) }
    }

    fun setReaderPageMargins(pageMargins: Float) {
        updateReaderSettings {
            it.copy(pageMargins = pageMargins.coerceIn(READER_PAGE_MARGINS_MIN, READER_PAGE_MARGINS_MAX))
        }
    }

    fun onPageInfoChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
        _footerState.value = EpubReaderFooterState(
            chapterTitle = locator.title?.takeIf(String::isNotBlank),
            pageIndex = pageIndex,
            totalPages = totalPages,
            progressPercent = ((locator.locations.totalProgression ?: locator.locations.progression)
                ?.times(100)
                ?.roundToInt()
                ?: 0)
                .coerceIn(0, 100),
        )
    }

    fun onLocatorChanged(locator: Locator) {
        val previous = latestPosition ?: initialPosition()
        val position = EpubPosition(
            bookId = args.bookId,
            bookFileId = args.bookFileId,
            cfi = locator.locations.fragments.firstOrNull(),
            href = locator.href.toString(),
            progression = locator.locations.progression
                ?.toFloat()
                ?.coerceIn(0f, 1f)
                ?: previous.progression,
            progressPercent = ((locator.locations.totalProgression ?: locator.locations.progression)
                ?.times(100)
                ?.toFloat()
                ?: previous.progressPercent)
                .coerceIn(0f, 100f),
        )
        latestPosition = position
        sessionStore.writeEpubPosition(position)
        sessionTracker.trackActivity(
            bookId = args.bookId,
            bookType = args.bookType.uppercase(),
            location = position.cfi ?: position.href,
            progressPercent = position.progressPercent,
        )
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(PROGRESS_SYNC_DELAY_MILLIS)
            syncProgress(position)
        }
    }

    fun flushProgress() {
        sessionTracker.endSession()
        val position = latestPosition ?: return
        syncJob?.cancel()
        syncJob = viewModelScope.launch { syncProgress(position) }
    }

    private fun openPublication(preferStreaming: Boolean = true) {
        syncJob?.cancel()
        publication?.close()
        publication = null
        _state.value = EpubReaderUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            // Reuse the previously downloaded copy when it is still readable.
            val fromOffline = offlineFile?.exists() == true && offlineFile.hasZipSignature()
            val fromCache = !fromOffline && cacheFile.exists() && cacheFile.hasZipSignature()
            var usingStreaming = false
            try {
                val asset = when {
                    fromOffline -> {
                        offlineFile!!.setLastModified(System.currentTimeMillis())
                        assetRetriever.retrieve(offlineFile)
                    }
                    fromCache -> {
                        cacheFile.setLastModified(System.currentTimeMillis())
                        assetRetriever.retrieve(cacheFile)
                    }
                    preferStreaming -> {
                        // Verify/refresh the session with a one-byte range request before handing
                        // the remote archive to Readium. Any failure falls back to a full download.
                        api.bookContentLength(args.bookId, args.bookType)
                        val contentUrl = api.bookContentUrl(args.bookId, args.bookType)
                            ?: throw ApiException(401, "No active session")
                        val absoluteContentUrl = AbsoluteUrl(contentUrl)
                            ?: throw EpubOpenException("Invalid book content URL")
                        usingStreaming = true
                        assetRetriever.retrieve(absoluteContentUrl, MediaType.EPUB)
                    }
                    else -> {
                        api.downloadEpub(args.bookId, args.bookType, cacheFile)
                        if (!cacheFile.hasZipSignature()) throw InvalidEpubArchiveException()
                        cacheFile.setLastModified(System.currentTimeMillis())
                        assetRetriever.retrieve(cacheFile)
                    }
                }
                    .getOrElse { throw EpubOpenException(it.message) }
                val openedPublication = publicationOpener.open(asset, allowUserInteraction = false)
                    .getOrElse {
                        asset.close()
                        throw EpubOpenException(it.message)
                    }
                publication = openedPublication
                val initialPosition = initialPosition()
                latestPosition = initialPosition
                _state.value = EpubReaderUiState.Ready(
                    publication = openedPublication,
                    initialLocator = initialPosition.toLocator(),
                )
                if (usingStreaming) cacheBookInBackground()
            } catch (error: Exception) {
                publication?.close()
                publication = null
                if (fromOffline) {
                    offlineFile?.delete()
                    openPublication()
                    return@launch
                }
                if (fromCache) {
                    // The cached copy turned out unreadable — drop it and download fresh once.
                    cacheFile.delete()
                    openPublication()
                    return@launch
                }
                if (preferStreaming) {
                    // Range support and remote EPUB parsing vary by server. Fall back to the
                    // proven full-download reader path before surfacing an error to the user.
                    openPublication(preferStreaming = false)
                    return@launch
                }
                if (error !is NetworkException && error !is ApiException) {
                    cacheFile.delete()
                }
                _state.value = EpubReaderUiState.Error(error.toReaderError())
            }
        }
    }

    private fun cacheBookInBackground() {
        if (cacheJob?.isActive == true) return
        cacheJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                api.downloadEpub(args.bookId, args.bookType, cacheFile)
                cacheFile.setLastModified(System.currentTimeMillis())
            }
        }
    }

    private fun initialPosition(): EpubPosition =
        sessionStore.readEpubPosition(args.bookId, args.bookFileId) ?: EpubPosition(
            bookId = args.bookId,
            bookFileId = args.bookFileId,
            cfi = args.cfi,
            href = args.href,
            progressPercent = args.progressPercent,
        )

    private suspend fun syncProgress(position: EpubPosition) {
        try {
            api.updateEpubProgress(position)
        } catch (_: Exception) {
            // Queue for the background worker; the encrypted local locator remains as backup.
            runCatching { pendingSyncQueue.enqueueEpubProgress(position) }
        }
    }

    /**
     * Loads annotations for the book: Room cache first (fast), then the server.
     * Offline, annotations queued for creation are shown with temporary ids.
     */
    private fun loadAnnotations() {
        viewModelScope.launch {
            refreshLocalAnnotations()
            try {
                val remote = api.annotations(args.bookId)
                database.annotationDao().replaceBook(args.bookId, remote.map { it.toEntity() })
                refreshLocalAnnotations()
            } catch (_: Exception) {
                // Offline: the cached list (plus queued creates) is already displayed.
            }
        }
    }

    private suspend fun refreshLocalAnnotations() {
        val cached = database.annotationDao().forBook(args.bookId).map { it.toModel() }
        val queued = pendingSyncQueue.queuedAnnotationCreates(args.bookId)
        val localOnly = queued.mapIndexed { index, request ->
            Annotation(
                id = LOCAL_ANNOTATION_ID_BASE - index,
                bookId = request.bookId,
                cfi = request.cfi,
                text = request.text,
                color = request.color,
                style = request.style,
                note = request.note,
                chapterTitle = request.chapterTitle,
            )
        }
        _annotations.value = cached + localOnly
    }

    fun createAnnotation(
        locatorJson: String,
        text: String,
        chapterTitle: String?,
        color: String,
        style: String,
        note: String?,
    ) {
        viewModelScope.launch {
            val request = CreateAnnotationRequest(
                bookId = args.bookId,
                cfi = locatorJson,
                text = text,
                color = color,
                style = style,
                note = note,
                chapterTitle = chapterTitle,
            )
            try {
                val created = api.createAnnotation(request)
                database.annotationDao().insertAll(listOf(created.toEntity()))
            } catch (_: Exception) {
                // Offline: queue for the sync worker and show it locally with a temp id.
                runCatching { pendingSyncQueue.enqueueAnnotationCreate(request) }
            }
            refreshLocalAnnotations()
        }
    }

    fun updateAnnotation(annotationId: Long, cfi: String?, request: UpdateAnnotationRequest) {
        viewModelScope.launch {
            if (annotationId > 0) {
                try {
                    val updated = api.updateAnnotation(annotationId, request)
                    database.annotationDao().insertAll(listOf(updated.toEntity()))
                } catch (_: Exception) {
                    runCatching { pendingSyncQueue.enqueueAnnotationUpdate(PendingAnnotationUpdate(annotationId, request)) }
                }
            } else if (cfi != null) {
                // Local-only annotation: overwrite the queued create payload
                // (same dedupe key derived from bookId + locator).
                runCatching {
                    pendingSyncQueue.enqueueAnnotationCreate(
                        CreateAnnotationRequest(
                            bookId = args.bookId,
                            cfi = cfi,
                            text = _annotations.value.firstOrNull { it.id == annotationId }?.text.orEmpty(),
                            color = request.color,
                            style = request.style,
                            note = request.note,
                            chapterTitle = _annotations.value.firstOrNull { it.id == annotationId }?.chapterTitle,
                        ),
                    )
                }
            }
            refreshLocalAnnotations()
        }
    }

    fun deleteAnnotation(annotationId: Long, cfi: String?) {
        viewModelScope.launch {
            if (annotationId > 0) {
                try {
                    api.deleteAnnotation(annotationId)
                } catch (_: Exception) {
                    runCatching { pendingSyncQueue.enqueueAnnotationDelete(annotationId) }
                }
                runCatching { database.annotationDao().deleteById(annotationId) }
            } else if (cfi != null) {
                runCatching { pendingSyncQueue.removeQueuedAnnotationCreate(args.bookId, cfi) }
            }
            refreshLocalAnnotations()
        }
    }

    private fun updateReaderSettings(transform: (EpubReaderSettings) -> EpubReaderSettings) {
        val settings = transform(_readerSettings.value)
        sessionStore.writeEpubReaderSettings(settings)
        _readerSettings.value = settings
    }

    private fun clearExpiredReaderFiles() {
        val files = readerDirectory.listFiles()?.filter(File::isFile) ?: return
        val now = System.currentTimeMillis()
        // Evict copies untouched beyond the retention window, then enforce the
        // overall size cap by removing the least recently read files first.
        files.filter { now - it.lastModified() > READER_CACHE_RETENTION_MILLIS }
            .forEach(File::delete)
        val remaining = readerDirectory.listFiles()?.filter(File::isFile) ?: return
        var totalSize = remaining.sumOf(File::length)
        if (totalSize <= READER_CACHE_MAX_BYTES) return
        for (file in remaining.sortedBy(File::lastModified)) {
            if (totalSize <= READER_CACHE_MAX_BYTES) break
            val size = file.length()
            if (file.delete()) totalSize -= size
        }
    }

    override fun onCleared() {
        sessionTracker.endSession()
        cacheJob?.cancel()
        publication?.close()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val args: EpubReaderArgs?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            requireNotNull(args) { "Missing EPUB reader arguments" }
            return EpubReaderViewModel(application, args) as T
        }
    }

    private companion object {
        const val PROGRESS_SYNC_DELAY_MILLIS = 1_500L
        const val READER_CACHE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1_000L
        const val READER_CACHE_MAX_BYTES = 512L * 1024 * 1024

        /** Local-only annotations (queued creates) use negative ids. */
        const val LOCAL_ANNOTATION_ID_BASE = -1000L
    }
}

private data class EpubReaderFooterState(
    val chapterTitle: String? = null,
    val pageIndex: Int = 0,
    val totalPages: Int = 0,
    val progressPercent: Int = 0,
)

private data class SystemBarsInsets(
    val statusBarTop: Dp = 0.dp,
    val navigationBarBottom: Dp = 0.dp,
)

/**
 * Captures the system bar insets exactly once (first non-zero dispatch).
 * Live insets drop to zero once the bars are hidden in immersive mode, which
 * would resize the reader WebView and re-paginate the EPUB content.
 */
@Composable
private fun rememberSystemBarsInsets(): SystemBarsInsets {
    val density = LocalDensity.current
    var statusBarTopPx by remember { mutableIntStateOf(0) }
    var navigationBarBottomPx by remember { mutableIntStateOf(0) }
    val currentStatusBarTop = WindowInsets.statusBars.getTop(density)
    val currentNavigationBarBottom = WindowInsets.navigationBars.getBottom(density)
    if (statusBarTopPx == 0 && currentStatusBarTop > 0) statusBarTopPx = currentStatusBarTop
    if (navigationBarBottomPx == 0 && currentNavigationBarBottom > 0) {
        navigationBarBottomPx = currentNavigationBarBottom
    }
    return SystemBarsInsets(
        statusBarTop = with(density) { statusBarTopPx.toDp() },
        navigationBarBottom = with(density) { navigationBarBottomPx.toDp() },
    )
}

private data class EpubReaderArgs(
    val bookId: Long,
    val bookFileId: Long,
    val title: String,
    val bookType: String,
    val cfi: String?,
    val href: String?,
    val progressPercent: Float,
) {
    companion object {
        fun fromIntent(intent: Intent): EpubReaderArgs? {
            val bookId = intent.getLongExtra("book_id", -1L)
            val bookFileId = intent.getLongExtra("book_file_id", -1L)
            if (bookId < 0 || bookFileId < 0) return null
            return EpubReaderArgs(
                bookId = bookId,
                bookFileId = bookFileId,
                title = intent.getStringExtra("title").orEmpty(),
                bookType = intent.getStringExtra("book_type") ?: "EPUB",
                cfi = intent.getStringExtra("cfi"),
                href = intent.getStringExtra("href"),
                progressPercent = intent.getFloatExtra("progress", 0f).coerceIn(0f, 100f),
            )
        }
    }
}

private fun EpubPosition.toLocator(): Locator? {
    val locationHref = href?.let(Url::invoke) ?: return null
    return Locator(
        href = locationHref,
        mediaType = MediaType.XHTML,
        locations = Locator.Locations(
            fragments = cfi?.takeIf(String::isNotBlank)?.let(::listOf).orEmpty(),
            // The navigator resolves the page inside the chapter from the
            // within-chapter progression, so keep it when restoring.
            progression = progression?.toDouble()?.coerceIn(0.0, 1.0),
            totalProgression = (progressPercent / 100).coerceIn(0f, 1f).toDouble(),
        ),
    )
}

private sealed interface EpubReaderUiState {
    data object Loading : EpubReaderUiState
    data class Ready(val publication: Publication, val initialLocator: Locator?) : EpubReaderUiState
    data class Error(val error: EpubReaderError) : EpubReaderUiState
}

private data class EpubReaderError(
    val type: EpubErrorType,
    val detail: String? = null,
)

private enum class EpubErrorType {
    Unauthorized,
    Forbidden,
    NotFound,
    Network,
    InvalidArchive,
    Parse,
    Generic,
}

private fun Exception.toReaderError(): EpubReaderError = when (this) {
    is NetworkException -> EpubReaderError(EpubErrorType.Network)
    is ApiException -> when (statusCode) {
        401 -> EpubReaderError(EpubErrorType.Unauthorized)
        403 -> EpubReaderError(EpubErrorType.Forbidden)
        404 -> EpubReaderError(EpubErrorType.NotFound)
        else -> EpubReaderError(EpubErrorType.Generic, message.asReaderErrorDetail())
    }
    is InvalidEpubArchiveException -> EpubReaderError(EpubErrorType.InvalidArchive)
    is EpubOpenException -> EpubReaderError(EpubErrorType.Parse, message.asReaderErrorDetail())
    else -> EpubReaderError(EpubErrorType.Generic, message.asReaderErrorDetail())
}

@Composable
private fun EpubReaderError.message(): String = when (type) {
    EpubErrorType.Unauthorized -> stringResource(R.string.error_unauthorized)
    EpubErrorType.Forbidden -> stringResource(R.string.error_forbidden)
    EpubErrorType.NotFound -> stringResource(R.string.error_not_found)
    EpubErrorType.Network -> stringResource(R.string.error_network)
    EpubErrorType.InvalidArchive -> stringResource(R.string.error_epub_invalid_archive)
    EpubErrorType.Parse -> stringResource(R.string.error_epub_parse)
    EpubErrorType.Generic -> stringResource(R.string.error_generic)
}

private const val READER_FONT_SCALE_MIN = 0.80f
private const val READER_FONT_SCALE_MAX = 1.40f
private const val READER_PAGE_MARGINS_MIN = 0f
private const val READER_PAGE_MARGINS_MAX = 3f

private class InvalidEpubArchiveException : Exception()

private class EpubOpenException(message: String?) : Exception(message)

private fun File.hasZipSignature(): Boolean = runCatching {
    if (length() < 4L) return@runCatching false
    inputStream().buffered().use { input ->
        input.read() == 'P'.code && input.read() == 'K'.code
    }
}.getOrDefault(false)

private fun String?.asReaderErrorDetail(): String? = this
    ?.trim()
    ?.replace(Regex("[\\r\\n]+"), " ")
    ?.take(240)
    ?.takeIf(String::isNotBlank)

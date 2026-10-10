package app.booklore.android.feature.reader

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.booklore.android.R
import app.booklore.android.core.model.Book
import app.booklore.android.core.model.PdfPosition
import app.booklore.android.core.model.PdfReaderSettings
import app.booklore.android.core.model.PdfReadingMode
import app.booklore.android.core.model.BookFile
import app.booklore.android.core.offline.OfflineBookStore
import app.booklore.android.core.offline.isReadableBookFile
import app.booklore.android.core.model.displayTitle
import app.booklore.android.core.model.supportsPdfReader
import app.booklore.android.core.database.BookLoreDatabase
import app.booklore.android.core.network.ApiException
import app.booklore.android.core.network.BookLoreApi
import app.booklore.android.core.network.NetworkException
import app.booklore.android.core.security.SessionStore
import app.booklore.android.sync.PendingSyncQueue
import app.booklore.android.ui.theme.BookLoreTheme
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfReaderActivity : ComponentActivity() {
    private val args by lazy { PdfReaderArgs.fromIntent(intent) }
    private val sessionStore by lazy { SessionStore(applicationContext) }
    private val viewModel by viewModels<PdfReaderViewModel> {
        PdfReaderViewModel.Factory(application, args)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (args == null) {
            finish()
            return
        }

        setContent {
            BookLoreTheme(displaySettings = sessionStore.readDisplaySettings()) {
                PdfReaderScreen(
                    title = args!!.title,
                    viewModel = viewModel,
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
            if (!book.supportsPdfReader()) return null
            return Intent(context, PdfReaderActivity::class.java)
                .putExtra(EXTRA_BOOK_ID, book.id)
                .putExtra(EXTRA_BOOK_FILE_ID, bookFileId)
                .putExtra(EXTRA_TITLE, book.displayTitle())
                .putExtra(EXTRA_BOOK_TYPE, bookFile.bookType ?: "PDF")
                .putExtra(EXTRA_PAGE, book.pdfProgress?.page ?: 1)
                .putExtra(EXTRA_PROGRESS, book.pdfProgress?.percentage ?: 0f)
        }

        private const val EXTRA_BOOK_ID = "book_id"
        private const val EXTRA_BOOK_FILE_ID = "book_file_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_BOOK_TYPE = "book_type"
        private const val EXTRA_PAGE = "page"
        private const val EXTRA_PROGRESS = "progress"
    }
}

@Composable
private fun PdfReaderScreen(
    title: String,
    viewModel: PdfReaderViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val readerState = state) {
        PdfReaderUiState.Loading -> LoadingPdfState()
        is PdfReaderUiState.Ready -> PdfReaderContent(
            title = title,
            state = readerState,
            viewModel = viewModel,
            onClose = onClose,
        )
        is PdfReaderUiState.Error -> PdfErrorState(
            error = readerState.error,
            onRetry = viewModel::retry,
        )
    }
}

@Composable
private fun PdfReaderContent(
    title: String,
    state: PdfReaderUiState.Ready,
    viewModel: PdfReaderViewModel,
    onClose: () -> Unit,
) {
    val readerSettings by viewModel.readerSettings.collectAsStateWithLifecycle()
    var currentPage by remember(state.pageCount, state.initialPage) {
        mutableIntStateOf(state.initialPage)
    }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var isZoomed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentPage, state.pageCount) {
        viewModel.onPageSelected(currentPage, state.pageCount)
    }

    ReaderShell(
        title = title,
        chrome = ReaderChrome(
            background = MaterialTheme.colorScheme.surface,
            content = MaterialTheme.colorScheme.onSurface,
        ),
        controlsVisible = controlsVisible,
        onControlsVisibleChanged = { controlsVisible = it },
        onClose = onClose,
        onPrevious = { currentPage = (currentPage - 1).coerceAtLeast(1) },
        onNext = { currentPage = (currentPage + 1).coerceAtMost(state.pageCount) },
        // Page-turn zones and swipe turns are disabled while zoomed so panning
        // the page does not accidentally change pages.
        enablePageTurns = readerSettings.readingMode == PdfReadingMode.PAGINATED && !isZoomed,
        enableSwipeTurns = !isZoomed,
        bottomBar = {
            PdfPageControls(
                currentPage = currentPage,
                pageCount = state.pageCount,
                onPageSelected = { currentPage = it.coerceIn(1, state.pageCount) },
                onOpenReaderSettings = { settingsVisible = true },
            )
        },
    ) { readerModifier ->
        when (readerSettings.readingMode) {
            PdfReadingMode.PAGINATED -> PdfPage(
                page = currentPage,
                viewModel = viewModel,
                zoomGesturesEnabled = true,
                onZoomedChanged = { isZoomed = it },
                modifier = readerModifier,
            )
            PdfReadingMode.CONTINUOUS -> PdfContinuousDocument(
                currentPage = currentPage,
                pageCount = state.pageCount,
                controlsVisible = controlsVisible,
                viewModel = viewModel,
                onPageSelected = { currentPage = it.coerceIn(1, state.pageCount) },
                onZoomedChanged = { isZoomed = it },
                modifier = readerModifier,
            )
        }
    }

    if (settingsVisible) {
        PdfReaderSettingsSheet(
            settings = readerSettings,
            onReadingModeSelected = viewModel::setReadingMode,
            onDismiss = { settingsVisible = false },
        )
    }
}

@Composable
private fun PdfContinuousDocument(
    currentPage: Int,
    pageCount: Int,
    controlsVisible: Boolean,
    viewModel: PdfReaderViewModel,
    onPageSelected: (Int) -> Unit,
    onZoomedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentPage - 1)
    val scope = rememberCoroutineScope()
    // Pinch zoom changes the width pages are laid out and rendered at; the
    // document becomes horizontally pannable through the scroll state.
    var pageZoom by remember { mutableFloatStateOf(1f) }
    val horizontalScroll = rememberScrollState()

    LaunchedEffect(listState) {
        // Do not feed every page boundary back into the whole reader while the
        // finger is moving. That state update also persists progress and can
        // force the controls/content tree to recompose during a frame.
        snapshotFlow { listState.firstVisibleItemIndex to listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { (_, isScrolling) -> !isScrolling }
            .map { (index, _) -> index + 1 }
            .collect(onPageSelected)
    }
    LaunchedEffect(currentPage, listState) {
        val requestedIndex = currentPage - 1
        if (listState.firstVisibleItemIndex != requestedIndex) {
            // A seek from the bottom slider should land directly on its target.
            // Animated intermediate pages feed back through snapshotFlow and can
            // otherwise overwrite a large seek with a one-page update.
            listState.scrollToItem(requestedIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val viewportWidth = maxWidth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScroll)
                .pdfPinchZoom { zoomChange ->
                    val newZoom = (pageZoom * zoomChange).coerceIn(1f, CONTINUOUS_MAX_ZOOM)
                    if (newZoom != pageZoom) {
                        // Keep the horizontal anchor roughly stable across the zoom change;
                        // the scroll range catches up with the new layout on the next frame.
                        val target = (horizontalScroll.value * newZoom / pageZoom).roundToInt()
                        pageZoom = newZoom
                        onZoomedChanged(newZoom > 1.01f)
                        scope.launch { horizontalScroll.scrollTo(target) }
                    }
                },
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    top = if (controlsVisible) 64.dp else 0.dp,
                    bottom = if (controlsVisible) 120.dp else 0.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(pageCount, key = { index -> index }) { index ->
                    PdfPage(
                        page = index + 1,
                        viewModel = viewModel,
                        modifier = Modifier
                            .requiredWidth(viewportWidth * pageZoom)
                            .aspectRatio(PDF_PAGE_ASPECT_RATIO),
                    )
                }
            }
        }
    }
}

/**
 * Detects two-finger pinch gestures without touching single-finger events, so
 * the surrounding LazyColumn scroll keeps working. Pinch events are consumed
 * on the Initial pass to stop the list from scrolling mid-pinch.
 */
private fun Modifier.pdfPinchZoom(onZoomChange: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var zooming = false
        var previousDistance = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressedChanges = event.changes.filter { it.pressed }
            when {
                pressedChanges.size >= 2 -> {
                    zooming = true
                    val distance = pressedChanges[0].position.distanceTo(pressedChanges[1].position)
                    if (previousDistance > 0f && distance > 0f) {
                        onZoomChange(distance / previousDistance)
                    }
                    previousDistance = distance
                    event.changes.forEach { it.consume() }
                }
                pressedChanges.isEmpty() -> break
                zooming -> {
                    // Consume the leftover finger so the list does not fling after the pinch.
                    previousDistance = 0f
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
}

private fun Offset.distanceTo(other: Offset): Float = (this - other).getDistance()

@Composable
private fun PdfPage(
    page: Int,
    viewModel: PdfReaderViewModel,
    zoomGesturesEnabled: Boolean = false,
    onZoomedChanged: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        var zoomScale by remember(page) { mutableFloatStateOf(1f) }
        var zoomOffset by remember(page) { mutableStateOf(Offset.Zero) }
        // The bitmap re-renders at a higher resolution once the zoom settles.
        var renderScale by remember(page) { mutableFloatStateOf(1f) }
        LaunchedEffect(page, zoomScale) {
            delay(RE_RENDER_DEBOUNCE_MILLIS)
            renderScale = zoomScale
        }
        val renderScaleStep = (renderScale * 4f).roundToInt() / 4f
        var bitmap by remember(page) { mutableStateOf<Bitmap?>(null) }
        var isRendering by remember(page) { mutableStateOf(true) }

        LaunchedEffect(page, width, height, renderScaleStep) {
            isRendering = true
            val rendered = viewModel.renderPage(
                page,
                (width * renderScaleStep).roundToInt(),
                (height * renderScaleStep).roundToInt(),
            )
            if (rendered != null) bitmap = rendered
            isRendering = false
        }

        bitmap?.let { pageBitmap ->
            val imageModifier = if (zoomGesturesEnabled) {
                // 手势必须位于 graphicsLayer 外层：否则事件坐标会被图层的逆变换
                // 污染，缩放过程中 centroid 不断漂移，导致缩放围绕错误的点进行。
                Modifier
                    .fillMaxSize()
                    .pointerInput(page) {
                        detectTransformGestures(panZoomLock = true) { centroid, panChange, zoomChange, _ ->
                            val newScale = (zoomScale * zoomChange).coerceIn(1f, MAX_ZOOM)
                            val ratio = newScale / zoomScale
                            val center = Offset(size.width / 2f, size.height / 2f)
                            // 让手指下的内容点在缩放前后保持不动：以 centroid 为锚
                            // 计算新的平移量（围绕中心缩放 + translation 的组合）。
                            val anchored = Offset(
                                (centroid.x - center.x) - (centroid.x - zoomOffset.x - center.x) * ratio,
                                (centroid.y - center.y) - (centroid.y - zoomOffset.y - center.y) * ratio,
                            ) + panChange * ratio
                            zoomScale = newScale
                            zoomOffset = if (newScale <= 1.01f) Offset.Zero else anchored.clampToPage(size.width, size.height, newScale)
                            onZoomedChanged?.invoke(newScale > 1.01f)
                        }
                    }
                    .pointerInput(page) {
                        detectTapGestures(
                            onDoubleTap = { tapPosition ->
                                if (zoomScale > 1.01f) {
                                    zoomScale = 1f
                                    zoomOffset = Offset.Zero
                                    onZoomedChanged?.invoke(false)
                                } else {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    zoomScale = DOUBLE_TAP_ZOOM
                                    zoomOffset = ((center - tapPosition) * (DOUBLE_TAP_ZOOM - 1f))
                                        .clampToPage(size.width, size.height, DOUBLE_TAP_ZOOM)
                                    onZoomedChanged?.invoke(true)
                                }
                            },
                        )
                    }
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = zoomOffset.x
                        translationY = zoomOffset.y
                    }
            } else {
                Modifier.fillMaxSize()
            }
            Image(
                bitmap = pageBitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.pdf_page_content_description, page),
                modifier = imageModifier,
                contentScale = ContentScale.Fit,
            )
        }
        if (bitmap == null && isRendering) {
            CircularProgressIndicator()
        }
    }
}

/** Keeps the zoomed page covering the viewport, i.e. pans cannot reveal the background. */
private fun Offset.clampToPage(width: Int, height: Int, scale: Float): Offset {
    val maxX = width * (scale - 1f) / 2f
    val maxY = height * (scale - 1f) / 2f
    return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}

@Composable
private fun PdfPageControls(
    currentPage: Int,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onOpenReaderSettings: () -> Unit,
) {
    var sliderPage by remember { mutableFloatStateOf(currentPage.toFloat()) }
    var sliderDragging by remember { mutableStateOf(false) }
    LaunchedEffect(currentPage, pageCount, sliderDragging) {
        if (!sliderDragging) sliderPage = currentPage.toFloat()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onPageSelected(currentPage - 1) },
                enabled = currentPage > 1,
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(R.string.content_description_previous_page),
                )
            }
            Text(
                text = stringResource(R.string.pdf_page_indicator, currentPage, pageCount),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onPageSelected(currentPage + 1) },
                    enabled = currentPage < pageCount,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = stringResource(R.string.content_description_next_page),
                    )
                }
                IconButton(onClick = onOpenReaderSettings) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = stringResource(R.string.content_description_pdf_reader_settings),
                    )
                }
            }
        }
        if (pageCount > 1) {
            Slider(
                value = sliderPage.coerceIn(1f, pageCount.toFloat()),
                onValueChange = {
                    sliderDragging = true
                    sliderPage = it
                },
                onValueChangeFinished = {
                    val selectedPage = sliderPage.roundToInt().coerceIn(1, pageCount)
                    sliderDragging = false
                    sliderPage = selectedPage.toFloat()
                    onPageSelected(selectedPage)
                },
                valueRange = 1f..pageCount.toFloat(),
                steps = (pageCount - 2).coerceAtLeast(0),
            )
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfReaderSettingsSheet(
    settings: PdfReaderSettings,
    onReadingModeSelected: (PdfReadingMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.pdf_reading_mode),
                style = MaterialTheme.typography.titleLarge,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PdfReadingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.readingMode == mode,
                        onClick = { onReadingModeSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, PdfReadingMode.entries.size),
                        label = { Text(mode.label()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfReadingMode.label(): String = when (this) {
    PdfReadingMode.PAGINATED -> stringResource(R.string.pdf_reading_mode_paginated)
    PdfReadingMode.CONTINUOUS -> stringResource(R.string.pdf_reading_mode_continuous)
}

@Composable
private fun LoadingPdfState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PdfErrorState(
    error: PdfReaderError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(error.message(), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

private class PdfReaderViewModel(
    application: Application,
    private val args: PdfReaderArgs,
) : AndroidViewModel(application) {
    private val sessionStore = SessionStore(application)
    private val api = BookLoreApi(sessionStore)
    private val pendingSyncQueue = PendingSyncQueue(application, BookLoreDatabase.get(application))
    private val offlineBookStore = OfflineBookStore(application)
    private val readerDirectory = File(application.filesDir, "reader-cache")
    private val cacheFile = File(readerDirectory, "${args.bookId}-${args.bookFileId}.pdf")
    private val offlineFile = offlineBookStore.fileFor(
        Book(
            id = args.bookId,
            primaryFile = BookFile(id = args.bookFileId, bookType = args.bookType, extension = "pdf"),
        ),
    )
    private val streamFile = File(readerDirectory, "${args.bookId}-${args.bookFileId}.pdf.streaming")
    private val sessionTracker = ReadingSessionTracker { report ->
        try {
            api.recordReadingSession(report)
        } catch (error: Exception) {
            // Keep the report for the background sync worker; rethrow so the tracker logs the miss.
            runCatching { pendingSyncQueue.enqueueReadingSession(report) }
            throw error
        }
    }
    private val rendererDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val _state = MutableStateFlow<PdfReaderUiState>(PdfReaderUiState.Loading)
    val state: StateFlow<PdfReaderUiState> = _state.asStateFlow()
    private val _readerSettings = MutableStateFlow(sessionStore.readPdfReaderSettings())
    val readerSettings: StateFlow<PdfReaderSettings> = _readerSettings.asStateFlow()

    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var latestPosition: PdfPosition? = null
    private var syncJob: Job? = null
    private var cacheJob: Job? = null

    init {
        clearExpiredReaderFiles()
        openDocument()
    }

    fun retry() = openDocument()

    fun setReadingMode(mode: PdfReadingMode) {
        val settings = _readerSettings.value
        if (settings.readingMode == mode) return
        val updated = settings.copy(readingMode = mode)
        sessionStore.writePdfReaderSettings(updated)
        _readerSettings.value = updated
    }

    fun onPageSelected(page: Int, pageCount: Int) {
        if (pageCount < 1) return
        val position = PdfPosition(
            bookId = args.bookId,
            bookFileId = args.bookFileId,
            page = page.coerceIn(1, pageCount),
            progressPercent = progressPercent(page, pageCount),
        )
        sessionTracker.trackActivity(
            bookId = args.bookId,
            bookType = args.bookType.uppercase(),
            location = position.page.toString(),
            progressPercent = position.progressPercent,
        )
        if (position == latestPosition) return
        latestPosition = position
        sessionStore.writePdfPosition(position)
        scheduleProgressSync(position)
    }

    fun flushProgress() {
        sessionTracker.endSession()
        val position = latestPosition ?: return
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) { syncProgress(position) }
    }

    suspend fun renderPage(page: Int, maxWidth: Int, maxHeight: Int): Bitmap? =
        withContext(rendererDispatcher) {
            val activeRenderer = renderer ?: return@withContext null
            if (maxWidth <= 0 || maxHeight <= 0) return@withContext null
            val safePage = page.coerceIn(1, activeRenderer.pageCount)
            val pdfPage = activeRenderer.openPage(safePage - 1)
            try {
                val scale = min(
                    maxWidth.toFloat() / pdfPage.width.coerceAtLeast(1),
                    maxHeight.toFloat() / pdfPage.height.coerceAtLeast(1),
                ).coerceAtLeast(0.1f)
                var width = (pdfPage.width * scale).roundToInt().coerceAtLeast(1)
                var height = (pdfPage.height * scale).roundToInt().coerceAtLeast(1)
                // Cap the bitmap size so zoomed rendering cannot exhaust memory.
                val pixels = width.toLong() * height.toLong()
                if (pixels > MAX_RENDER_PIXELS) {
                    val shrink = sqrt(MAX_RENDER_PIXELS.toDouble() / pixels).toFloat()
                    width = (width * shrink).roundToInt().coerceAtLeast(1)
                    height = (height * shrink).roundToInt().coerceAtLeast(1)
                }
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    pdfPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            } finally {
                pdfPage.close()
            }
        }

    private fun openDocument(preferStreaming: Boolean = true) {
        syncJob?.cancel()
        _state.value = PdfReaderUiState.Loading
        viewModelScope.launch {
            var fromCache = false
            var usingStreaming = false
            try {
                val pageCount = withContext(rendererDispatcher) {
                    closeRenderer()
                    // Reuse the previously downloaded copy when it is still readable.
                    val fromOffline = offlineFile?.let(::isReadableBookFile) == true
                    fromCache = !fromOffline && isReadableBookFile(cacheFile)
                    val newDescriptor = when {
                        fromOffline -> {
                            offlineFile!!.setLastModified(System.currentTimeMillis())
                            ParcelFileDescriptor.open(offlineFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                        fromCache -> {
                            cacheFile.setLastModified(System.currentTimeMillis())
                            ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                        preferStreaming -> {
                            // PdfRenderer needs a seekable descriptor. This proxy fetches only the
                            // byte ranges it asks for; a failure falls back to a full local copy.
                            usingStreaming = true
                            RangeBackedBookFile(
                                context = getApplication(),
                                api = api,
                                bookId = args.bookId,
                                bookType = args.bookType,
                                streamFile = streamFile,
                            ).open()
                        }
                        else -> {
                            api.downloadPdf(args.bookId, args.bookType, cacheFile)
                            if (!cacheFile.hasPdfSignature()) throw InvalidPdfFileException()
                            cacheFile.setLastModified(System.currentTimeMillis())
                            ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
                        }
                    }
                    try {
                        val newRenderer = PdfRenderer(newDescriptor)
                        if (newRenderer.pageCount < 1) {
                            newRenderer.close()
                            throw IllegalStateException("The PDF has no pages")
                        }
                        descriptor = newDescriptor
                        renderer = newRenderer
                        newRenderer.pageCount
                    } catch (error: Exception) {
                        newDescriptor.close()
                        throw error
                    }
                }
                val position = initialPosition(pageCount)
                latestPosition = position
                _state.value = PdfReaderUiState.Ready(
                    pageCount = pageCount,
                    initialPage = position.page,
                )
                if (usingStreaming) cacheBookInBackground()
            } catch (error: Exception) {
                withContext(rendererDispatcher) { closeRenderer() }
                if (!sessionStore.readLocalMode() && offlineFile?.exists() == true && !fromCache) {
                    offlineFile.delete()
                    openDocument()
                    return@launch
                }
                if (fromCache && !sessionStore.readLocalMode()) {
                    // The cached copy turned out unreadable — drop it and download fresh once.
                    cacheFile.delete()
                    openDocument()
                    return@launch
                }
                if (preferStreaming && !sessionStore.readLocalMode()) {
                    // A server without range support, or a proxy-read failure, should never
                    // prevent the established full-download reader path from opening the book.
                    openDocument(preferStreaming = false)
                    return@launch
                }
                if (!sessionStore.readLocalMode() && error !is NetworkException && error !is ApiException) {
                    cacheFile.delete()
                }
                _state.value = PdfReaderUiState.Error(error.toPdfReaderError())
            }
        }
    }

    private fun cacheBookInBackground() {
        if (cacheJob?.isActive == true) return
        cacheJob = viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                api.downloadPdf(args.bookId, args.bookType, cacheFile)
                cacheFile.setLastModified(System.currentTimeMillis())
            }
        }
    }

    private fun initialPosition(pageCount: Int): PdfPosition {
        val stored = sessionStore.readPdfPosition(args.bookId, args.bookFileId)
            ?: PdfPosition(
                bookId = args.bookId,
                bookFileId = args.bookFileId,
                page = args.page,
                progressPercent = args.progressPercent,
            )
        val page = stored.page.coerceIn(1, pageCount)
        return stored.copy(page = page, progressPercent = progressPercent(page, pageCount))
    }

    private fun scheduleProgressSync(position: PdfPosition) {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            delay(PROGRESS_SYNC_DELAY_MILLIS)
            syncProgress(position)
        }
    }

    private suspend fun syncProgress(position: PdfPosition) {
        try {
            api.updatePdfProgress(position)
        } catch (_: Exception) {
            // Queue for the background worker; the encrypted local page remains as backup.
            runCatching { pendingSyncQueue.enqueuePdfProgress(position) }
        }
    }

    private fun clearExpiredReaderFiles() {
        if (sessionStore.readLocalMode()) return
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

    private fun closeRenderer() {
        renderer?.close()
        renderer = null
        descriptor?.close()
        descriptor = null
    }

    override fun onCleared() {
        sessionTracker.endSession()
        syncJob?.cancel()
        cacheJob?.cancel()
        closeRenderer()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val args: PdfReaderArgs?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            requireNotNull(args) { "Missing PDF reader arguments" }
            return PdfReaderViewModel(application, args) as T
        }
    }

    private companion object {
        const val PROGRESS_SYNC_DELAY_MILLIS = 1_500L
        const val READER_CACHE_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1_000L
        const val READER_CACHE_MAX_BYTES = 512L * 1024 * 1024
    }
}

private data class PdfReaderArgs(
    val bookId: Long,
    val bookFileId: Long,
    val title: String,
    val bookType: String,
    val page: Int,
    val progressPercent: Float,
) {
    companion object {
        fun fromIntent(intent: Intent): PdfReaderArgs? {
            val bookId = intent.getLongExtra("book_id", -1L)
            val bookFileId = intent.getLongExtra("book_file_id", -1L)
            if (bookId < 0 || bookFileId < 0) return null
            return PdfReaderArgs(
                bookId = bookId,
                bookFileId = bookFileId,
                title = intent.getStringExtra("title").orEmpty(),
                bookType = intent.getStringExtra("book_type") ?: "PDF",
                page = intent.getIntExtra("page", 1).coerceAtLeast(1),
                progressPercent = intent.getFloatExtra("progress", 0f).coerceIn(0f, 100f),
            )
        }
    }
}

private sealed interface PdfReaderUiState {
    data object Loading : PdfReaderUiState
    data class Ready(val pageCount: Int, val initialPage: Int) : PdfReaderUiState
    data class Error(val error: PdfReaderError) : PdfReaderUiState
}

private const val PDF_PAGE_ASPECT_RATIO = 0.72f

// Pinch-zoom limits for the paginated reader.
private const val MAX_ZOOM = 5f
private const val DOUBLE_TAP_ZOOM = 2.5f
// Pinch-zoom limit for the continuous reader (pages re-layout at this width scale).
private const val CONTINUOUS_MAX_ZOOM = 3f
// Re-render the page bitmap at the zoomed resolution once the gesture settles.
private const val RE_RENDER_DEBOUNCE_MILLIS = 250L
// Upper bound for a rendered page bitmap (≈16MB as ARGB_8888) to keep zoomed
// rendering within a sane memory budget.
private const val MAX_RENDER_PIXELS = 4_000_000L

private enum class PdfReaderError {
    Unauthorized,
    Forbidden,
    NotFound,
    Network,
    Generic,
}

private fun Exception.toPdfReaderError(): PdfReaderError = when (this) {
    is NetworkException -> PdfReaderError.Network
    is ApiException -> when (statusCode) {
        401 -> PdfReaderError.Unauthorized
        403 -> PdfReaderError.Forbidden
        404 -> PdfReaderError.NotFound
        else -> PdfReaderError.Generic
    }
    else -> PdfReaderError.Generic
}

@Composable
private fun PdfReaderError.message(): String = when (this) {
    PdfReaderError.Unauthorized -> stringResource(R.string.error_unauthorized)
    PdfReaderError.Forbidden -> stringResource(R.string.error_forbidden)
    PdfReaderError.NotFound -> stringResource(R.string.error_not_found)
    PdfReaderError.Network -> stringResource(R.string.error_network)
    PdfReaderError.Generic -> stringResource(R.string.error_generic)
}

private fun progressPercent(page: Int, pageCount: Int): Float =
    if (pageCount > 0) (page.coerceIn(1, pageCount).toFloat() / pageCount * 100f).coerceIn(0f, 100f) else 0f

private class InvalidPdfFileException : Exception()

private fun File.hasPdfSignature(): Boolean = runCatching {
    if (length() < 5L) return@runCatching false
    inputStream().buffered().use { input ->
        val header = ByteArray(5)
        input.read(header) == 5 && String(header) == "%PDF-"
    }
}.getOrDefault(false)

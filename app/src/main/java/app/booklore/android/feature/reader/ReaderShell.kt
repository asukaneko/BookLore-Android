package app.booklore.android.feature.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import app.booklore.android.R
import kotlin.math.abs

internal data class ReaderChrome(
    val background: Color,
    val content: Color,
)

/** Shared reader frame for format-specific content renderers. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderShell(
    title: String,
    chrome: ReaderChrome,
    controlsVisible: Boolean,
    onControlsVisibleChanged: (Boolean) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    bottomBar: @Composable () -> Unit,
    enablePageTurns: Boolean = true,
    enableSwipeTurns: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    stableContentLayout: Boolean = false,
    content: @Composable BoxScope.(Modifier) -> Unit,
) {
    ReaderSystemBars(
        hidden = !controlsVisible,
        stableLayout = stableContentLayout,
        // System bar icons must contrast with the reader chrome (which may be
        // dark even when the system is in light mode, and vice versa).
        lightBackground = chrome.background.luminance() > 0.5f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chrome.background),
    ) {
        content(
            Modifier
                .fillMaxSize()
                .readerGestures(
                    enablePageTurns = enablePageTurns,
                    enableSwipeTurns = enableSwipeTurns,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onToggleControls = { onControlsVisibleChanged(!controlsVisible) },
                ),
        )
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            Surface(color = chrome.background, contentColor = chrome.content) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = chrome.background,
                        titleContentColor = chrome.content,
                        navigationIconContentColor = chrome.content,
                    ),
                )
            }
        }
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Surface(
                color = chrome.background,
                contentColor = chrome.content,
                tonalElevation = 3.dp,
            ) {
                bottomBar()
            }
        }
        if (footer != null) {
            AnimatedVisibility(
                visible = !controlsVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                footer()
            }
        }
    }
}

private fun Modifier.readerGestures(
    enablePageTurns: Boolean,
    enableSwipeTurns: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleControls: () -> Unit,
): Modifier = pointerInput(enablePageTurns, enableSwipeTurns, onPrevious, onNext, onToggleControls) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        val initialX = down.position.x
        var horizontalDistance = 0f
        var verticalDistance = 0f

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            horizontalDistance += change.position.x - change.previousPosition.x
            verticalDistance += change.position.y - change.previousPosition.y
            if (change.pressed) continue

            val isTap = abs(horizontalDistance) <= viewConfiguration.touchSlop &&
                abs(verticalDistance) <= viewConfiguration.touchSlop
            when {
                isTap && initialX in (size.width / 3f)..(size.width * 2f / 3f) -> onToggleControls()
                enableSwipeTurns &&
                    abs(horizontalDistance) >= size.width * PAGE_SWIPE_THRESHOLD &&
                    abs(horizontalDistance) > abs(verticalDistance) -> {
                    if (horizontalDistance < 0f) onNext() else onPrevious()
                }
                enablePageTurns && isTap && initialX < size.width / 3f -> onPrevious()
                enablePageTurns && isTap && initialX > size.width * 2f / 3f -> onNext()
            }
            break
        }
    }
}

@Composable
private fun ReaderSystemBars(hidden: Boolean, stableLayout: Boolean = false, lightBackground: Boolean = true) {
    val view = LocalView.current
    val window = view.context.findActivity()?.window ?: return

    DisposableEffect(window, hidden, stableLayout, lightBackground) {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        // Icon color must follow the reader chrome instead of the system theme flag.
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
        // With stableLayout the window stays edge-to-edge permanently so the content
        // bounds never change when bars are toggled (prevents EPUB reflow jitter).
        WindowCompat.setDecorFitsSystemWindows(window, !hidden && !stableLayout)
        if (hidden) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (!stableLayout) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
            }
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PAGE_SWIPE_THRESHOLD = 0.12f

package com.ccread.reader.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ccread.reader.JingduApp
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.EpubProgress
import com.ccread.reader.domain.model.TocNode
import com.ccread.reader.domain.settings.BackgroundPalette
import com.ccread.reader.domain.settings.BrightnessController
import com.ccread.reader.ui.reader.epub.EpubReaderView
import com.ccread.reader.ui.reader.txt.TxtReaderView
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun ReaderScreen(bookId: Long, onBack: () -> Unit, onMissingFile: () -> Unit = {}) {
    val app = LocalContext.current.applicationContext as JingduApp
    val viewModel: ReaderViewModel = viewModel(
        key = "reader-$bookId",
        factory = viewModelFactory {
            initializer {
                ReaderViewModel(bookId, app.bookRepository, app.settingsRepository, app.txtBookIndexer)
            }
        },
    )
    val state by viewModel.state.collectAsState()
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    var timeText by remember { mutableStateOf(currentTimeText()) }
    var imageData by remember { mutableStateOf<String?>(null) }
    var imageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            timeText = currentTimeText()
        }
    }

    BackHandler {
        if (!viewModel.handleBack()) onBack()
    }

    LaunchedEffect(state.missingFile) {
        if (state.missingFile) onMissingFile()
    }

    DisposableEffect(state.settings.followSystemBrightness, state.settings.brightness) {
        val window = view.context.findActivity()?.window
        val attributes = window?.attributes
        if (attributes != null) {
            attributes.screenBrightness = BrightnessController.windowBrightness(
                state.settings.followSystemBrightness,
                state.settings.brightness,
            )
            window.attributes = attributes
        }
        onDispose {
            val current = window?.attributes
            if (current != null) {
                current.screenBrightness = -1f
                window.attributes = current
            }
        }
    }

    val backgroundStyle = BackgroundPalette.styleOf(state.settings.backgroundId)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundStyle.background,
    ) {
        if (!state.loaded) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("正在打开…")
            }
        } else if (state.book == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("找不到这本书")
                    TextButton(onClick = onBack) { Text("返回书架") }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ReaderTopBar(
                        chapterTitle = state.chapterTitle,
                        time = timeText,
                        color = backgroundStyle.text.copy(alpha = 0.7f),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        val book = state.book
                        if (book != null && book.format == BookFormat.TXT) {
                            key(state.jumpToken) {
                                TxtReaderView(
                                    book = book,
                                    chapters = state.chapters,
                                    settings = state.settings,
                                    mode = state.mode,
                                    textColor = backgroundStyle.text,
                                    startChapterIndex = state.startChapterIndex,
                                    startOffsetInChapter = state.startOffsetInChapter,
                                    onCenterTap = { viewModel.onTapZone(TapZone.CENTER) },
                                    onPositionChanged = viewModel::onPositionChanged,
                                    onMissingFile = viewModel::markMissingFile,
                                )
                            }
                        } else if (book != null) {
                            EpubReaderView(
                                book = book,
                                settings = state.settings,
                                mode = state.mode,
                                initialLocatorJson = (book.progress as? EpubProgress)?.locatorJson,
                                jumpHref = state.epubJumpHref,
                                onJumpConsumed = viewModel::consumeEpubJump,
                                onTocLoaded = viewModel::onEpubTocLoaded,
                                onCenterTap = { viewModel.onTapZone(TapZone.CENTER) },
                                onImageTap = { data ->
                                    imageData = data
                                    imageVisible = true
                                },
                                onLocatorChanged = viewModel::onEpubLocator,
                                onMissingFile = viewModel::markMissingFile,
                            )
                        }
                    }
                    ReaderBottomBar(
                        percentText = state.percentText,
                        color = backgroundStyle.text.copy(alpha = 0.7f),
                    )
                }

                if (state.missingFile) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        tonalElevation = 6.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("找不到原文件，可能已被移动或删除")
                            TextButton(onClick = onBack) { Text("返回书架重新选择") }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.overlays.menu,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { viewModel.dismissOverlays() },
                    )
                }
                AnimatedVisibility(
                    visible = state.overlays.menu,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    ReaderMenu(
                        onToc = viewModel::openToc,
                        onSettings = viewModel::openSettings,
                        onBrightness = viewModel::openBrightness,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AnimatedVisibility(
                    visible = state.overlays.toc,
                    enter = slideInHorizontally { -it } + fadeIn(),
                    exit = slideOutHorizontally { -it } + fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TocDrawer(
                        bookTitle = state.book?.title.orEmpty(),
                        rows = if (state.book?.format == BookFormat.TXT) {
                            state.chapters.map { chapter ->
                                TocRow(
                                    key = chapter.index.toString(),
                                    title = chapter.title,
                                    depth = 0,
                                )
                            }
                        } else {
                            epubRows(state.epubToc, state.expandedEpubKeys)
                        },
                        currentKey = if (state.book?.format == BookFormat.TXT) {
                            state.currentChapterIndex.toString()
                        } else {
                            state.epubCurrentHref
                        },
                        onSelect = { row ->
                            if (state.book?.format == BookFormat.TXT) {
                                row.key.toIntOrNull()?.let(viewModel::jumpTo)
                            } else {
                                viewModel.jumpToEpub(row.key)
                            }
                        },
                        onToggle = { row ->
                            if (state.book?.format != BookFormat.TXT) {
                                viewModel.toggleEpubNode(row.key)
                            }
                        },
                        onResplit = if (state.book?.format == BookFormat.TXT) viewModel::resplit else null,
                        onDismiss = viewModel::dismissOverlays,
                    )
                }

                when {
                    state.overlays.settings -> {
                        ReadingSettingsSheet(
                            settings = state.settings,
                            format = state.book?.format ?: BookFormat.TXT,
                            onChange = { updated -> viewModel.updateSettings { updated } },
                            onDismiss = viewModel::dismissOverlays,
                        )
                    }
                    state.overlays.brightness -> {
                        BrightnessSheet(
                            settings = state.settings,
                            onChange = { updated -> viewModel.updateSettings { updated } },
                            onDismiss = viewModel::dismissOverlays,
                        )
                    }
                }

                ImageViewerDialog(
                    dataUrl = imageData,
                    visible = imageVisible,
                    onDismiss = { imageVisible = false },
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun epubRows(
    nodes: List<TocNode>,
    expanded: Set<String>,
    depth: Int = 0,
): List<TocRow> = nodes.flatMap { node ->
    val key = node.href ?: node.title
    val row = TocRow(
        key = key,
        title = node.title,
        depth = depth,
        expandable = node.children.isNotEmpty(),
        expanded = key in expanded,
    )
    if (row.expanded && node.children.isNotEmpty()) {
        listOf(row) + epubRows(node.children, expanded, depth + 1)
    } else {
        listOf(row)
    }
}

private fun currentTimeText(): String =
    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

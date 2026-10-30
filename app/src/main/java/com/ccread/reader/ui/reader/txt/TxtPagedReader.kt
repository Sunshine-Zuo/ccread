package com.ccread.reader.ui.reader.txt

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.progress.ProgressCalculator
import com.ccread.reader.domain.txt.ComposePageBreaker
import com.ccread.reader.domain.txt.Page
import com.ccread.reader.domain.txt.PageMapping
import com.ccread.reader.domain.txt.TxtBookSource
import com.ccread.reader.domain.txt.TxtIndent
import com.ccread.reader.ui.reader.ReaderInteraction
import com.ccread.reader.ui.reader.TapZone
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 排版耗时不短，放到单一后台线程，避免卡住菜单收起等主线程动画。 */
private val paginationDispatcher = Dispatchers.Default.limitedParallelism(1)

private data class PageRef(
    val chapterIndex: Int,
    val pageIndex: Int,
    val page: Page,
)

@Composable
fun TxtPagedReader(
    source: TxtBookSource,
    chapters: List<Chapter>,
    settings: ReadingSettings,
    textColor: Color,
    startChapterIndex: Int,
    startOffsetInChapter: Int,
    onCenterTap: () -> Unit,
    onPositionChanged: (chapterIndex: Int, offsetInChapter: Int, percent: Double) -> Unit,
) {
    if (chapters.isEmpty()) {
        Text("这本书没有可显示的章节")
        return
    }

    var chapterIndex by rememberSaveable {
        mutableIntStateOf(chapters.firstOrNull { it.index == startChapterIndex }?.index ?: chapters.first().index)
    }
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var desiredOffset by rememberSaveable {
        mutableIntStateOf(if (startChapterIndex == chapterIndex) startOffsetInChapter else 0)
    }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var animating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val textCache = remember { mutableStateMapOf<Int, String>() }
    val pageCache = remember { mutableStateMapOf<String, List<Page>>() }

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = remember(settings.fontSize, settings.lineSpacing, textColor) {
        TextStyle(
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
            color = textColor,
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val marginPx = with(density) { settings.marginHorizontal.dp.toPx().roundToInt() }
        val verticalPaddingPx = with(density) { 8.dp.toPx().roundToInt() }
        val widthPx = (constraints.maxWidth - marginPx * 2).coerceAtLeast(1)
        val heightPx = (constraints.maxHeight - verticalPaddingPx * 2).coerceAtLeast(1)
        val pageWidth = constraints.maxWidth.toFloat()

        LaunchedEffect(
            chapterIndex,
            widthPx,
            heightPx,
            settings.fontSize,
            settings.lineSpacing,
            settings.marginHorizontal,
        ) {
            val pageKey = "$widthPx:$heightPx:${settings.fontSize}:${settings.lineSpacing}:${settings.marginHorizontal}"
            pageCache.keys.filterNot { it.startsWith("$pageKey:") }.forEach { pageCache.remove(it) }
            val breaker = ComposePageBreaker(measurer, widthPx, heightPx, textStyle)
            val neighbors = listOf(chapterIndex, chapterIndex - 1, chapterIndex + 1)
            for (index in neighbors) {
                if (chapters.none { it.index == index }) continue
                val text = textCache[index] ?: withContext(Dispatchers.IO) {
                    source.chapterText(index)
                }.also { textCache[index] = it }
                val key = "$pageKey:$index"
                if (pageCache[key] == null) {
                    pageCache[key] = withContext(paginationDispatcher) {
                        breaker.paginate(text)
                    }
                }
            }
        }

        val pageKey = "$widthPx:$heightPx:${settings.fontSize}:${settings.lineSpacing}:${settings.marginHorizontal}"
        val chapter = chapters.firstOrNull { it.index == chapterIndex } ?: chapters.first()
        val pages = pageCache["$pageKey:${chapter.index}"] ?: emptyList()

        LaunchedEffect(pages) {
            if (pages.isNotEmpty()) {
                pageIndex = PageMapping.pageForOffset(pages, desiredOffset)
            }
        }

        LaunchedEffect(chapterIndex, pageIndex, pages) {
            val page = pages.getOrNull(pageIndex) ?: return@LaunchedEffect
            val percent = ProgressCalculator.txtPercent(
                totalChars = source.totalChars(),
                chapterCharStart = chapter.charStart,
                offsetInChapter = page.startChar.toLong(),
            )
            onPositionChanged(chapterIndex, page.startChar, percent)
        }

        fun goNext() {
            if (pages.isEmpty()) return
            if (pageIndex < pages.lastIndex) {
                pageIndex++
                desiredOffset = pages[pageIndex].startChar
            } else {
                val next = chapters.firstOrNull { it.index == chapterIndex + 1 } ?: return
                chapterIndex = next.index
                pageIndex = 0
                desiredOffset = 0
            }
        }

        fun goPrevious() {
            if (pages.isEmpty()) return
            if (pageIndex > 0) {
                pageIndex--
                desiredOffset = pages[pageIndex].startChar
            } else {
                val previous = chapters.firstOrNull { it.index == chapterIndex - 1 } ?: return
                chapterIndex = previous.index
                desiredOffset = Int.MAX_VALUE
                pageIndex = pageCache["$pageKey:${previous.index}"]?.lastIndex ?: 0
            }
        }

        val currentPage = pages.getOrNull(pageIndex)
        val currentRef = currentPage?.let { PageRef(chapter.index, pageIndex, it) }
        val nextRef = currentRef?.let { neighborPage(it, 1, pageKey, chapters, pageCache) }
        val previousRef = currentRef?.let { neighborPage(it, -1, pageKey, chapters, pageCache) }

        val latestNextRef by rememberUpdatedState(nextRef)
        val latestPreviousRef by rememberUpdatedState(previousRef)
        val latestGoNext by rememberUpdatedState { goNext() }
        val latestGoPrevious by rememberUpdatedState { goPrevious() }

        if (pages.isEmpty()) {
            Text(
                text = "正在排版…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = settings.marginHorizontal.dp, vertical = 8.dp),
                color = textColor,
            )
        } else if (currentPage != null) {
            val isNext = dragOffset < 0f
            val neighbor = when {
                dragOffset == 0f -> null
                isNext -> nextRef
                else -> previousRef
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (animating) return@detectTapGestures
                            when (ReaderInteraction.zoneFor(offset.x, size.width.toFloat())) {
                                TapZone.LEFT -> latestGoPrevious()
                                TapZone.RIGHT -> latestGoNext()
                                TapZone.CENTER -> onCenterTap()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragCancel = {
                                if (!animating) {
                                    animating = true
                                    val start = dragOffset
                                    scope.launch {
                                        animate(start, 0f, animationSpec = tween(180)) { value, _ ->
                                            dragOffset = value
                                        }
                                        animating = false
                                    }
                                }
                            },
                            onDragEnd = {
                                if (animating) return@detectHorizontalDragGestures
                                val width = size.width.toFloat()
                                val target = if (dragOffset < 0f) latestNextRef else latestPreviousRef
                                if (target != null && PageMapping.shouldFlip(dragOffset, width)) {
                                    animating = true
                                    val start = dragOffset
                                    val end = if (dragOffset < 0f) -width else width
                                    scope.launch {
                                        animate(start, end, animationSpec = tween(220)) { value, _ ->
                                            dragOffset = value
                                        }
                                        chapterIndex = target.chapterIndex
                                        pageIndex = target.pageIndex
                                        desiredOffset = target.page.startChar
                                        dragOffset = 0f
                                        animating = false
                                    }
                                } else if (dragOffset != 0f) {
                                    animating = true
                                    val start = dragOffset
                                    scope.launch {
                                        animate(start, 0f, animationSpec = tween(180)) { value, _ ->
                                            dragOffset = value
                                        }
                                        animating = false
                                    }
                                }
                            },
                            onHorizontalDrag = { change, amount ->
                                if (animating) return@detectHorizontalDragGestures
                                val canMove = (amount < 0f && latestNextRef != null) ||
                                    (amount > 0f && latestPreviousRef != null)
                                if (!canMove) return@detectHorizontalDragGestures
                                change.consume()
                                val width = size.width.toFloat()
                                dragOffset = (dragOffset + amount).coerceIn(-width, width)
                            },
                        )
                    },
            ) {
                PageContent(
                    page = currentPage,
                    chapterText = textCache[chapter.index],
                    style = textStyle,
                    horizontalPadding = settings.marginHorizontal.dp,
                    offsetX = dragOffset,
                )
                neighbor?.let { ref ->
                    PageContent(
                        page = ref.page,
                        chapterText = textCache[ref.chapterIndex],
                        style = textStyle,
                        horizontalPadding = settings.marginHorizontal.dp,
                        offsetX = dragOffset + (if (isNext) pageWidth else -pageWidth),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageContent(
    page: Page,
    chapterText: String?,
    style: TextStyle,
    horizontalPadding: Dp,
    offsetX: Float,
) {
    val text = remember(page, chapterText) {
        chapterText?.let { TxtIndent.page(it, page.startChar, page.endChar) }
            ?: AnnotatedString(page.text)
    }
    Text(
        text = text,
        style = style,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
    )
}

private fun neighborPage(
    current: PageRef,
    delta: Int,
    pageKey: String,
    chapters: List<Chapter>,
    pageCache: Map<String, List<Page>>,
): PageRef? {
    val currentPages = pageCache["$pageKey:${current.chapterIndex}"] ?: return null
    val targetIndex = current.pageIndex + delta
    if (targetIndex in currentPages.indices) {
        return PageRef(current.chapterIndex, targetIndex, currentPages[targetIndex])
    }
    val targetChapter = chapters.firstOrNull { it.index == current.chapterIndex + delta } ?: return null
    val targetPages = pageCache["$pageKey:${targetChapter.index}"] ?: return null
    if (targetPages.isEmpty()) return null
    val index = if (delta > 0) 0 else targetPages.lastIndex
    return PageRef(targetChapter.index, index, targetPages[index])
}

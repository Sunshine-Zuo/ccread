package com.ccread.reader.ui.reader.txt

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
import com.ccread.reader.ui.reader.ReaderInteraction
import com.ccread.reader.ui.reader.TapZone
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                    pageCache[key] = breaker.paginate(text)
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

        if (pages.isEmpty()) {
            Text(
                text = "正在排版…",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = settings.marginHorizontal.dp, vertical = 8.dp),
                color = textColor,
            )
        } else {
            val page = pages.getOrNull(pageIndex) ?: pages.first()
            Text(
                text = page.text,
                style = textStyle,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pages, pageIndex, chapterIndex) {
                        detectTapGestures { offset ->
                            when (ReaderInteraction.zoneFor(offset.x, size.width.toFloat())) {
                                TapZone.LEFT -> goPrevious()
                                TapZone.RIGHT -> goNext()
                                TapZone.CENTER -> onCenterTap()
                            }
                        }
                    }
                    .pointerInput(pages, pageIndex, chapterIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (PageMapping.shouldFlip(dragOffset, size.width.toFloat())) {
                                    if (dragOffset < 0) goNext() else goPrevious()
                                }
                                dragOffset = 0f
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                            },
                        )
                    }
                    .offset { IntOffset(dragOffset.roundToInt(), 0) }
                    .padding(horizontal = settings.marginHorizontal.dp, vertical = 8.dp),
            )
        }
    }
}

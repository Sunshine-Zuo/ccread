package com.ccread.reader.ui.reader.txt

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.progress.ProgressCalculator
import com.ccread.reader.domain.txt.ScrollItem
import com.ccread.reader.domain.txt.ScrollMapping
import com.ccread.reader.domain.txt.TxtBookSource
import com.ccread.reader.domain.txt.TxtIndent
import com.ccread.reader.ui.reader.ReaderInteraction
import com.ccread.reader.ui.reader.TapZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ScrollEntry(
    val chapterIndex: Int,
    val charStart: Int,
    val text: String,
)

@Composable
fun TxtScrollReader(
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

    val listState = rememberLazyListState()
    var entries by remember { mutableStateOf<List<ScrollEntry>>(emptyList()) }
    var loadedUpTo by remember { mutableIntStateOf(-1) }
    var loadedFrom by remember { mutableIntStateOf(startChapterIndex) }
    var initialScrollDone by remember { mutableStateOf(false) }

    val textStyle = remember(settings.fontSize, settings.lineSpacing, textColor) {
        TextStyle(
            fontSize = settings.fontSize.sp,
            lineHeight = (settings.fontSize * settings.lineSpacing).sp,
            color = textColor,
        )
    }

    LaunchedEffect(chapters) {
        val startPosition = chapters.indexOfFirst { it.index == startChapterIndex }.coerceAtLeast(0)
        val initial = chapters.drop(startPosition).take(2)
        entries = withContext(Dispatchers.IO) {
            initial.flatMap { entriesFor(it, source) }
        }
        loadedUpTo = initial.lastOrNull()?.index ?: startChapterIndex
        loadedFrom = initial.firstOrNull()?.index ?: startChapterIndex
        if (!initialScrollDone) {
            val target = entries.indexOfLast {
                it.chapterIndex == startChapterIndex && it.charStart <= startOffsetInChapter
            }
            listState.scrollToItem(target.coerceAtLeast(0))
            initialScrollDone = true
        }
    }

    LaunchedEffect(entries, loadedUpTo) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                if (last >= entries.size - 3) {
                    val next = chapters.firstOrNull { it.index == loadedUpTo + 1 } ?: return@collect
                    val more = withContext(Dispatchers.IO) { entriesFor(next, source) }
                    entries = entries + more
                    loadedUpTo = next.index
                }
            }
    }

    LaunchedEffect(entries, loadedFrom) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { first ->
                if (!initialScrollDone) return@collect
                if (first <= 0) {
                    val previous = chapters.firstOrNull { it.index == loadedFrom - 1 } ?: return@collect
                    val more = withContext(Dispatchers.IO) { entriesFor(previous, source) }
                    if (more.isNotEmpty()) {
                        entries = more + entries
                        loadedFrom = previous.index
                        listState.scrollToItem(more.size)
                    }
                }
            }
    }

    LaunchedEffect(entries) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val item = entries.getOrNull(index) ?: return@collect
                val chapter = chapters.firstOrNull { it.index == item.chapterIndex } ?: return@collect
                val percent = ProgressCalculator.txtPercent(
                    totalChars = source.totalChars(),
                    chapterCharStart = chapter.charStart,
                    offsetInChapter = item.charStart.toLong(),
                )
                onPositionChanged(item.chapterIndex, item.charStart, percent)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val up = waitForUpOrCancellation() ?: return@awaitEachGesture
                    if ((up.position - down.position).getDistance() < viewConfiguration.touchSlop &&
                        ReaderInteraction.zoneFor(down.position.x, size.width.toFloat()) == TapZone.CENTER
                    ) {
                        onCenterTap()
                    }
                }
            },
        contentPadding = PaddingValues(horizontal = settings.marginHorizontal.dp, vertical = 8.dp),
    ) {
        items(count = entries.size, key = { index -> "${entries[index].chapterIndex}:${entries[index].charStart}" }) { index ->
            Text(
                text = TxtIndent.paragraph(entries[index].text),
                style = textStyle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            )
        }
    }
}

private fun entriesFor(chapter: Chapter, source: TxtBookSource): List<ScrollEntry> {
    val text = source.chapterText(chapter.index)
    if (text.isEmpty()) return emptyList()
    val result = ArrayList<ScrollEntry>()
    var start = 0
    for (index in text.indices) {
        if (text[index] == '\n') {
            if (index > start) result += ScrollEntry(chapter.index, start, text.substring(start, index))
            start = index + 1
        }
    }
    if (start < text.length) result += ScrollEntry(chapter.index, start, text.substring(start))
    return result
}

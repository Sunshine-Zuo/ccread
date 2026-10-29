package com.ccread.reader.ui.reader.txt

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.ccread.reader.data.file.ContentUriSeekableFile
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.txt.TxtBookSource
import java.nio.charset.Charset

@Composable
fun TxtReaderView(
    book: Book,
    chapters: List<Chapter>,
    settings: ReadingSettings,
    mode: PageMode,
    textColor: Color,
    startChapterIndex: Int,
    startOffsetInChapter: Int,
    onCenterTap: () -> Unit,
    onPositionChanged: (chapterIndex: Int, offsetInChapter: Int, percent: Double) -> Unit,
    onMissingFile: () -> Unit,
) {
    val context = LocalContext.current
    val file = remember(book.uri) {
        runCatching { ContentUriSeekableFile(context, Uri.parse(book.uri)) }.getOrNull()
    }
    DisposableEffect(file) {
        onDispose { file?.close() }
    }

    if (file == null) {
        LaunchedEffect(Unit) { onMissingFile() }
        Text("找不到原文件", modifier = Modifier.fillMaxSize())
        return
    }

    val charset = remember(book.encodingName) {
        book.encodingName?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
    }
    val source = remember(file, chapters, charset, book.totalChars) {
        TxtBookSource(file, chapters, charset, book.totalChars ?: 0L)
    }

    when (mode) {
        PageMode.PAGED -> TxtPagedReader(
            source = source,
            chapters = chapters,
            settings = settings,
            textColor = textColor,
            startChapterIndex = startChapterIndex,
            startOffsetInChapter = startOffsetInChapter,
            onCenterTap = onCenterTap,
            onPositionChanged = onPositionChanged,
        )

        PageMode.SCROLL -> TxtScrollReader(
            source = source,
            chapters = chapters,
            settings = settings,
            textColor = textColor,
            startChapterIndex = startChapterIndex,
            startOffsetInChapter = startOffsetInChapter,
            onCenterTap = onCenterTap,
            onPositionChanged = onPositionChanged,
        )
    }
}

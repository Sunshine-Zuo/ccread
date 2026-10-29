package com.ccread.reader.domain.book

import android.content.Context
import android.net.Uri
import com.ccread.reader.data.file.ContentUriSeekableFile
import com.ccread.reader.data.repository.BookRepository
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.txt.TxtChapterSplitter
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TxtBookIndexer(
    private val context: Context,
    private val repository: BookRepository,
) {
    suspend fun resplit(book: Book): List<Chapter>? = withContext(Dispatchers.IO) {
        val file = runCatching {
            ContentUriSeekableFile(context, Uri.parse(book.uri))
        }.getOrNull() ?: return@withContext null

        try {
            val charset = book.encodingName
                ?.let { runCatching { Charset.forName(it) }.getOrNull() }
                ?: Charsets.UTF_8
            val index = TxtChapterSplitter.split(file, charset)
            val chapters = index.chapters.mapIndexed { position, boundary ->
                Chapter(
                    bookId = book.id,
                    index = position,
                    title = boundary.title,
                    startByte = boundary.startByte,
                    endByte = boundary.endByte,
                    charStart = boundary.charStart,
                    charCount = boundary.charCount,
                )
            }
            repository.replaceChapters(book.id, chapters)
            chapters
        } finally {
            file.close()
        }
    }
}

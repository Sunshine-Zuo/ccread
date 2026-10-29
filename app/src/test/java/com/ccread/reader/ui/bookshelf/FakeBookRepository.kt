package com.ccread.reader.ui.bookshelf

import com.ccread.reader.data.repository.BookRepository
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBookRepository(initial: List<Book> = emptyList()) : BookRepository {

    val books = MutableStateFlow(initial)
    var lastDeleteOriginal: Boolean? = null

    override fun observeBooks(): Flow<List<Book>> = books

    override suspend fun getBook(id: Long): Book? = books.value.firstOrNull { it.id == id }

    override suspend fun addBook(book: Book, chapters: List<Chapter>): Long = 0

    override suspend fun updateBook(book: Book, chapters: List<Chapter>) {
        books.value = books.value.map { if (it.id == book.id) book else it }
    }

    override suspend fun updateLastRead(id: Long, at: Long) {
        books.value = books.value.map { if (it.id == id) it.copy(lastReadAt = at) else it }
    }

    override suspend fun saveProgress(id: Long, progress: ReadingProgress) = Unit

    override suspend fun deleteBook(book: Book, deleteOriginalFile: Boolean): Boolean {
        lastDeleteOriginal = deleteOriginalFile
        books.value = books.value.filterNot { it.id == book.id }
        return true
    }

    override suspend fun getChapters(bookId: Long): List<Chapter> = emptyList()

    override suspend fun replaceChapters(bookId: Long, chapters: List<Chapter>) = Unit

    override suspend fun findByUri(uri: String): Book? = books.value.firstOrNull { it.uri == uri }
}

fun testBook(id: Long, importedAt: Long, lastReadAt: Long?) = Book(
    id = id,
    title = "书$id",
    format = BookFormat.TXT,
    uri = "content://book/$id",
    sizeBytes = 1,
    importedAt = importedAt,
    lastReadAt = lastReadAt,
    progress = null,
    encodingName = "UTF-8",
    totalChars = 10,
)

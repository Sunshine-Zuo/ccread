package com.ccread.reader.ui.bookshelf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccread.reader.data.repository.BookRepository
import com.ccread.reader.domain.book.BookImporter
import com.ccread.reader.domain.book.ImportResult
import com.ccread.reader.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookshelfUiState(
    val books: List<Book> = emptyList(),
    val importing: Boolean = false,
    val message: String? = null,
    val missingBook: Book? = null,
    val openBookId: Long? = null,
)

class BookshelfViewModel(
    private val repository: BookRepository,
    private val importer: BookImporter? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val mutable = MutableStateFlow(BookshelfUiState())
    val state: StateFlow<BookshelfUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeBooks().collect { books ->
                mutable.update { it.copy(books = BookshelfSorting.sort(books)) }
            }
        }
    }

    fun import(uri: Uri) {
        val importer = importer ?: return
        if (mutable.value.importing) return
        viewModelScope.launch {
            mutable.update { it.copy(importing = true) }
            val result = importer.import(uri)
            mutable.update { it.copy(importing = false, message = result.message()) }
        }
    }

    fun importAndOpen(uri: Uri) {
        val importer = importer ?: return
        if (mutable.value.importing) return
        viewModelScope.launch {
            mutable.update { it.copy(importing = true) }
            val result = importer.import(uri)
            val openBookId = result.openedBookId()
            mutable.update {
                it.copy(
                    importing = false,
                    openBookId = openBookId,
                    message = if (openBookId == null) result.message() else null,
                )
            }
        }
    }

    fun consumeOpenBook() {
        mutable.update { it.copy(openBookId = null) }
    }

    fun openBook(id: Long) {
        viewModelScope.launch { repository.updateLastRead(id, clock()) }
    }

    fun relink(oldBook: Book, uri: Uri) {
        val importer = importer ?: return
        viewModelScope.launch {
            mutable.update { it.copy(importing = true) }
            val result = importer.relink(oldBook, uri)
            if (result is ImportResult.Success) {
                mutable.update { it.copy(importing = false, missingBook = null, message = null) }
            } else {
                mutable.update { it.copy(importing = false, message = result.message()) }
            }
        }
    }

    fun delete(book: Book, deleteOriginal: Boolean) {
        viewModelScope.launch {
            val originalDeleted = repository.deleteBook(book, deleteOriginal)
            mutable.update {
                it.copy(message = if (!originalDeleted) "无法删除原文件" else null)
            }
        }
    }

    fun onBookOpened(book: Book) {
        viewModelScope.launch { repository.updateLastRead(book.id, clock()) }
    }

    fun reportMissing(book: Book) {
        mutable.update { it.copy(missingBook = book) }
    }

    fun dismissMissing() {
        mutable.update { it.copy(missingBook = null) }
    }

    fun clearMessage() {
        mutable.update { it.copy(message = null) }
    }
}

private fun ImportResult.message(): String? = when (this) {
    is ImportResult.Success -> null
    is ImportResult.TooLarge -> "文件超过大小上限（TXT 200MB / EPUB 500MB）"
    ImportResult.UnsupportedFormat -> "只支持 TXT 和 EPUB 文件"
    ImportResult.NotSeekable -> "该文件无法直接读取，请先保存到手机本地存储"
    is ImportResult.Duplicate -> "这本书已经在书架上了"
    is ImportResult.Failed -> message
}

internal fun ImportResult.openedBookId(): Long? = when (this) {
    is ImportResult.Success -> bookId
    is ImportResult.Duplicate -> bookId
    else -> null
}

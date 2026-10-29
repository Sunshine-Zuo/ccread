package com.ccread.reader.ui.bookshelf

import com.ccread.reader.domain.model.Book

object BookshelfSorting {
    fun sort(books: List<Book>): List<Book> = books.sortedWith(
        compareByDescending<Book> { it.lastReadAt ?: it.importedAt }
            .thenByDescending { it.importedAt },
    )
}

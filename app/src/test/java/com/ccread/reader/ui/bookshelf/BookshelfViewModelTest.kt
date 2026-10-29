package com.ccread.reader.ui.bookshelf

import com.ccread.reader.domain.book.ImportResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BookshelfViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `recently read books come first and unread use import time`() {
        val books = listOf(
            testBook(id = 1, importedAt = 100, lastReadAt = null),
            testBook(id = 2, importedAt = 200, lastReadAt = null),
            testBook(id = 3, importedAt = 50, lastReadAt = 300),
        )

        assertEquals(listOf(3L, 2L, 1L), BookshelfSorting.sort(books).map { it.id })
    }

    @Test
    fun `delete without original file only removes from shelf`() = runTest {
        val repository = FakeBookRepository(listOf(testBook(id = 1, importedAt = 1, lastReadAt = null)))
        val viewModel = BookshelfViewModel(repository, importer = null)

        viewModel.delete(repository.books.value.first(), deleteOriginal = false)

        assertEquals(false, repository.lastDeleteOriginal)
        assertTrue(repository.books.value.isEmpty())
    }

    @Test
    fun `missing file keeps the book and exposes recovery state`() {
        val book = testBook(id = 1, importedAt = 1, lastReadAt = null)
        val repository = FakeBookRepository(listOf(book))
        val viewModel = BookshelfViewModel(repository, importer = null)

        viewModel.reportMissing(book)

        assertEquals(book, viewModel.state.value.missingBook)
        viewModel.dismissMissing()
        assertNull(viewModel.state.value.missingBook)
    }

    @Test
    fun `opening a book updates its last read time`() = runTest {
        val book = testBook(id = 1, importedAt = 1, lastReadAt = null)
        val repository = FakeBookRepository(listOf(book))
        val viewModel = BookshelfViewModel(repository, importer = null)

        viewModel.onBookOpened(book)

        assertFalse(repository.books.value.first().lastReadAt == null)
    }

    @Test
    fun `a new import opens the created book`() {
        assertEquals(7L, ImportResult.Success(7).openedBookId(duplicateId = null))
    }

    @Test
    fun `a duplicate import opens the book already on the shelf`() {
        assertEquals(3L, ImportResult.Duplicate.openedBookId(duplicateId = 3))
    }

    @Test
    fun `a failed import opens nothing`() {
        assertNull(ImportResult.Failed("坏了").openedBookId(duplicateId = 1))
        assertNull(ImportResult.Duplicate.openedBookId(duplicateId = null))
    }
}

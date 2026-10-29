package com.ccread.reader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ccread.reader.data.db.AppDatabase
import com.ccread.reader.data.db.BookEntity
import com.ccread.reader.data.db.ChapterEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BookDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun book(title: String, importedAt: Long, lastReadAt: Long?) = BookEntity(
        title = title,
        format = "TXT",
        uri = title,
        sizeBytes = 1,
        importedAt = importedAt,
        lastReadAt = lastReadAt,
        progressJson = null,
        encodingName = "UTF-8",
        totalChars = 10,
    )

    @Test
    fun `books are ordered by last read then import time`() = runTest {
        db.bookDao().insertBook(book("A", importedAt = 100, lastReadAt = null))
        db.bookDao().insertBook(book("B", importedAt = 200, lastReadAt = null))
        db.bookDao().insertBook(book("C", importedAt = 50, lastReadAt = 300))

        val titles = db.bookDao().observeAll().first().map { it.title }

        assertEquals(listOf("C", "B", "A"), titles)
    }

    @Test
    fun `deleting a book removes its chapters`() = runTest {
        val id = db.bookDao().insertBook(book("A", importedAt = 1, lastReadAt = null))
        db.bookDao().replaceChapters(
            id,
            listOf(
                ChapterEntity(bookId = id, chapterIndex = 0, title = "一", startByte = 0, endByte = 4, charStart = 0, charCount = 2),
                ChapterEntity(bookId = id, chapterIndex = 1, title = "二", startByte = 4, endByte = 8, charStart = 2, charCount = 2),
            ),
        )

        val stored = db.bookDao().getBook(id)!!
        db.bookDao().deleteBook(stored)

        assertTrue(db.bookDao().getChapters(id).isEmpty())
    }

    @Test
    fun `replacing chapters does not duplicate rows`() = runTest {
        val id = db.bookDao().insertBook(book("A", importedAt = 1, lastReadAt = null))
        db.bookDao().replaceChapters(id, listOf(ChapterEntity(bookId = id, chapterIndex = 0, title = "一", startByte = 0, endByte = 4, charStart = 0, charCount = 2)))
        db.bookDao().replaceChapters(id, listOf(ChapterEntity(bookId = id, chapterIndex = 0, title = "新一", startByte = 0, endByte = 4, charStart = 0, charCount = 2)))

        val chapters = db.bookDao().getChapters(id)
        assertEquals(1, chapters.size)
        assertEquals("新一", chapters.first().title)
    }

    @Test
    fun `updating a book keeps its id and new uri`() = runTest {
        val id = db.bookDao().insertBook(book("A", importedAt = 1, lastReadAt = null))
        val stored = db.bookDao().getBook(id)!!

        db.bookDao().updateBook(stored.copy(uri = "content://new", sizeBytes = 99))

        val updated = db.bookDao().getBook(id)!!
        assertEquals("content://new", updated.uri)
        assertEquals(99, updated.sizeBytes)
    }
}

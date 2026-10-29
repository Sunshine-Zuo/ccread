package com.ccread.reader.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY COALESCE(lastReadAt, importedAt) DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBook(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): BookEntity?

    @Insert
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET lastReadAt = :at WHERE id = :id")
    suspend fun updateLastRead(id: Long, at: Long)

    @Query("UPDATE books SET progressJson = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: String?)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY chapterIndex")
    suspend fun getChapters(bookId: Long): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChapters(bookId: Long)

    @Transaction
    suspend fun insertBookWithChapters(book: BookEntity, chapters: List<ChapterEntity>): Long {
        val id = insertBook(book)
        if (chapters.isNotEmpty()) {
            insertChapters(chapters.map { it.copy(bookId = id) })
        }
        return id
    }

    @Transaction
    suspend fun replaceChapters(bookId: Long, chapters: List<ChapterEntity>) {
        deleteChapters(bookId)
        insertChapters(chapters)
    }
}

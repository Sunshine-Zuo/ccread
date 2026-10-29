package com.ccread.reader.data.repository

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.ccread.reader.data.db.BookDao
import com.ccread.reader.data.db.BookEntity
import com.ccread.reader.data.db.ChapterEntity
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.ProgressCodec
import com.ccread.reader.domain.model.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun getBook(id: Long): Book?
    suspend fun addBook(book: Book, chapters: List<Chapter>): Long
    suspend fun updateBook(book: Book, chapters: List<Chapter>)
    suspend fun updateLastRead(id: Long, at: Long)
    suspend fun saveProgress(id: Long, progress: ReadingProgress)
    suspend fun deleteBook(book: Book, deleteOriginalFile: Boolean): Boolean
    suspend fun getChapters(bookId: Long): List<Chapter>
    suspend fun replaceChapters(bookId: Long, chapters: List<Chapter>)
    suspend fun findByUri(uri: String): Book?
}

class RoomBookRepository(
    private val dao: BookDao,
    private val contentResolver: ContentResolver,
) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> =
        dao.observeAll().map { books -> books.map { it.toDomain() } }

    override suspend fun getBook(id: Long): Book? = dao.getBook(id)?.toDomain()

    override suspend fun addBook(book: Book, chapters: List<Chapter>): Long =
        dao.insertBookWithChapters(
            book.toEntity(),
            chapters.map { it.copy(bookId = 0).toEntity() },
        )

    override suspend fun updateBook(book: Book, chapters: List<Chapter>) {
        dao.updateBook(book.toEntity())
        if (chapters.isNotEmpty()) {
            dao.replaceChapters(book.id, chapters.map { it.copy(bookId = book.id).toEntity() })
        }
    }

    override suspend fun updateLastRead(id: Long, at: Long) = dao.updateLastRead(id, at)

    override suspend fun saveProgress(id: Long, progress: ReadingProgress) =
        dao.updateProgress(id, ProgressCodec.encode(progress))

    override suspend fun deleteBook(book: Book, deleteOriginalFile: Boolean): Boolean {
        val entity = dao.getBook(book.id) ?: return !deleteOriginalFile
        val originalDeleted = if (deleteOriginalFile) deleteOriginal(book.uri) else true
        dao.deleteBook(entity)
        releasePermission(book.uri)
        return originalDeleted
    }

    override suspend fun getChapters(bookId: Long): List<Chapter> =
        dao.getChapters(bookId).map { it.toDomain() }

    override suspend fun replaceChapters(bookId: Long, chapters: List<Chapter>) =
        dao.replaceChapters(bookId, chapters.map { it.copy(bookId = bookId).toEntity() })

    override suspend fun findByUri(uri: String): Book? = dao.findByUri(uri)?.toDomain()

    private fun deleteOriginal(uri: String): Boolean = runCatching {
        DocumentsContract.deleteDocument(contentResolver, Uri.parse(uri))
    }.getOrDefault(false)

    private fun releasePermission(uri: String) {
        runCatching {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
    }
}

private fun BookEntity.toDomain() = Book(
    id = id,
    title = title,
    format = runCatching { BookFormat.valueOf(format) }.getOrDefault(BookFormat.TXT),
    uri = uri,
    sizeBytes = sizeBytes,
    importedAt = importedAt,
    lastReadAt = lastReadAt,
    progress = ProgressCodec.decode(progressJson),
    encodingName = encodingName,
    totalChars = totalChars,
)

private fun Book.toEntity() = BookEntity(
    id = id,
    title = title,
    format = format.name,
    uri = uri,
    sizeBytes = sizeBytes,
    importedAt = importedAt,
    lastReadAt = lastReadAt,
    progressJson = progress?.let { ProgressCodec.encode(it) },
    encodingName = encodingName,
    totalChars = totalChars,
)

private fun ChapterEntity.toDomain() = Chapter(
    bookId = bookId,
    index = chapterIndex,
    title = title,
    startByte = startByte,
    endByte = endByte,
    charStart = charStart,
    charCount = charCount,
)

private fun Chapter.toEntity() = ChapterEntity(
    bookId = bookId,
    chapterIndex = index,
    title = title,
    startByte = startByte,
    endByte = endByte,
    charStart = charStart,
    charCount = charCount,
)

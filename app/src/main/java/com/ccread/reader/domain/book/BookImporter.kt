package com.ccread.reader.domain.book

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.ccread.reader.data.file.BookFileStore
import com.ccread.reader.data.file.ContentUriSeekableFile
import com.ccread.reader.data.repository.BookRepository
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.BookFormat
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.TxtProgress
import com.ccread.reader.domain.progress.TxtProgressRemap
import com.ccread.reader.domain.txt.CharsetDetector
import com.ccread.reader.domain.txt.SeekableFile
import com.ccread.reader.domain.txt.TxtChapterSplitter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookImporter(
    private val context: Context,
    private val repository: BookRepository,
) {

    private val fileStore = BookFileStore(context)

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val (fileName, reportedSize) = queryFile(resolver, uri)
                ?: return@withContext ImportResult.Failed("无法读取文件信息")
            val format = ImportRules.formatOf(fileName, resolver.getType(uri))
                ?: return@withContext ImportResult.UnsupportedFormat

            repository.findByUri(uri.toString())?.let { existing ->
                return@withContext ImportResult.Duplicate(existing.id)
            }

            // 微信等应用通过 ACTION_VIEW 传来的 URI 往往只带临时读权限，
            // 无法持久化；此时复制到应用私有目录，否则进程结束后文件就再也打不开。
            val (storedUri, needsCopy) = storageTarget(resolver, uri, format)
            repository.findByUri(storedUri.toString())?.let { existing ->
                return@withContext ImportResult.Duplicate(existing.id)
            }
            if (needsCopy && !fileStore.copy(uri, storedUri)) {
                return@withContext ImportResult.Failed("无法保存文件副本")
            }

            val seekable = openSeekable(storedUri) ?: return@withContext ImportResult.NotSeekable
            seekable.use {
                val limit = ImportRules.limitBytes(format)
                if (reportedSize != null && reportedSize > limit) {
                    return@withContext ImportResult.TooLarge(limit)
                }
                if (seekable.sizeBytes > limit) {
                    return@withContext ImportResult.TooLarge(limit)
                }
                if (seekable.sizeBytes <= 0) {
                    return@withContext ImportResult.Failed("文件为空")
                }

                val now = System.currentTimeMillis()
                val book = Book(
                    id = 0,
                    title = ImportRules.titleOf(fileName),
                    format = format,
                    uri = storedUri.toString(),
                    sizeBytes = seekable.sizeBytes,
                    importedAt = now,
                    lastReadAt = null,
                    progress = null,
                    encodingName = null,
                    totalChars = null,
                )

                when (format) {
                    BookFormat.TXT -> importTxt(book, seekable)
                    BookFormat.EPUB -> {
                        val id = repository.addBook(book, emptyList())
                        ImportResult.Success(id)
                    }
                }
            }
        } catch (t: Throwable) {
            ImportResult.Failed(t.message ?: "导入失败")
        }
    }

    suspend fun relink(book: Book, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val (fileName, reportedSize) = queryFile(resolver, uri)
                ?: return@withContext ImportResult.Failed("无法读取文件信息")
            val format = ImportRules.formatOf(fileName, resolver.getType(uri))
                ?: return@withContext ImportResult.UnsupportedFormat
            if (format != book.format) {
                return@withContext ImportResult.Failed("文件格式与原书不一致")
            }

            val (storedUri, needsCopy) = storageTarget(resolver, uri, format)
            if (needsCopy && !fileStore.copy(uri, storedUri)) {
                return@withContext ImportResult.Failed("无法保存文件副本")
            }
            val seekable = openSeekable(storedUri) ?: return@withContext ImportResult.NotSeekable

            seekable.use {
                val limit = ImportRules.limitBytes(format)
                if ((reportedSize ?: seekable.sizeBytes) > limit) {
                    return@withContext ImportResult.TooLarge(limit)
                }
                if (seekable.sizeBytes <= 0) {
                    return@withContext ImportResult.Failed("文件为空")
                }

                when (format) {
                    BookFormat.TXT -> {
                        val prefixLength = minOf(64L * 1024, seekable.sizeBytes).toInt()
                        val charset = CharsetDetector.detect(seekable.readAt(0, prefixLength))
                        val index = TxtChapterSplitter.split(seekable, charset)
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
                        val remapped = TxtProgressRemap.remap(
                            book.progress as? TxtProgress,
                            index.totalChars,
                            chapters,
                        )
                        repository.updateBook(
                            book.copy(
                                title = ImportRules.titleOf(fileName),
                                uri = storedUri.toString(),
                                sizeBytes = seekable.sizeBytes,
                                encodingName = charset.name(),
                                totalChars = index.totalChars,
                                progress = remapped,
                            ),
                            chapters,
                        )
                        discardOldCopy(book, storedUri)
                        ImportResult.Success(book.id)
                    }

                    BookFormat.EPUB -> {
                        repository.updateBook(
                            book.copy(
                                title = ImportRules.titleOf(fileName),
                                uri = storedUri.toString(),
                                sizeBytes = seekable.sizeBytes,
                            ),
                            emptyList(),
                        )
                        discardOldCopy(book, storedUri)
                        ImportResult.Success(book.id)
                    }
                }
            }
        } catch (t: Throwable) {
            ImportResult.Failed(t.message ?: "重新关联失败")
        }
    }

    private fun persistPermission(resolver: ContentResolver, uri: Uri): Boolean {
        val readWrite = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        if (runCatching { resolver.takePersistableUriPermission(uri, readWrite) }.isSuccess) {
            return true
        }
        return runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.isSuccess
    }

    /** 能持久化授权时直接引用原文件，否则返回应用私有副本的位置。 */
    private fun storageTarget(
        resolver: ContentResolver,
        uri: Uri,
        format: BookFormat,
    ): Pair<Uri, Boolean> {
        if (uri.scheme == "file" || persistPermission(resolver, uri)) return uri to false
        return fileStore.targetFor(uri, format) to true
    }

    private fun discardOldCopy(book: Book, newUri: Uri) {
        if (book.uri != newUri.toString()) fileStore.deleteOwned(Uri.parse(book.uri))
    }

    private suspend fun importTxt(book: Book, seekable: SeekableFile): ImportResult {
        val prefixLength = minOf(64L * 1024, seekable.sizeBytes).toInt()
        val charset = CharsetDetector.detect(seekable.readAt(0, prefixLength))
        val index = TxtChapterSplitter.split(seekable, charset)
        val chapters = index.chapters.mapIndexed { position, boundary ->
            Chapter(
                bookId = 0,
                index = position,
                title = boundary.title,
                startByte = boundary.startByte,
                endByte = boundary.endByte,
                charStart = boundary.charStart,
                charCount = boundary.charCount,
            )
        }
        val id = repository.addBook(
            book.copy(encodingName = charset.name(), totalChars = index.totalChars),
            chapters,
        )
        return ImportResult.Success(id)
    }

    private fun openSeekable(uri: Uri): SeekableFile? {
        val file = runCatching { ContentUriSeekableFile(context, uri) }.getOrNull() ?: return null
        return runCatching {
            if (file.sizeBytes > 0) file.readAt(0, 1)
            file
        }.getOrElse {
            file.close()
            null
        }
    }

    private fun queryFile(resolver: ContentResolver, uri: Uri): Pair<String, Long?>? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val name = if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    cursor.getString(nameIndex)
                } else {
                    uri.lastPathSegment ?: "未命名"
                }
                val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                return name to size
            }
        }
        val fallbackName = uri.lastPathSegment ?: return null
        return fallbackName to null
    }
}

private inline fun SeekableFile.use(block: (SeekableFile) -> ImportResult): ImportResult =
    try {
        block(this)
    } finally {
        close()
    }

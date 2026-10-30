package com.ccread.reader.data.file

import android.content.Context
import android.net.Uri
import com.ccread.reader.domain.model.BookFormat
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * 应用私有目录中的书籍副本。
 *
 * 通过 ACTION_VIEW 打开的文件（例如微信“用其他应用打开”）通常只带临时读权限，
 * takePersistableUriPermission 会失败；应用进程结束后授权即失效。此时把内容复制到
 * 应用私有目录，后续不再依赖外部授权。
 */
class BookFileStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, "books")

    fun targetFor(source: Uri, format: BookFormat): Uri {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(source.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return Uri.fromFile(File(directory, "$digest.${format.name.lowercase()}"))
    }

    fun copy(source: Uri, target: Uri): Boolean {
        val file = File(target.path ?: return false)
        val temp = File(file.parentFile, "${file.name}.tmp")
        return try {
            directory.mkdirs()
            context.contentResolver.openInputStream(source)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            if (file.exists() && !file.delete()) return false
            val renamed = temp.renameTo(file)
            if (!renamed) temp.delete()
            renamed
        } catch (_: IOException) {
            temp.delete()
            false
        }
    }

    fun owns(uri: Uri): Boolean {
        if (uri.scheme != "file") return false
        val path = uri.path ?: return false
        return runCatching {
            File(path).canonicalFile.parentFile == directory.canonicalFile
        }.getOrDefault(false)
    }

    fun deleteOwned(uri: Uri): Boolean {
        if (!owns(uri)) return true
        val path = uri.path ?: return false
        return runCatching {
            val file = File(path)
            file.delete() || !file.exists()
        }.getOrDefault(false)
    }
}

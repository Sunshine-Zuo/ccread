package com.ccread.reader.ui.bookshelf

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ccread.reader.domain.book.FileLocation
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.BookFormat
import java.util.Locale

@Composable
fun BookDetailsDialog(book: Book, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val location = FileLocation.resolve(context, book.uri)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(book.title) },
        text = {
            Text(
                buildString {
                    appendLine("格式：${book.format.name}")
                    appendLine("大小：${formatSize(book.sizeBytes)}")
                    append("本地文件位置：$location")
                },
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    if (location.startsWith("/storage/emulated/0/")) {
                        val relative = location.removePrefix("/storage/emulated/0/")
                        val parent = relative.substringBeforeLast('/', "")
                        val documentId = if (parent.isEmpty()) "primary:" else "primary:$parent"
                        val folderUri = DocumentsContract.buildDocumentUri(
                            "com.android.externalstorage.documents",
                            documentId,
                        )
                        val folderIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(folderUri, DocumentsContract.Document.MIME_TYPE_DIR)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        if (runCatching { context.startActivity(folderIntent) }.isSuccess) {
                            return@TextButton
                        }
                    }
                    val mime = if (book.format == BookFormat.EPUB) {
                        "application/epub+zip"
                    } else {
                        "text/plain"
                    }
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(book.uri), mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val opened = runCatching { context.startActivity(intent) }.isSuccess
                    if (!opened) {
                        Toast.makeText(context, "系统无法打开该文件位置", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("在系统中打开")
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
    )
}

@Composable
fun MissingFileDialog(
    book: Book,
    onDismiss: () -> Unit,
    onRelink: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("找不到原文件") },
        text = { Text("《${book.title}》的原文件可能已被移动或删除。你可以重新选择文件，或把它移出书架。") },
        confirmButton = {
            TextButton(onClick = onRelink) { Text("重新选择文件") }
        },
        dismissButton = {
            TextButton(onClick = onRemove) { Text("从书架移除") }
        },
    )
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

package com.ccread.reader.ui.bookshelf

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ccread.reader.domain.book.FileLocation
import com.ccread.reader.domain.model.Book
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailsDialog(book: Book, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val location = FileLocation.resolve(context, book.uri)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            Column {
                DetailRow("格式", book.format.name)
                DetailRow("大小", formatSize(book.sizeBytes))
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        0.7.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "本地文件位置",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("关闭") }
                FilledTonalButton(
                    onClick = {
                        val intent = folderIntent(location, book.uri)
                        if (intent == null) {
                            val message = if (
                                book.uri.startsWith("file://") &&
                                location.startsWith("/data/")
                            ) {
                                "文件已复制到应用内部存储，系统文件管理器无法打开"
                            } else {
                                "无法定位文件所在目录"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        } else if (runCatching { context.startActivity(intent) }.isFailure) {
                            Toast.makeText(context, "系统无法打开文件所在目录", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                ) {
                    Text("打开目录")
                }
            }
        },
    )
}

internal fun folderIntent(location: String, uriString: String): Intent? {
    fileSystemFolderIntent(location)?.let { return it }
    return contentFolderIntent(uriString)
}

private fun fileSystemFolderIntent(location: String): Intent? {
    val primaryRelative = when {
        location == "/sdcard" || location == "/mnt/sdcard" -> ""
        location.startsWith("/sdcard/") -> location.removePrefix("/sdcard/")
        location.startsWith("/mnt/sdcard/") -> location.removePrefix("/mnt/sdcard/")
        location.startsWith("/mnt/user/0/primary/") -> location.removePrefix("/mnt/user/0/primary/")
        location.startsWith("/data/media/0/") -> location.removePrefix("/data/media/0/")
        location.startsWith("/storage/self/primary/") -> location.removePrefix("/storage/self/primary/")
        location.startsWith("/storage/emulated/0/") -> location.removePrefix("/storage/emulated/0/")
        else -> null
    }
    if (primaryRelative != null) {
        val parent = primaryRelative.substringBeforeLast('/', "")
        val documentId = if (parent.isEmpty()) "primary:" else "primary:$parent"
        return folderViewIntent(
            DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                documentId,
            ),
        )
    }
    if (location == "/storage/emulated/0" ||
        location == "/mnt/user/0/primary" ||
        location == "/data/media/0" ||
        location == "/storage/self/primary"
    ) {
        return folderViewIntent(
            DocumentsContract.buildDocumentUri(
                "com.android.externalstorage.documents",
                "primary:",
            ),
        )
    }
    if (!location.startsWith("/storage/")) return null
    val rest = location.removePrefix("/storage/")
    val volume = rest.substringBefore('/')
    if (volume.isEmpty()) return null
    val parent = rest.substringAfter('/', "").substringBeforeLast('/', "")
    val documentId = if (parent.isEmpty()) "$volume:" else "$volume:$parent"
    return folderViewIntent(
        DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            documentId,
        ),
    )
}

private fun contentFolderIntent(uriString: String): Intent? {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    if (uri.scheme != "content") return null
    val authority = uri.authority ?: return null
    val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null

    if (authority == "com.android.providers.downloads.documents") {
        // Downloads 提供方不暴露文件夹 document，退到 Download 根目录
        return folderViewIntent(DocumentsContract.buildRootUri(authority, "downloads"))
    }

    val separator = documentId.indexOf(':')
    if (separator <= 0) return null
    val prefix = documentId.substring(0, separator)
    val path = documentId.substring(separator + 1)
    val parent = path.substringBeforeLast('/', "")
    val parentId = if (parent.isEmpty()) "$prefix:" else "$prefix:$parent"
    return folderViewIntent(DocumentsContract.buildDocumentUri(authority, parentId))
}

private fun folderViewIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingFileDialog(
    book: Book,
    onDismiss: () -> Unit,
    onRelink: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = { Text("找不到原文件", style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                text = "《${book.title}》的原文件可能已被移动或删除。你可以重新选择文件，或把它移出书架。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            FilledTonalButton(onClick = onRelink, shape = RoundedCornerShape(6.dp)) {
                Text("重新选择文件")
            }
        },
        dismissButton = {
            TextButton(onClick = onRemove) {
                Text("从书架移除", color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

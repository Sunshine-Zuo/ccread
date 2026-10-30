package com.ccread.reader.ui.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ccread.reader.domain.model.Book

@Composable
fun DeleteBookDialog(
    book: Book,
    onDismiss: () -> Unit,
    onConfirm: (deleteOriginal: Boolean) -> Unit,
) {
    var deleteOriginal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        title = { Text("删除书籍", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    text = "确认从书架删除《${book.title}》？",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteOriginal = !deleteOriginal }
                            .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = deleteOriginal,
                            onCheckedChange = { deleteOriginal = it },
                        )
                        Text(
                            text = "同时删除本地文件",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(deleteOriginal) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

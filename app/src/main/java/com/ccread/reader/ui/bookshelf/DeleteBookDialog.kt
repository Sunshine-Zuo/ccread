package com.ccread.reader.ui.bookshelf

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
        title = { Text("删除《${book.title}》") },
        text = {
            Column {
                Text("确认从书架删除这本书？")
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = deleteOriginal, onCheckedChange = { deleteOriginal = it })
                    Text("同时删除本地文件")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(deleteOriginal) }) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

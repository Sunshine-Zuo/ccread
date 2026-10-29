package com.ccread.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TocDrawer(
    bookTitle: String,
    rows: List<TocRow>,
    currentKey: String?,
    onSelect: (TocRow) -> Unit,
    onToggle: (TocRow) -> Unit,
    onResplit: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(currentKey, rows) {
        val index = rows.indexOfFirst { it.key == currentKey }
        if (index >= 0) {
            listState.scrollToItem(index)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.8f)
                .align(Alignment.CenterStart),
            tonalElevation = 8.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = bookTitle,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }

                if (onResplit != null) {
                    TextButton(
                        onClick = onResplit,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) {
                        Text("重新识别")
                    }
                }

                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.key }) { row ->
                        val selected = row.key == currentKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent,
                                )
                                .clickable { onSelect(row) }
                                .padding(
                                    start = (16 + row.depth * 16).dp,
                                    end = 12.dp,
                                    top = 12.dp,
                                    bottom = 12.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (row.expandable) {
                                Text(
                                    text = if (row.expanded) "▾" else "▸",
                                    modifier = Modifier
                                        .width(24.dp)
                                        .clickable { onToggle(row) },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = row.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

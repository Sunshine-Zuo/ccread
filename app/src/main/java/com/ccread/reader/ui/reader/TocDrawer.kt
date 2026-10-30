package com.ccread.reader.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.ui.theme.JingduIcons

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
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.82f)
                .align(Alignment.CenterStart),
            shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 20.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
            ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bookTitle,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 0.7.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = JingduIcons.Close,
                                contentDescription = "关闭目录",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }

                    if (onResplit != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(
                                    width = 0.7.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(5.dp),
                                )
                                .clickable(onClick = onResplit)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "重新识别",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }

                HorizontalDivider(
                    thickness = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    items(rows, key = { it.key }) { row ->
                        val selected = row.key == currentKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                )
                                .clickable { onSelect(row) }
                                .padding(
                                    start = (12 + row.depth * 16).dp,
                                    end = 12.dp,
                                    top = 10.dp,
                                    bottom = 10.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (row.expandable) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { onToggle(row) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = JingduIcons.ChevronDown,
                                        contentDescription = if (row.expanded) "收起" else "展开",
                                        tint = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(if (row.expanded) 0f else -90f),
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = row.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                ),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

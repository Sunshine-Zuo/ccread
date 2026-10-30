package com.ccread.reader.ui.bookshelf

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.domain.model.Book
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val progress = (book.progress?.percent ?: 0.0).toFloat().coerceIn(0f, 100f)
    val ink = MaterialTheme.colorScheme.onSurface
    val coverStart = MaterialTheme.colorScheme.surface
    val coverEnd = MaterialTheme.colorScheme.surfaceContainerLow
    val pageStart = MaterialTheme.colorScheme.surfaceContainerHigh
    val pageEnd = MaterialTheme.colorScheme.surfaceContainerHighest
    val spineDark = MaterialTheme.colorScheme.surfaceContainerHighest
    val spineLight = MaterialTheme.colorScheme.surfaceContainerHigh
    val foldLight = MaterialTheme.colorScheme.surfaceContainerLowest
    val foldDark = MaterialTheme.colorScheme.surfaceContainerHigh
    val sealColor = MaterialTheme.colorScheme.primary
    val titleText = book.title.trim()
    val coverShape = RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 5.dp,
        bottomEnd = 5.dp,
        bottomStart = 2.dp,
    )
    val pageShape = RoundedCornerShape(
        topStart = 2.dp,
        topEnd = 4.dp,
        bottomEnd = 4.dp,
        bottomStart = 2.dp,
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f),
        ) {
            // 页口：封面右侧与底部露出的纸张厚度
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 8.dp, bottom = 6.dp)
                    .offset(x = 5.dp, y = 4.dp)
                    .clip(pageShape)
                    .background(Brush.verticalGradient(listOf(pageStart, pageEnd)))
                    .drawBehind {
                        val step = 2.6.dp.toPx()
                        val left = size.width - 5.dp.toPx()
                        val right = size.width - 1.dp.toPx()
                        var y = 1.dp.toPx()
                        var index = 0
                        while (y < size.height) {
                            drawLine(
                                color = ink.copy(alpha = if (index % 3 == 0) 0.16f else 0.09f),
                                start = Offset(left, y),
                                end = Offset(right, y),
                                strokeWidth = 0.5.dp.toPx(),
                            )
                            y += step
                            index++
                        }
                    },
            )

            // 封面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 7.dp, bottom = 5.dp)
                    .shadow(3.dp, coverShape)
                    .clip(coverShape)
                    .background(Brush.linearGradient(listOf(coverStart, coverEnd)))
                    .border(0.8.dp, ink.copy(alpha = 0.42f), coverShape)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuOpen = true },
                    ),
            ) {
                // 折角：翻开的一小块纸背
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp),
                ) {
                    val width = size.width
                    val height = size.height
                    val foldPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(width, 0f)
                        lineTo(width, height)
                        close()
                    }
                    drawPath(
                        path = foldPath,
                        brush = Brush.linearGradient(
                            colors = listOf(foldLight, foldDark),
                            start = Offset(0f, 0f),
                            end = Offset(width, height),
                        ),
                    )
                    drawLine(
                        color = ink.copy(alpha = 0.08f),
                        start = Offset(0f, 1.dp.toPx()),
                        end = Offset(width - 1.dp.toPx(), height),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawLine(
                        color = ink.copy(alpha = 0.32f),
                        start = Offset(0f, 0f),
                        end = Offset(width, height),
                        strokeWidth = 0.6.dp.toPx(),
                    )
                }

                Row(modifier = Modifier.fillMaxSize()) {
                    // 书脊：中间略亮，两侧压暗，做出圆脊的错觉
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(9.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(spineDark, spineLight, spineDark),
                                ),
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(0.8.dp)
                                .background(ink.copy(alpha = 0.18f)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 13.dp)
                                .width(4.dp)
                                .height(1.dp)
                                .background(ink.copy(alpha = 0.26f)),
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 13.dp)
                                .width(4.dp)
                                .height(1.dp)
                                .background(ink.copy(alpha = 0.26f)),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        // 封面包边线
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .border(
                                    width = 0.4.dp,
                                    color = ink.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(1.dp),
                                ),
                        )

                        // 书名直接显示在封面上
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                letterSpacing = 0.4.sp,
                            ),
                            color = ink,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 12.dp, top = 13.dp, end = 20.dp),
                        )

                        // 底部信息
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 9.dp, bottom = 9.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(ink.copy(alpha = 0.18f)),
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = book.format.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        letterSpacing = 0.7.sp,
                                    ),
                                    color = ink.copy(alpha = 0.55f),
                                )
                                if (progress > 0f) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${progress.roundToInt()}%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            letterSpacing = 0.sp,
                                            fontFeatureSettings = "tnum",
                                        ),
                                        color = ink.copy(alpha = 0.55f),
                                    )
                                }
                            }
                            if (progress > 0f) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(ink.copy(alpha = 0.12f)),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress / 100f)
                                            .fillMaxHeight()
                                            .background(sealColor),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = RoundedCornerShape(8.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 10.dp,
        ) {
            DropdownMenuItem(
                text = { Text("详情", style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    menuOpen = false
                    onDetails()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "删除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

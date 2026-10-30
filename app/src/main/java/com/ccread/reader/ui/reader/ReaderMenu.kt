package com.ccread.reader.ui.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ccread.reader.ui.theme.JingduIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderMenu(
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    canPreviousChapter: Boolean,
    canNextChapter: Boolean,
    onToc: () -> Unit,
    onSettings: () -> Unit,
    onBrightness: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        border = BorderStroke(
            0.8.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChapterButton(
                    label = "上一章",
                    icon = JingduIcons.ChevronLeft,
                    enabled = canPreviousChapter,
                    onClick = onPreviousChapter,
                    modifier = Modifier.weight(1f),
                    iconAtEnd = false,
                )
                ChapterButton(
                    label = "下一章",
                    icon = JingduIcons.ChevronRight,
                    enabled = canNextChapter,
                    onClick = onNextChapter,
                    modifier = Modifier.weight(1f),
                    iconAtEnd = true,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MenuAction(
                    label = "目录",
                    icon = JingduIcons.List,
                    onClick = onToc,
                    modifier = Modifier.weight(1f),
                )
                MenuAction(
                    label = "阅读设置",
                    icon = null,
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                )
                MenuAction(
                    label = "亮度",
                    icon = JingduIcons.Sun,
                    onClick = onBrightness,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconAtEnd: Boolean,
) {
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (enabled) 1f else 0.45f,
        ),
        contentColor = contentColor,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 1f else 0.5f),
        ),
        modifier = modifier.height(38.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    letterSpacing = 0.6.sp,
                ),
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(if (iconAtEnd) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 12.dp)
                    .size(15.dp),
                tint = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (enabled) 1f else 0.38f,
                ),
            )
        }
    }
}

@Composable
private fun MenuAction(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(4.dp),
                )
                .border(
                    width = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            } else {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.ccread.reader.ui.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderTopBar(
    chapterTitle: String,
    time: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            text = chapterTitle,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 64.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = time,
            modifier = Modifier.align(Alignment.CenterEnd),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = "tnum",
                letterSpacing = 0.sp,
            ),
            color = color,
        )
    }
}

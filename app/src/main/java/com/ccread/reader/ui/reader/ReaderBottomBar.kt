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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderBottomBar(percentText: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = percentText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = "tnum",
                letterSpacing = 0.4.sp,
                fontSize = 11.sp,
            ),
            color = color,
        )
    }
}

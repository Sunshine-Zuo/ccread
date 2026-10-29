package com.ccread.reader.ui.reader

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderMenu(
    onToc: () -> Unit,
    onSettings: () -> Unit,
    onBrightness: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
        ) {
            TextButton(onClick = onToc, modifier = Modifier.weight(1f)) { Text("目录") }
            TextButton(onClick = onSettings, modifier = Modifier.weight(1f)) { Text("阅读设置") }
            TextButton(onClick = onBrightness, modifier = Modifier.weight(1f)) { Text("亮度") }
        }
    }
}

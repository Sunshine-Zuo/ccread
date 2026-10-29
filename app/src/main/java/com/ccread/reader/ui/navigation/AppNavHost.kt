package com.ccread.reader.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ccread.reader.ui.bookshelf.BookshelfScreen
import com.ccread.reader.ui.reader.ReaderScreen

@Composable
fun AppNavHost(
    incomingUri: Uri? = null,
    onIncomingConsumed: () -> Unit = {},
) {
    var readerBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    var missingBookId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(incomingUri) {
        if (incomingUri != null) readerBookId = null
    }

    val bookId = readerBookId
    if (bookId == null) {
        BookshelfScreen(
            onOpenBook = { readerBookId = it },
            missingBookId = missingBookId,
            onMissingConsumed = { missingBookId = null },
            incomingUri = incomingUri,
            onIncomingConsumed = onIncomingConsumed,
        )
    } else {
        ReaderScreen(
            bookId = bookId,
            onBack = { readerBookId = null },
            onMissingFile = { missingBookId = bookId },
        )
    }
}

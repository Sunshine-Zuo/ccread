package com.ccread.reader.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ccread.reader.JingduApp
import com.ccread.reader.domain.model.Book

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onOpenBook: (Long) -> Unit,
    missingBookId: Long? = null,
    onMissingConsumed: () -> Unit = {},
    incomingUri: Uri? = null,
    onIncomingConsumed: () -> Unit = {},
) {
    val app = LocalContext.current.applicationContext as JingduApp
    val viewModel: BookshelfViewModel = viewModel(
        factory = viewModelFactory {
            initializer { BookshelfViewModel(app.bookRepository, app.bookImporter) }
        },
    )
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var deleteTarget by remember { mutableStateOf<Book?>(null) }
    var detailsTarget by remember { mutableStateOf<Book?>(null) }
    var relinkTarget by remember { mutableStateOf<Book?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.import(uri)
    }
    val relinkLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val old = relinkTarget
        if (uri != null && old != null) viewModel.relink(old, uri)
        relinkTarget = null
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearMessage()
    }

    LaunchedEffect(state.books, missingBookId) {
        val id = missingBookId ?: return@LaunchedEffect
        state.books.firstOrNull { it.id == id }?.let { book ->
            viewModel.reportMissing(book)
            onMissingConsumed()
        }
    }

    LaunchedEffect(incomingUri) {
        val uri = incomingUri ?: return@LaunchedEffect
        viewModel.importAndOpen(uri)
        onIncomingConsumed()
    }

    LaunchedEffect(state.openBookId) {
        val id = state.openBookId ?: return@LaunchedEffect
        viewModel.consumeOpenBook()
        viewModel.openBook(id)
        onOpenBook(id)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("书架", fontWeight = FontWeight.SemiBold) },
                actions = {
                    TextButton(
                        enabled = !state.importing,
                        onClick = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                    ) {
                        Text("+", style = MaterialTheme.typography.headlineSmall)
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.books.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有书，点右上角 + 导入", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = {
                                viewModel.onBookOpened(book)
                                onOpenBook(book.id)
                            },
                            onDelete = { deleteTarget = book },
                            onDetails = { detailsTarget = book },
                        )
                    }
                }
            }

            if (state.importing) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "正在导入并解析…",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }

    deleteTarget?.let { book ->
        DeleteBookDialog(
            book = book,
            onDismiss = { deleteTarget = null },
            onConfirm = { deleteOriginal ->
                viewModel.delete(book, deleteOriginal)
                deleteTarget = null
            },
        )
    }

    detailsTarget?.let { book ->
        BookDetailsDialog(book = book, onDismiss = { detailsTarget = null })
    }

    state.missingBook?.let { book ->
        MissingFileDialog(
            book = book,
            onDismiss = viewModel::dismissMissing,
            onRelink = {
                relinkTarget = book
                relinkLauncher.launch(arrayOf("*/*"))
            },
            onRemove = {
                viewModel.delete(book, deleteOriginal = false)
                viewModel.dismissMissing()
            },
        )
    }
}

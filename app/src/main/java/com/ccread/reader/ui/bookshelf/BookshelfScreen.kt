package com.ccread.reader.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ccread.reader.JingduApp
import com.ccread.reader.domain.model.Book
import com.ccread.reader.ui.theme.JingduIcons

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
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(6.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    actionColor = MaterialTheme.colorScheme.inversePrimary,
                )
            }
        },
        topBar = {
            BookshelfHeader(
                bookCount = state.books.size,
                importing = state.importing,
                onImport = { importLauncher.launch(arrayOf("*/*")) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.books.isEmpty()) {
                EmptyBookshelf()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 24.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
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
                            modifier = Modifier.animateItem(),
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookshelfHeader(
    bookCount: Int,
    importing: Boolean,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "书架",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alignByBaseline(),
            )
            if (importing) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = buildAnnotatedString {
                        appendInlineContent("spinner", " ")
                        append(" 正在导入并解析…")
                    },
                    inlineContent = mapOf(
                        "spinner" to InlineTextContent(
                            Placeholder(
                                width = 14.sp,
                                height = 14.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            ),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alignByBaseline(),
                )
            } else if (bookCount > 0) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "共 $bookCount 册",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                onClick = onImport,
                enabled = !importing,
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                ),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Icon(
                            imageVector = JingduIcons.Plus,
                            contentDescription = "添加书籍",
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (importing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                strokeCap = StrokeCap.Round,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
            )
        }
    }
}

@Composable
private fun EmptyBookshelf() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(42.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(42.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = JingduIcons.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "书架还空着",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点右上角的 + 添一本书",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

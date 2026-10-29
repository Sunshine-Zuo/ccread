package com.ccread.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccread.reader.data.epub.EpubSession
import com.ccread.reader.data.repository.BookRepository
import com.ccread.reader.data.settings.SettingsRepository
import com.ccread.reader.domain.book.TxtBookIndexer
import com.ccread.reader.domain.model.Book
import com.ccread.reader.domain.model.Chapter
import com.ccread.reader.domain.model.EpubProgress
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import com.ccread.reader.domain.model.TocNode
import com.ccread.reader.domain.model.TxtProgress
import com.ccread.reader.domain.progress.PercentFormatter
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ReaderUiState(
    val loaded: Boolean = false,
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val chapterTitle: String = "",
    val percentText: String = "0.00%",
    val overlays: OverlayState = OverlayState(),
    val mode: PageMode = PageMode.PAGED,
    val settings: ReadingSettings = ReadingSettings(),
    val startChapterIndex: Int = 0,
    val startOffsetInChapter: Int = 0,
    val currentChapterIndex: Int = 0,
    val jumpToken: Int = 0,
    val missingFile: Boolean = false,
    val epubToc: List<TocNode> = emptyList(),
    val epubCurrentHref: String? = null,
    val epubJumpHref: String? = null,
    val expandedEpubKeys: Set<String> = emptySet(),
)

class ReaderViewModel(
    private val bookId: Long,
    private val repository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val indexer: TxtBookIndexer? = null,
) : ViewModel() {

    private val mutable = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = mutable.asStateFlow()

    private val commands = MutableSharedFlow<PageCommand>(extraBufferCapacity = 4)
    val pageCommands: SharedFlow<PageCommand> = commands.asSharedFlow()

    init {
        viewModelScope.launch {
            settingsRepository.update { it.copy(pageMode = PageMode.PAGED) }
        }
        viewModelScope.launch {
            val book = repository.getBook(bookId)
            val chapters = repository.getChapters(bookId)
            val txtProgress = book?.progress as? TxtProgress
            val startChapter = txtProgress?.chapterIndex
                ?: chapters.firstOrNull()?.index
                ?: 0
            val startOffset = txtProgress?.charOffsetInChapter?.toInt() ?: 0
            val percent = book?.progress?.percent ?: 0.0
            mutable.update {
                it.copy(
                    loaded = true,
                    book = book,
                    chapters = chapters,
                    startChapterIndex = startChapter,
                    startOffsetInChapter = startOffset,
                    currentChapterIndex = startChapter,
                    chapterTitle = chapters.firstOrNull { chapter -> chapter.index == startChapter }?.title.orEmpty(),
                    percentText = PercentFormatter.format(percent),
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                mutable.update { it.copy(settings = settings, mode = settings.pageMode) }
            }
        }
    }

    fun onTapZone(zone: TapZone) {
        when (val action = ReaderInteraction.onTap(zone, mutable.value.mode, mutable.value.overlays)) {
            is ReaderAction.PreviousPage -> viewModelScope.launch { commands.emit(PageCommand.PREVIOUS) }
            is ReaderAction.NextPage -> viewModelScope.launch { commands.emit(PageCommand.NEXT) }
            is ReaderAction.UpdateOverlays -> mutable.update { it.copy(overlays = action.state) }
        }
    }

    fun handleBack(): Boolean {
        if (!mutable.value.overlays.anyVisible()) return false
        mutable.update { it.copy(overlays = ReaderInteraction.onBack(it.overlays)) }
        return true
    }

    fun openToc() = mutable.update { it.copy(overlays = OverlayState(toc = true)) }
    fun openSettings() = mutable.update { it.copy(overlays = OverlayState(settings = true)) }
    fun openBrightness() = mutable.update { it.copy(overlays = OverlayState(brightness = true)) }
    fun dismissOverlays() = mutable.update { it.copy(overlays = OverlayState()) }

    fun updateReadingState(chapterTitle: String? = null, percent: Double? = null) {
        mutable.update {
            it.copy(
                chapterTitle = chapterTitle ?: it.chapterTitle,
                percentText = percent?.let { value -> PercentFormatter.format(value) } ?: it.percentText,
            )
        }
    }

    fun onPositionChanged(chapterIndex: Int, offsetInChapter: Int, percent: Double) {
        val current = mutable.value
        val title = current.chapters.firstOrNull { it.index == chapterIndex }?.title ?: current.chapterTitle
        mutable.update {
            it.copy(
                chapterTitle = title,
                percentText = PercentFormatter.format(percent),
                currentChapterIndex = chapterIndex,
                startChapterIndex = chapterIndex,
                startOffsetInChapter = offsetInChapter,
            )
        }
        val book = current.book ?: return
        viewModelScope.launch {
            repository.saveProgress(
                book.id,
                TxtProgress(
                    percent = percent,
                    chapterIndex = chapterIndex,
                    charOffsetInChapter = offsetInChapter.toLong(),
                ),
            )
        }
    }

    fun markMissingFile() {
        mutable.update { it.copy(missingFile = true) }
    }

    fun onEpubLocator(locatorJson: String, chapterTitle: String, percent: Double) {
        val href = EpubSession.parseLocator(locatorJson)?.href?.toString()
        val safePercent = percent.coerceIn(0.0, 100.0)
        val ancestors = href?.let { TocAncestors.of(mutable.value.epubToc, it) }.orEmpty()
        mutable.update {
            it.copy(
                chapterTitle = chapterTitle,
                percentText = PercentFormatter.format(safePercent),
                epubCurrentHref = href,
                expandedEpubKeys = it.expandedEpubKeys + ancestors,
                missingFile = false,
            )
        }
        val book = mutable.value.book ?: return
        viewModelScope.launch {
            repository.saveProgress(book.id, EpubProgress(percent = safePercent, locatorJson = locatorJson))
        }
    }

    fun onEpubTocLoaded(tree: List<TocNode>) {
        mutable.update { it.copy(epubToc = tree) }
    }

    fun jumpToEpub(href: String) {
        mutable.update { it.copy(epubJumpHref = href, overlays = OverlayState()) }
    }

    fun consumeEpubJump() {
        mutable.update { it.copy(epubJumpHref = null) }
    }

    fun toggleEpubNode(key: String) {
        mutable.update {
            val expanded = it.expandedEpubKeys
            it.copy(
                expandedEpubKeys = if (key in expanded) expanded - key else expanded + key,
            )
        }
    }

    fun updateSettings(transform: (ReadingSettings) -> ReadingSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun jumpTo(chapterIndex: Int) {
        mutable.update {
            it.copy(
                startChapterIndex = chapterIndex,
                startOffsetInChapter = 0,
                currentChapterIndex = chapterIndex,
                chapterTitle = it.chapters.firstOrNull { chapter -> chapter.index == chapterIndex }?.title
                    ?: it.chapterTitle,
                overlays = OverlayState(),
                jumpToken = it.jumpToken + 1,
            )
        }
    }

    fun resplit() {
        val current = mutable.value
        val book = current.book ?: return
        val indexer = indexer ?: return
        viewModelScope.launch {
            val oldChapter = current.chapters.firstOrNull { it.index == current.currentChapterIndex }
            val oldPosition = (oldChapter?.charStart ?: 0L) + current.startOffsetInChapter
            val chapters = indexer.resplit(book) ?: return@launch
            val (newIndex, newOffset) = ResplitMapping.map(current.chapters, chapters, oldPosition)
            mutable.update {
                it.copy(
                    chapters = chapters,
                    startChapterIndex = newIndex,
                    startOffsetInChapter = newOffset.toInt(),
                    currentChapterIndex = newIndex,
                    chapterTitle = chapters.firstOrNull { chapter -> chapter.index == newIndex }?.title
                        ?: it.chapterTitle,
                    overlays = OverlayState(),
                    jumpToken = it.jumpToken + 1,
                )
            }
        }
    }
}

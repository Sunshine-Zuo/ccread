package com.ccread.reader

import android.app.Application
import com.ccread.reader.data.db.AppDatabase
import com.ccread.reader.data.repository.RoomBookRepository
import com.ccread.reader.data.settings.SettingsRepository
import com.ccread.reader.domain.book.BookImporter
import com.ccread.reader.domain.book.TxtBookIndexer

class JingduApp : Application() {
    val database by lazy { AppDatabase.get(this) }
    val bookRepository by lazy { RoomBookRepository(database.bookDao(), contentResolver) }
    val bookImporter by lazy { BookImporter(this, bookRepository) }
    val txtBookIndexer by lazy { TxtBookIndexer(this, bookRepository) }
    val settingsRepository by lazy { SettingsRepository.create(this) }
}

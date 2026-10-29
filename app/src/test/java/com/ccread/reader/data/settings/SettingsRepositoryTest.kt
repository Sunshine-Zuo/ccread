package com.ccread.reader.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `defaults are used and values are clamped`() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmp.newFile("settings.preferences_pb") },
        )
        val repository = SettingsRepository(store)

        val defaults = repository.settings.first()
        assertEquals(18, defaults.fontSize)
        assertEquals(24, defaults.marginHorizontal)
        assertEquals(1.5f, defaults.lineSpacing, 0.001f)
        assertEquals(0.5f, defaults.brightness, 0.001f)

        repository.update {
            it.copy(fontSize = 5, marginHorizontal = 99, lineSpacing = 3f, brightness = 2f)
        }

        val clamped = repository.settings.first()
        assertEquals(12, clamped.fontSize)
        assertEquals(48, clamped.marginHorizontal)
        assertEquals(2f, clamped.lineSpacing, 0.001f)
        assertEquals(1f, clamped.brightness, 0.001f)
    }
}

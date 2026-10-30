package com.ccread.reader.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `custom background is persisted and can be cleared`() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmp.newFile("settings.preferences_pb") },
        )
        val repository = SettingsRepository(store)

        repository.update { it.copy(customBackgroundArgb = 0xFF101112.toInt()) }
        assertEquals(0xFF101112.toInt(), repository.settings.first().customBackgroundArgb)

        repository.update { it.copy(customBackgroundArgb = null) }
        assertNull(repository.settings.first().customBackgroundArgb)
    }

    @Test
    fun `font color is persisted and can return to the background default`() = runTest {
        val store = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { tmp.newFile("settings.preferences_pb") },
        )
        val repository = SettingsRepository(store)

        repository.update { it.copy(textColorArgb = 0xFF112233.toInt()) }
        assertEquals(0xFF112233.toInt(), repository.settings.first().textColorArgb)

        repository.update { it.copy(textColorArgb = null) }
        assertNull(repository.settings.first().textColorArgb)
    }
}

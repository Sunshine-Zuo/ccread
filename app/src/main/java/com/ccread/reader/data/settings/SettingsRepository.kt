package com.ccread.reader.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ccread.reader.domain.model.BackgroundId
import com.ccread.reader.domain.model.PageMode
import com.ccread.reader.domain.model.ReadingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<ReadingSettings> = dataStore.data.map { it.toSettings().clamped() }

    suspend fun update(transform: (ReadingSettings) -> ReadingSettings) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toSettings()).clamped()
            preferences[Keys.FONT_SIZE] = next.fontSize
            preferences[Keys.MARGIN] = next.marginHorizontal
            preferences[Keys.LINE_SPACING] = next.lineSpacing
            preferences[Keys.BACKGROUND] = next.backgroundId.name
            preferences[Keys.PAGE_MODE] = next.pageMode.name
            preferences[Keys.FOLLOW_SYSTEM_BRIGHTNESS] = next.followSystemBrightness
            preferences[Keys.BRIGHTNESS] = next.brightness
        }
    }

    companion object {
        fun create(context: Context): SettingsRepository = SettingsRepository(context.settingsDataStore)
    }
}

private object Keys {
    val FONT_SIZE = intPreferencesKey("font_size")
    val MARGIN = intPreferencesKey("margin")
    val LINE_SPACING = floatPreferencesKey("line_spacing")
    val BACKGROUND = stringPreferencesKey("background")
    val PAGE_MODE = stringPreferencesKey("page_mode")
    val FOLLOW_SYSTEM_BRIGHTNESS = booleanPreferencesKey("follow_system_brightness")
    val BRIGHTNESS = floatPreferencesKey("brightness")
}

private fun Preferences.toSettings(): ReadingSettings {
    val defaults = ReadingSettings()
    return ReadingSettings(
        fontSize = this[Keys.FONT_SIZE] ?: defaults.fontSize,
        marginHorizontal = this[Keys.MARGIN] ?: defaults.marginHorizontal,
        lineSpacing = this[Keys.LINE_SPACING] ?: defaults.lineSpacing,
        backgroundId = this[Keys.BACKGROUND]
            ?.let { runCatching { BackgroundId.valueOf(it) }.getOrNull() }
            ?: defaults.backgroundId,
        pageMode = this[Keys.PAGE_MODE]
            ?.let { runCatching { PageMode.valueOf(it) }.getOrNull() }
            ?: defaults.pageMode,
        followSystemBrightness = this[Keys.FOLLOW_SYSTEM_BRIGHTNESS] ?: defaults.followSystemBrightness,
        brightness = this[Keys.BRIGHTNESS] ?: defaults.brightness,
    )
}

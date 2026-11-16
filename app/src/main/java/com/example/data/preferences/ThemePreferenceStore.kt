package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PlaybackLoopMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.winterPrefsDataStore by preferencesDataStore(name = "winter_theme_prefs")

class ThemePreferenceStore(private val context: Context) {
    private val darkWinterKey = booleanPreferencesKey("dark_winter_mode")
    private val loopModeKey = stringPreferencesKey("playback_loop_mode")
    private val showTutorialOnStartKey = booleanPreferencesKey("show_tutorial_on_start")

    val darkWinterModeFlow: Flow<Boolean> = context.winterPrefsDataStore.data
        .map { prefs -> prefs[darkWinterKey] ?: true }

    val loopModeFlow: Flow<PlaybackLoopMode> = context.winterPrefsDataStore.data
        .map { prefs ->
            val raw = prefs[loopModeKey] ?: PlaybackLoopMode.LOOP_ALL.name
            runCatching { PlaybackLoopMode.valueOf(raw) }.getOrDefault(PlaybackLoopMode.LOOP_ALL)
        }

    val showTutorialOnStartFlow: Flow<Boolean> = context.winterPrefsDataStore.data
        .map { prefs -> prefs[showTutorialOnStartKey] ?: true }

    suspend fun setDarkWinterMode(enabled: Boolean) {
        context.winterPrefsDataStore.edit { prefs ->
            prefs[darkWinterKey] = enabled
        }
    }

    suspend fun setLoopMode(mode: PlaybackLoopMode) {
        context.winterPrefsDataStore.edit { prefs ->
            prefs[loopModeKey] = mode.name
        }
    }

    suspend fun setShowTutorialOnStart(show: Boolean) {
        context.winterPrefsDataStore.edit { prefs ->
            prefs[showTutorialOnStartKey] = show
        }
    }
}

package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemePreference
import com.example.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.duckLandDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "duck_land_prefs"
)

class ThemePreferencesRepository(private val context: Context) : PreferencesRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    override val themePreference: Flow<ThemePreference> =
        context.applicationContext.duckLandDataStore.data.map { prefs ->
            val raw = prefs[Keys.THEME_MODE]
            ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.SYSTEM
        }

    override val notificationsEnabled: Flow<Boolean> =
        context.applicationContext.duckLandDataStore.data.map { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
        }

    override suspend fun setThemePreference(preference: ThemePreference) {
        context.applicationContext.duckLandDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = preference.name
        }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.applicationContext.duckLandDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
}

package com.openmusic.search.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_QUALITY = intPreferencesKey("default_quality")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CACHE_SIZE_MB = intPreferencesKey("cache_size_mb")
    }

    enum class Theme { LIGHT, DARK, SYSTEM }

    val theme: Flow<Theme> = context.dataStore.data.map {
        Theme.valueOf(it[Keys.THEME] ?: Theme.SYSTEM.name)
    }

    val defaultQuality: Flow<Int> = context.dataStore.data.map { it[Keys.DEFAULT_QUALITY] ?: 0 }
    val autoPlayNext: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_PLAY_NEXT] ?: true }
    val backgroundPlayback: Flow<Boolean> = context.dataStore.data.map { it[Keys.BACKGROUND_PLAYBACK] ?: true }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[Keys.WIFI_ONLY] ?: false }
    val cacheSizeMb: Flow<Int> = context.dataStore.data.map { it[Keys.CACHE_SIZE_MB] ?: 500 }

    suspend fun setTheme(theme: Theme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDefaultQuality(quality: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_QUALITY] = quality }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_NEXT] = enabled }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setCacheSizeMb(mb: Int) {
        context.dataStore.edit { it[Keys.CACHE_SIZE_MB] = mb }
    }
}

package com.aetheraudio.pro.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aether_settings")

/** Small DataStore-backed prefs: theme mode, AMOLED true-black, EQ band count preference, etc. */
class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_FOLLOW_SYSTEM_THEME = booleanPreferencesKey("follow_system_theme")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_AMOLED_TRUE_BLACK = booleanPreferencesKey("amoled_true_black")
        private val KEY_EQ_BAND_COUNT_PREF = intPreferencesKey("eq_band_count_pref") // 10 or 32 (UI grouping only)

        @Volatile private var instance: SettingsRepository? = null
        fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
            instance ?: SettingsRepository(context.applicationContext).also { instance = it }
        }
    }

    val followSystemTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_FOLLOW_SYSTEM_THEME] ?: true }
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_THEME] ?: true }
    val amoledTrueBlack: Flow<Boolean> = context.dataStore.data.map { it[KEY_AMOLED_TRUE_BLACK] ?: false }
    val eqBandCountPreference: Flow<Int> = context.dataStore.data.map { it[KEY_EQ_BAND_COUNT_PREF] ?: 10 }

    suspend fun setFollowSystemTheme(value: Boolean) = context.dataStore.edit { it[KEY_FOLLOW_SYSTEM_THEME] = value }
    suspend fun setDarkTheme(value: Boolean) = context.dataStore.edit { it[KEY_DARK_THEME] = value }
    suspend fun setAmoledTrueBlack(value: Boolean) = context.dataStore.edit { it[KEY_AMOLED_TRUE_BLACK] = value }
    suspend fun setEqBandCountPreference(value: Int) = context.dataStore.edit { it[KEY_EQ_BAND_COUNT_PREF] = value }
}

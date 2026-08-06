package com.maddogwarner.essential8kb.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
) {
    val showSplashOnStartup: Flow<Boolean> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[SHOW_SPLASH_ON_STARTUP_KEY] != false
            }

    val referenceOnlyMode: Flow<Boolean> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[REFERENCE_ONLY_MODE_KEY] == true
            }

    val deepAuditEnabled: Flow<Boolean> = booleanSetting(DEEP_AUDIT_ENABLED_KEY)

    val multiProfileEnabled: Flow<Boolean> = booleanSetting(MULTI_PROFILE_ENABLED_KEY)

    suspend fun setShowSplashOnStartup(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_SPLASH_ON_STARTUP_KEY] = show
        }
    }

    suspend fun setReferenceOnlyMode(referenceOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[REFERENCE_ONLY_MODE_KEY] = referenceOnly
        }
    }

    suspend fun setDeepAuditEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DEEP_AUDIT_ENABLED_KEY] = enabled }
    }

    suspend fun setMultiProfileEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[MULTI_PROFILE_ENABLED_KEY] = enabled }
    }

    private fun booleanSetting(key: Preferences.Key<Boolean>): Flow<Boolean> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences -> preferences[key] == true }

    companion object {
        private val SHOW_SPLASH_ON_STARTUP_KEY = booleanPreferencesKey("showSplashOnStartup")
        private val REFERENCE_ONLY_MODE_KEY = booleanPreferencesKey("referenceOnlyMode")
        private val DEEP_AUDIT_ENABLED_KEY = booleanPreferencesKey("deepAuditEnabled")
        private val MULTI_PROFILE_ENABLED_KEY = booleanPreferencesKey("multiProfileEnabled")
    }
}

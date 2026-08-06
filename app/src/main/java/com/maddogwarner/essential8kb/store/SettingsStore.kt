package com.maddogwarner.essential8kb.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
                preferences[GlobalSettingsKeys.showSplashOnStartup] != false
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
                preferences[GlobalSettingsKeys.referenceOnlyMode] == true
            }

    val deepAuditEnabled: Flow<Boolean> = booleanSetting(GlobalSettingsKeys.deepAuditEnabled)

    val multiProfileEnabled: Flow<Boolean> = booleanSetting(GlobalSettingsKeys.multiProfileEnabled)

    suspend fun setShowSplashOnStartup(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[GlobalSettingsKeys.showSplashOnStartup] = show
        }
    }

    suspend fun setReferenceOnlyMode(referenceOnly: Boolean) {
        dataStore.edit { preferences ->
            preferences[GlobalSettingsKeys.referenceOnlyMode] = referenceOnly
        }
    }

    suspend fun setDeepAuditEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[GlobalSettingsKeys.deepAuditEnabled] = enabled }
    }

    suspend fun setMultiProfileEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[GlobalSettingsKeys.multiProfileEnabled] = enabled }
    }

    private fun booleanSetting(key: Preferences.Key<Boolean>): Flow<Boolean> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences -> preferences[key] == true }
}

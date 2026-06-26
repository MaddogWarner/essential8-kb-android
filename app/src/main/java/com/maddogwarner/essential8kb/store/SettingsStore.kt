package com.maddogwarner.essential8kb.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsStore(
    private val dataStore: DataStore<Preferences>,
) {
    val licenseMode: Flow<Microsoft365LicenseMode> =
        dataStore.data.map { preferences ->
            Microsoft365LicenseMode.fromRawValue(preferences[LICENSE_MODE_KEY])
        }

    suspend fun setLicenseMode(mode: Microsoft365LicenseMode) {
        dataStore.edit { preferences ->
            preferences[LICENSE_MODE_KEY] = mode.rawValue
        }
    }

    companion object {
        private val LICENSE_MODE_KEY = stringPreferencesKey("microsoft365LicenseMode")
    }
}

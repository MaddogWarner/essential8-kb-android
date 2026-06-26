package com.maddogwarner.essential8kb

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.store.SettingsStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun licenseModeRoundTripsUsingIosRawValue() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "settings.preferences_pb") },
        )
        val store = SettingsStore(dataStore)

        store.setLicenseMode(Microsoft365LicenseMode.E3_P2)

        assertEquals(Microsoft365LicenseMode.E3_P2, store.licenseMode.first())
        scope.cancel()
    }

    @Test
    fun unexpectedLicenseModeFallsBackToNone() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "invalid-settings.preferences_pb") },
        )
        val key = stringPreferencesKey("microsoft365LicenseMode")
        dataStore.edit { preferences ->
            preferences[key] = "unexpected"
        }
        val store = SettingsStore(dataStore)

        assertEquals(Microsoft365LicenseMode.NONE, store.licenseMode.first())
        scope.cancel()
    }
}

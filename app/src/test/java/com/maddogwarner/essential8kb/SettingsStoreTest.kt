package com.maddogwarner.essential8kb

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.maddogwarner.essential8kb.store.SettingsStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun globalFeaturePreferencesRoundTrip() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "global-settings.preferences_pb") },
        )
        val store = SettingsStore(dataStore)

        assertTrue(store.showSplashOnStartup.first())
        assertFalse(store.referenceOnlyMode.first())
        assertFalse(store.deepAuditEnabled.first())
        assertFalse(store.multiProfileEnabled.first())

        store.setShowSplashOnStartup(false)
        store.setReferenceOnlyMode(true)
        store.setDeepAuditEnabled(true)
        store.setMultiProfileEnabled(true)

        assertFalse(store.showSplashOnStartup.first())
        assertTrue(store.referenceOnlyMode.first())
        assertTrue(store.deepAuditEnabled.first())
        assertTrue(store.multiProfileEnabled.first())

        scope.cancel()
    }
}

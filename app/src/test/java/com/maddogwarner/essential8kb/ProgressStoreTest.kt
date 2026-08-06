package com.maddogwarner.essential8kb

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import com.maddogwarner.essential8kb.store.StepState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun statusesRoundTripOnActiveProfile() = runBlocking {
        val scope = testScope()
        val store = ProgressStore(dataStore(scope, "statuses.preferences_pb"))

        assertTrue(store.stepStatuses.first().isEmpty())
        store.toggle("1-1-0")
        assertEquals(StepState.IMPLEMENTED, store.stepStatuses.first()["1-1-0"]?.state)
        store.setStatus(StepState.NOT_APPLICABLE, "Out of scope", "1-1-1")
        assertEquals("Out of scope", store.stepStatuses.first()["1-1-1"]?.reason)
        store.toggle("1-1-0")
        assertFalse(store.stepStatuses.first().containsKey("1-1-0"))

        scope.cancel()
    }

    @Test
    fun migrationFromLegacyStringSetCreatesDefaultProfile() = runBlocking {
        val scope = testScope()
        val dataStore = dataStore(scope, "legacy-set.preferences_pb")
        dataStore.edit { preferences ->
            preferences[stringSetPreferencesKey("e8kb.stepProgress")] = setOf("1-1-0", "1-1-1")
        }

        val store = ProgressStore(dataStore)
        val profile = store.activeProfile.first()

        assertEquals("Default", profile.name)
        assertEquals(2, profile.stepProgress.size)
        assertEquals(StepState.IMPLEMENTED, profile.stepProgress["1-1-0"]?.state)
        assertEquals(StepState.IMPLEMENTED, profile.stepProgress["1-1-1"]?.state)

        scope.cancel()
    }

    @Test
    fun migrationFromStatusDictionaryPreservesStatusesAndSettings() = runBlocking {
        val scope = testScope()
        val dataStore = dataStore(scope, "legacy-dictionary.preferences_pb")
        val statuses = JSONObject().apply {
            put("1-1-0", JSONObject().apply {
                put("state", "Implemented")
                put("reason", JSONObject.NULL)
            })
            put("1-1-1", JSONObject().apply {
                put("state", "Not Applicable")
                put("reason", "Legacy reason")
            })
        }
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey("e8kb.stepProgressDict")] = statuses.toString()
            preferences[intPreferencesKey("targetMaturityLevel")] = 2
            preferences[stringPreferencesKey("osScopeFilter")] = "server"
            preferences[stringPreferencesKey("microsoft365LicenseMode")] = "e3P2"
        }

        val store = ProgressStore(dataStore)
        val profile = store.activeProfile.first()

        assertEquals(2, profile.stepProgress.size)
        assertEquals(StepState.NOT_APPLICABLE, profile.stepProgress["1-1-1"]?.state)
        assertEquals("Legacy reason", profile.stepProgress["1-1-1"]?.reason)
        assertEquals(MaturityLevel.ML2, store.targetMaturityLevel.first())
        assertEquals(OSScope.SERVER, store.osScope.first())
        assertEquals(Microsoft365LicenseMode.E3_P2, store.licenseMode.first())

        scope.cancel()
    }

    @Test
    fun profileCreateRenameSwitchAndDeleteFollowRules() = runBlocking {
        val scope = testScope()
        val store = ProgressStore(dataStore(scope, "profile-operations.preferences_pb"))
        val defaultId = store.activeProfile.first().id

        val secondId = store.createProfile("  Production  ")
        assertEquals("Production", store.profiles.first().last().name)
        store.renameProfile(secondId, "  Hospital A  ")
        assertEquals("Hospital A", store.profiles.first().last().name)
        store.renameProfile(secondId, "   ")
        assertEquals("Hospital A", store.profiles.first().last().name)

        store.switchProfile(secondId)
        assertEquals(secondId, store.activeProfileId.first())
        store.deleteProfile(secondId)
        assertEquals(listOf(defaultId), store.profiles.first().map { it.id })
        assertEquals(defaultId, store.activeProfileId.first())

        store.deleteProfile(defaultId)
        assertEquals(1, store.profiles.first().size)

        val unnamedId = store.createProfile(" \n ")
        assertEquals("New Profile", store.profiles.first().first { it.id == unnamedId }.name)

        scope.cancel()
    }

    @Test
    fun switchingProfilesChangesVisibleStatusesAndPerProfileSettings() = runBlocking {
        val scope = testScope()
        val store = ProgressStore(dataStore(scope, "profile-switch.preferences_pb"))
        val defaultId = store.activeProfile.first().id
        store.setStatus(StepState.IMPLEMENTED, null, "1-1-0")
        store.setTargetMaturityLevel(MaturityLevel.ML1)
        store.setOSScope(OSScope.WORKSTATION)
        store.setLicenseMode(Microsoft365LicenseMode.E5)

        val secondId = store.createProfile("Second")
        store.switchProfile(secondId)
        assertTrue(store.stepStatuses.first().isEmpty())
        assertEquals(MaturityLevel.ML3, store.targetMaturityLevel.first())
        assertEquals(OSScope.BOTH, store.osScope.first())
        assertEquals(Microsoft365LicenseMode.NONE, store.licenseMode.first())
        store.setStatus(StepState.NOT_APPLICABLE, "Second profile", "1-1-0")

        store.switchProfile(defaultId)
        assertEquals(StepState.IMPLEMENTED, store.stepStatuses.first()["1-1-0"]?.state)
        assertEquals(MaturityLevel.ML1, store.targetMaturityLevel.first())
        assertEquals(OSScope.WORKSTATION, store.osScope.first())
        assertEquals(Microsoft365LicenseMode.E5, store.licenseMode.first())

        scope.cancel()
    }

    @Test
    fun resetAllReplacesProfilesAndClearsGlobalBooleans() = runBlocking {
        val scope = testScope()
        val dataStore = dataStore(scope, "reset.preferences_pb")
        val progressStore = ProgressStore(dataStore)
        val settingsStore = SettingsStore(dataStore)
        val originalId = progressStore.activeProfile.first().id
        progressStore.createProfile("Second")
        progressStore.setStatus(StepState.IMPLEMENTED, null, "1-1-0")
        settingsStore.setShowSplashOnStartup(false)
        settingsStore.setReferenceOnlyMode(true)
        settingsStore.setDeepAuditEnabled(true)
        settingsStore.setMultiProfileEnabled(true)

        progressStore.resetAll()

        val profiles = progressStore.profiles.first()
        assertEquals(1, profiles.size)
        assertEquals("Default", profiles.single().name)
        assertNotEquals(originalId, profiles.single().id)
        assertTrue(progressStore.stepStatuses.first().isEmpty())
        assertTrue(settingsStore.showSplashOnStartup.first())
        assertFalse(settingsStore.referenceOnlyMode.first())
        assertFalse(settingsStore.deepAuditEnabled.first())
        assertFalse(settingsStore.multiProfileEnabled.first())

        scope.cancel()
    }

    @Test
    fun complianceUsesScopeFilteredSteps() = runBlocking {
        val scope = testScope()
        val store = ProgressStore(dataStore(scope, "scope-compliance.preferences_pb"))
        val allWorkstationSteps = EssentialControlsData.all.flatMap {
            it.steps(MaturityLevel.ML3, OSScope.WORKSTATION)
        }
        allWorkstationSteps.forEach { store.setStatus(StepState.IMPLEMENTED, null, it.id) }
        val statuses = store.stepStatuses.first()
        val allSteps = EssentialControlsData.all.flatMap { it.steps(MaturityLevel.ML3, OSScope.BOTH) }

        assertEquals(100.0, store.compliancePercentage(allWorkstationSteps, statuses), 0.001)
        assertEquals(65.0 / 67.0 * 100.0, store.compliancePercentage(allSteps, statuses), 0.001)
        assertTrue(
            store.isControlComplete(
                EssentialControlsData.regularBackups,
                MaturityLevel.ML3,
                OSScope.WORKSTATION,
                statuses,
            ),
        )

        scope.cancel()
    }

    private fun testScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun dataStore(scope: CoroutineScope, fileName: String) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(temporaryFolder.root, fileName) },
    )
}

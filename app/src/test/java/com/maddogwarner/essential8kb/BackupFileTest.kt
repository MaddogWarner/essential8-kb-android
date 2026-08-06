package com.maddogwarner.essential8kb

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.AUDIT_ENTRIES_PER_STEP_CAP
import com.maddogwarner.essential8kb.store.AuditEntry
import com.maddogwarner.essential8kb.store.BackupException
import com.maddogwarner.essential8kb.store.BackupFile
import com.maddogwarner.essential8kb.store.GlobalSettingsBackup
import com.maddogwarner.essential8kb.store.Profile
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupFileTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun versionTwoRoundTripRestoresProfilesAndGlobalSettings() = runBlocking {
        val sourceScope = testScope()
        val destinationScope = testScope()
        val sourceDataStore = dataStore(sourceScope, "backup-source.preferences_pb")
        val destinationDataStore = dataStore(destinationScope, "backup-destination.preferences_pb")
        val source = ProgressStore(sourceDataStore)
        val destination = ProgressStore(destinationDataStore)
        val sourceSettings = SettingsStore(sourceDataStore)
        val destinationSettings = SettingsStore(destinationDataStore)

        sourceSettings.setDeepAuditEnabled(true)
        sourceSettings.setShowSplashOnStartup(false)
        source.setTargetMaturityLevel(MaturityLevel.ML2)
        source.setOSScope(OSScope.SERVER)
        source.setLicenseMode(Microsoft365LicenseMode.E5)
        source.setStatus(StepState.NOT_APPLICABLE, "Approved exception", "Audit note", "1-1-0")
        val secondId = source.createProfile("DR")
        source.switchProfile(secondId)
        source.setOSScope(OSScope.WORKSTATION)

        val decoded = BackupFile.decode(BackupFile.encode(source.exportAllProfiles()))
        destination.importFullDevice(decoded)

        val sourceProfiles = source.profiles.first()
        val restoredProfiles = destination.profiles.first()
        assertEquals(sourceProfiles.map { it.id }, restoredProfiles.map { it.id })
        assertEquals(sourceProfiles.map { it.name }, restoredProfiles.map { it.name })
        assertEquals(sourceProfiles.map { it.stepProgress }, restoredProfiles.map { it.stepProgress })
        assertEquals(
            sourceProfiles.map { profile -> profile.auditTrail.mapValues { it.value.map(AuditEntry::note) } },
            restoredProfiles.map { profile -> profile.auditTrail.mapValues { it.value.map(AuditEntry::note) } },
        )
        assertEquals(
            sourceProfiles.map { it.targetMaturityLevelRaw },
            restoredProfiles.map { it.targetMaturityLevelRaw },
        )
        assertEquals(sourceProfiles.map { it.osScopeFilterRaw }, restoredProfiles.map { it.osScopeFilterRaw })
        assertEquals(
            sourceProfiles.map { it.microsoft365LicenseModeRaw },
            restoredProfiles.map { it.microsoft365LicenseModeRaw },
        )
        assertTrue(destinationSettings.deepAuditEnabled.first())
        assertFalse(destinationSettings.showSplashOnStartup.first())
        assertTrue(destinationSettings.multiProfileEnabled.first())

        sourceScope.cancel()
        destinationScope.cancel()
    }

    @Test
    fun versionOneMigratesToImportedProfileAndValidatesSettings() {
        val valid = """
            {"schemaVersion":1,"appVersion":"1.6","exportedAt":"2026-07-13T00:00:00Z","stepProgress":{"old":{"state":"Implemented"}},"settings":{"targetMaturityLevel":1,"osScopeFilter":"server","microsoft365LicenseMode":"e5"}}
        """.trimIndent()
        val migrated = BackupFile.decode(valid.toByteArray())

        assertEquals(BackupFile.CURRENT_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals("Imported", migrated.profiles.single().name)
        assertEquals(StepState.IMPLEMENTED, migrated.profiles.single().stepProgress["old"]?.state)
        assertEquals(1, migrated.profiles.single().targetMaturityLevelRaw)
        assertEquals("server", migrated.profiles.single().osScopeFilterRaw)
        assertEquals("e5", migrated.profiles.single().microsoft365LicenseModeRaw)
        assertNull(migrated.globalSettings)

        val unknown = """
            {"schemaVersion":1,"appVersion":"1.6","exportedAt":"2026-07-13T00:00:00Z","stepProgress":{},"settings":{"targetMaturityLevel":99,"osScopeFilter":"cloud","microsoft365LicenseMode":"enterprise"}}
        """.trimIndent()
        val defaults = BackupFile.decode(unknown.toByteArray()).profiles.single()
        assertEquals(MaturityLevel.ML3.level, defaults.targetMaturityLevelRaw)
        assertEquals(OSScope.BOTH.rawValue, defaults.osScopeFilterRaw)
        assertEquals(Microsoft365LicenseMode.NONE.rawValue, defaults.microsoft365LicenseModeRaw)
    }

    @Test
    fun iosStyleVersionTwoBackupDecodesWithAssessmentContext() {
        val json = """
            {
              "appVersion" : "1.8",
              "exportedAt" : "2026-08-06T10:00:00Z",
              "globalSettings" : {
                "deepAuditEnabled" : true,
                "multiProfileEnabled" : false,
                "referenceOnlyMode" : true,
                "showSplashOnStartup" : false
              },
              "profiles" : [
                {
                  "auditTrail" : {
                    "1-1-0" : [
                      {
                        "id" : "E2AC8BC0-87ED-4DB5-A229-583063CE209F",
                        "newState" : "Implemented",
                        "note" : "iOS export",
                        "previousState" : "Not Implemented",
                        "timestamp" : "2026-08-06T09:59:00Z"
                      }
                    ]
                  },
                  "createdAt" : "2026-08-01T00:00:00Z",
                  "id" : "6BBCE345-B0BB-46A5-9B92-A7A3188EB91D",
                  "microsoft365LicenseModeRaw" : "e5",
                  "name" : "iOS Assessment",
                  "osScopeFilterRaw" : "server",
                  "stepProgress" : {
                    "1-1-0" : {
                      "state" : "Implemented"
                    },
                    "1-1-1" : {
                      "reason" : "Approved exception",
                      "state" : "Not Applicable"
                    }
                  },
                  "targetMaturityLevelRaw" : 2
                }
              ],
              "schemaVersion" : 2
            }
        """.trimIndent()

        val backup = BackupFile.decode(json.toByteArray())
        val profile = backup.profiles.single()

        assertEquals("iOS Assessment", profile.name)
        assertEquals(StepState.IMPLEMENTED, profile.stepProgress["1-1-0"]?.state)
        assertEquals("Approved exception", profile.stepProgress["1-1-1"]?.reason)
        assertEquals("iOS export", profile.auditTrail["1-1-0"]?.single()?.note)
        assertEquals(2, profile.targetMaturityLevelRaw)
        assertEquals("server", profile.osScopeFilterRaw)
        assertEquals("e5", profile.microsoft365LicenseModeRaw)
        assertTrue(backup.globalSettings?.deepAuditEnabled == true)
        assertEquals(false, backup.globalSettings?.showSplashOnStartup)
    }

    @Test
    fun invalidFilesAndBackupInvariantsAreRejected() {
        val profile = Profile.newDefault()
        val unsupported = backup(profiles = listOf(profile), schemaVersion = 3)
        val unsupportedError = assertThrows(BackupException.UnsupportedSchema::class.java) {
            BackupFile.decode(BackupFile.encode(unsupported))
        }
        assertEquals("This backup uses schema version 3. Update the app before importing it.", unsupportedError.message)

        assertThrows(BackupException.InvalidFile::class.java) {
            BackupFile.decode("not json".toByteArray())
        }
        assertThrows(BackupException.InvalidFile::class.java) {
            BackupFile.decode(BackupFile.encode(backup(profiles = emptyList())))
        }
        assertThrows(BackupException.InvalidFile::class.java) {
            BackupFile.decode(BackupFile.encode(backup(profiles = listOf(profile, profile))))
        }
        assertThrows(BackupException.FileTooLarge::class.java) {
            BackupFile.decode(ByteArray(BackupFile.MAXIMUM_FILE_SIZE + 1))
        }
        assertThrows(BackupException.FileTooLarge::class.java) {
            BackupFile.encode(backup(profiles = listOf(profile.copy(name = "x".repeat(BackupFile.MAXIMUM_FILE_SIZE)))))
        }
        assertThrows(BackupException.FileTooLarge::class.java) {
            BackupFile.read(ByteArrayInputStream(ByteArray(BackupFile.MAXIMUM_FILE_SIZE + 1)))
        }
    }

    @Test
    fun encodeCapsEachStepsAuditTrailToNewestTwoHundredEntries() {
        val entries = List(AUDIT_ENTRIES_PER_STEP_CAP + 5) { index ->
            AuditEntry(
                id = UUID.randomUUID().toString(),
                timestamp = 1_700_000_000_000L + index * 1_000L,
                previousState = StepState.NOT_IMPLEMENTED,
                newState = StepState.IMPLEMENTED,
                note = index.toString(),
            )
        }
        val profile = Profile.newDefault().copy(auditTrail = mapOf("capped" to entries))

        val decoded = BackupFile.decode(BackupFile.encode(backup(profiles = listOf(profile))))
        val restored = decoded.profiles.single().auditTrail.getValue("capped")

        assertEquals(AUDIT_ENTRIES_PER_STEP_CAP, restored.size)
        assertEquals("5", restored.first().note)
        assertEquals("204", restored.last().note)
    }

    @Test
    fun activeProfileImportUsesFreshIdsAndPreservesExistingProfiles() = runBlocking {
        val sourceScope = testScope()
        val destinationScope = testScope()
        val source = ProgressStore(dataStore(sourceScope, "profile-source.preferences_pb"))
        val destinationDataStore = dataStore(destinationScope, "profile-destination.preferences_pb")
        val destination = ProgressStore(destinationDataStore)
        val originalId = destination.activeProfile.first().id
        source.renameProfile(source.activeProfile.first().id, "Default")
        source.setStatus(StepState.IMPLEMENTED, null, "portable")
        val sourceId = source.activeProfile.first().id

        destination.importAsNewProfile(source.exportActiveProfile())

        val profiles = destination.profiles.first()
        assertEquals(2, profiles.size)
        assertTrue(profiles.any { it.id == originalId })
        val imported = profiles.first { it.id != originalId }
        assertNotEquals(sourceId, imported.id)
        assertEquals("Default (imported)", imported.name)
        assertEquals(StepStatus(StepState.IMPLEMENTED), imported.stepProgress["portable"])
        assertEquals(imported.id, destination.activeProfileId.first())
        assertTrue(SettingsStore(destinationDataStore).multiProfileEnabled.first())

        sourceScope.cancel()
        destinationScope.cancel()
    }

    @Test
    fun fullDeviceImportRemovesGlobalSettingsMissingFromEnvelope() = runBlocking {
        val scope = testScope()
        val dataStore = dataStore(scope, "nullable-settings.preferences_pb")
        val store = ProgressStore(dataStore)
        val settingsStore = SettingsStore(dataStore)
        settingsStore.setReferenceOnlyMode(true)
        settingsStore.setDeepAuditEnabled(true)
        settingsStore.setMultiProfileEnabled(true)
        settingsStore.setShowSplashOnStartup(false)

        store.importFullDevice(
            backup(
                profiles = listOf(Profile.newDefault()),
                globalSettings = GlobalSettingsBackup(null, null, null, null),
            ),
        )

        assertTrue(settingsStore.showSplashOnStartup.first())
        assertFalse(settingsStore.referenceOnlyMode.first())
        assertFalse(settingsStore.deepAuditEnabled.first())
        assertFalse(settingsStore.multiProfileEnabled.first())
        scope.cancel()
    }

    private fun backup(
        profiles: List<Profile>,
        schemaVersion: Int = BackupFile.CURRENT_SCHEMA_VERSION,
        globalSettings: GlobalSettingsBackup? = null,
    ) = BackupFile(
        schemaVersion = schemaVersion,
        appVersion = "1.0",
        exportedAt = 1_700_000_000_000L,
        profiles = profiles,
        globalSettings = globalSettings,
    )

    private fun testScope(): CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun dataStore(scope: CoroutineScope, fileName: String) = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(temporaryFolder.root, fileName) },
    )
}

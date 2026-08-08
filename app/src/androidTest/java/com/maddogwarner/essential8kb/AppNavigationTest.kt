package com.maddogwarner.essential8kb

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var testScope: CoroutineScope
    private lateinit var testDataStoreFile: File
    private lateinit var testDataStore: DataStore<Preferences>

    @Before
    fun createIsolatedDataStore() {
        testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        testDataStoreFile = File(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir,
            "app-navigation-${UUID.randomUUID()}.preferences_pb",
        )
        testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testDataStoreFile },
        )
    }

    @After
    fun cancelIsolatedDataStoreScope() {
        testScope.cancel()
        testDataStoreFile.delete()
    }

    @Test
    fun homeRendersControlsAndOpensDetail() {
        setIsolatedAppContent()

        // Wait for Splash screen and dismiss it
        composeRule.onNodeWithText("Get Started").performClick()

        composeRule.onNodeWithText("Application Control").assertExists()
        scrollToText("Regular Backups")
        composeRule.onNodeWithText("Regular Backups").assertExists()

        scrollToText("Application Control")
        composeRule.onNodeWithText("Application Control").performClick()

        composeRule.onNodeWithText("Mitigation 1").assertExists()
        composeRule.onNodeWithText("Overview").assertExists()
    }

    @Test
    fun targetSegmentPickerChangesPendingStepsCount() {
        setIsolatedAppContent()

        // Dismiss onboarding splash
        composeRule.onNodeWithText("Get Started").performClick()

        // Default target is ML3, so all 67 steps are pending
        composeRule.onNodeWithText("Pending: 67", useUnmergedTree = true).assertExists()

        // Click on ML1 segment
        composeRule.onNodeWithText("ML1").performClick()

        // ML1 pending steps total should be 27
        composeRule.onNodeWithText("Pending: 27", useUnmergedTree = true).assertExists()
    }

    @Test
    fun beyondTargetBadgesShowOnControlDetailPage() {
        setIsolatedAppContent()

        // Dismiss onboarding splash
        composeRule.onNodeWithText("Get Started").performClick()

        // Click on ML1 segment
        composeRule.onNodeWithText("ML1").performClick()

        // Open Application Control
        composeRule.onNodeWithText("Application Control").performClick()

        // Since target is ML1, both the ML2 and ML3 rows show "Beyond target".
        scrollToText("Maturity Level 3")
        composeRule.onAllNodesWithText("Beyond target").assertCountEquals(2)
    }

    @Test
    fun microsoft365SelectionUpdatesSettingsAndDrivesMaturityAdditions() {
        runBlocking { SettingsStore(testDataStore).setShowSplashOnStartup(false) }
        setIsolatedAppContent()

        scrollToText("M365 Additional Controls")
        composeRule.onNodeWithText("M365 Additional Controls").performClick()

        composeRule.onNodeWithText("None").assertExists()
        composeRule.onNodeWithText("E3").assertExists().performClick()
        composeRule.onNodeWithText("P1").assertExists()
        composeRule.onNodeWithText("P2").assertExists()
        composeRule.onNodeWithText("Current mode: E3 + P1").assertExists()

        composeRule.onNodeWithText("E5").performClick()
        composeRule.onNodeWithText("Current mode: E5").assertExists()
        composeRule.onNodeWithText("P1").assertDoesNotExist()
        composeRule.onNodeWithText("P2").assertDoesNotExist()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { ProgressStore(testDataStore).licenseMode.first() } == Microsoft365LicenseMode.E5
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        scrollToText("Application Control")
        composeRule.onNodeWithText("Application Control").performClick()
        composeRule.onNodeWithText("Maturity Level 1").performClick()
        scrollToText("M365 / MDE additions")
        composeRule.onNodeWithText("M365 / MDE additions").assertExists()
    }

    @Test
    fun aboutShowsPrivacyReferencesAndActionableAsdLink() {
        runBlocking { SettingsStore(testDataStore).setShowSplashOnStartup(false) }
        setIsolatedAppContent()

        scrollToText("About & Privacy")
        composeRule.onNodeWithText("About & Privacy").performClick()

        scrollToText("Privacy Policy")
        composeRule.onNodeWithText("Privacy Policy").assertExists()
        scrollToText("References")
        composeRule.onNodeWithText("References").assertExists()
        scrollToText("ASD Essential Eight maturity model")
        composeRule.onNodeWithText("ASD Essential Eight maturity model")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun searchScreenFiltersAndHighlightsISMControl() {
        setIsolatedAppContent()

        // Dismiss onboarding splash
        composeRule.onNodeWithText("Get Started").performClick()

        // Open Search from topbar
        composeRule.onNodeWithContentDescription("Search").performClick()

        // Search textfield exists, type "ISM-1490"
        composeRule.onNodeWithText("Search GPOs, registries, commands, ISM or ATT&CK IDs…").performTextInput("ISM-1490")

        // Result matching "Extend AppLocker enforcement to servers" and "Deploy Windows Defender Application Control (WDAC)" should show
        composeRule.onNodeWithText("Extend AppLocker enforcement to servers").assertExists()
        composeRule.onNodeWithText("Deploy Windows Defender Application Control (WDAC)").assertExists()

        // Tap one search result
        composeRule.onNodeWithText("Extend AppLocker enforcement to servers").performClick()

        // Verifies it navigated to detail view: Mitigation 1 - ML2
        composeRule.onNodeWithText("Application Control — ML2").assertExists()
    }

    @Test
    fun searchScreenFindsStepsByAttackTechniqueId() {
        runBlocking { SettingsStore(testDataStore).setShowSplashOnStartup(false) }
        setIsolatedAppContent()

        composeRule.onNodeWithContentDescription("Search").performClick()
        composeRule.onNodeWithText("Search GPOs, registries, commands, ISM or ATT&CK IDs…")
            .performTextInput("T1059")

        composeRule.onNodeWithText("Enable the Application Identity service").assertExists()
        composeRule.onNodeWithText(
            "T1059 — Command and Scripting Interpreter (Support)",
        ).assertExists()
    }

    @Test
    fun attackCoverageOpensTechniqueDetail() {
        runBlocking { SettingsStore(testDataStore).setShowSplashOnStartup(false) }
        setIsolatedAppContent()

        scrollToText("MITRE ATT&CK® Coverage")
        composeRule.onNodeWithText("MITRE ATT&CK® Coverage").performClick()
        composeRule.onNodeWithText("Summary").assertExists()
        scrollToText("T1059")
        composeRule.onNodeWithText("T1059").performClick()

        composeRule.onNodeWithText("ATT&CK Technique").assertExists()
        composeRule.onNodeWithText("Command and Scripting Interpreter").assertExists()
        scrollToText("View on attack.mitre.org")
        composeRule.onNodeWithText("View on attack.mitre.org").assertHasClickAction()
    }

    @Test
    fun referenceOnlyModeHidesAttackCoverageRow() {
        runBlocking {
            SettingsStore(testDataStore).setShowSplashOnStartup(false)
            SettingsStore(testDataStore).setReferenceOnlyMode(true)
        }
        setIsolatedAppContent()

        scrollToText("Settings")
        composeRule.onNodeWithText("MITRE ATT&CK® Coverage").assertDoesNotExist()
    }

    @Test
    fun resetAppDataWipesUserProgress() {
        setIsolatedAppContent()

        // Dismiss onboarding splash
        composeRule.onNodeWithText("Get Started").performClick()

        // Change target to ML1, verifying pending is 27
        composeRule.onNodeWithText("ML1").performClick()
        composeRule.onNodeWithText("Pending: 27", useUnmergedTree = true).assertExists()

        // Open About screen
        scrollToText("About & Privacy")
        composeRule.onNodeWithText("About & Privacy").performClick()

        // Verify Reset App Data exists
        scrollToText("Reset App Data")
        composeRule.onNodeWithText("Reset App Data").assertExists()
        composeRule.onNodeWithText("Reset App Data").performClick()

        // Confirmation alert shows, click Reset
        composeRule.onNodeWithText("Reset").performClick()

        // Confirm splash is active again (default settings restored)
        composeRule.onNodeWithText("Get Started").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        // Target maturity level should default back to ML3, restoring pending count to 67
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Pending: 67", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Pending: 67", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aboutShowsScopePickerAndAssessmentFeatureControls() {
        setIsolatedAppContent()
        composeRule.onNodeWithText("Get Started").performClick()

        scrollToText("About & Privacy")
        composeRule.onNodeWithText("About & Privacy").performClick()

        scrollToText("Assessment Features")
        composeRule.onNodeWithText("Assessment Features").assertExists()
        composeRule.onNodeWithText("Multiple Profiles").assertExists()
        composeRule.onNodeWithText("Deep Audit Mode").assertExists()
        scrollToText("Backup & Restore")
        composeRule.onNodeWithText("Backup & Restore").assertExists()
        composeRule.onNodeWithText("Export Backup").assertExists()
        composeRule.onNodeWithText("Import Backup").assertExists()
        scrollToText("OS Scope")
        composeRule.onNodeWithText("OS Scope").assertExists()
        composeRule.onNodeWithText("Workstation").assertExists()
        composeRule.onNodeWithText("Server").assertExists()
        composeRule.onNodeWithText("Both").assertExists()
    }

    @Test
    fun profilesScreenCreatesAndActivatesProfile() {
        runBlocking { SettingsStore(testDataStore).setMultiProfileEnabled(true) }
        setIsolatedAppContent()
        composeRule.onNodeWithText("Get Started").performClick()

        scrollToText("About & Privacy")
        composeRule.onNodeWithText("About & Privacy").performClick()
        scrollToText("Profiles")
        composeRule.onNodeWithText("Profiles").performClick()
        composeRule.onNodeWithContentDescription("Add Profile").performClick()
        composeRule.onNodeWithText("Profile name").performTextInput("Hospital A")
        composeRule.onNodeWithText("Create").performClick()

        composeRule.onNodeWithText("Hospital A").assertExists()
        composeRule.onNodeWithContentDescription("Active profile").assertExists()

        composeRule.onNodeWithContentDescription("Back").performClick()
        scrollToText("Profiles")
        composeRule.onNodeWithText("Profiles").performClick()
        composeRule.onNodeWithText("Create Profile").assertDoesNotExist()
    }

    @Test
    fun multiProfileExportChooserHasExplicitCancelAction() {
        runBlocking { ProgressStore(testDataStore).createProfile("Second") }
        setIsolatedAppContent()
        composeRule.onNodeWithText("Get Started").performClick()

        scrollToText("About & Privacy")
        composeRule.onNodeWithText("About & Privacy").performClick()
        scrollToText("Export Backup")
        composeRule.onNodeWithText("Export Backup").performClick()

        composeRule.onNodeWithText("This profile only").assertExists()
        composeRule.onNodeWithText("All profiles").assertExists()
        composeRule.onNodeWithText("Cancel").assertExists().performClick()
        composeRule.onNodeWithText("This profile only").assertDoesNotExist()
    }

    @Test
    fun persistedSplashPreferenceSuppressesSplash() {
        runBlocking { SettingsStore(testDataStore).setShowSplashOnStartup(false) }
        setIsolatedAppContent()

        composeRule.onNodeWithText("Get Started").assertDoesNotExist()
        composeRule.onNodeWithText("Application Control").assertExists()
    }

    private fun setIsolatedAppContent() {
        composeRule.setContent {
            AppRoot(
                progressStoreOverride = ProgressStore(testDataStore),
                settingsStoreOverride = SettingsStore(testDataStore),
            )
        }
    }

    private fun scrollToText(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }
}

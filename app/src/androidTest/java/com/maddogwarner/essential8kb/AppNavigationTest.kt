package com.maddogwarner.essential8kb

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.platform.app.InstrumentationRegistry
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

        composeRule.onNodeWithText("Application Control").assertExists()
        composeRule.onNodeWithText("Regular Backups").assertExists()

        composeRule.onNodeWithText("Application Control").performClick()

        composeRule.onNodeWithText("Mitigation 1").assertExists()
        composeRule.onNodeWithText("Overview").assertExists()
    }

    @Test
    fun microsoft365SelectionUpdatesActiveMode() {
        setIsolatedAppContent()

        composeRule.onNodeWithText("M365 Additional Controls").performClick()
        composeRule.onNodeWithText("E3").performClick()
        composeRule.onNodeWithText("P2").assertExists()

        composeRule.onNodeWithText("E5").performClick()

        composeRule.onNodeWithText("Current mode: E5").assertExists()
    }

    @Test
    fun aboutScreenShowsPrivacyAndReferences() {
        setIsolatedAppContent()

        composeRule.onNodeWithText("About & Privacy").performClick()

        composeRule.onNodeWithText("Privacy Policy").assertExists()
        composeRule.onNodeWithText("References").assertExists()
        composeRule.onNodeWithText("ASD Essential Eight maturity model").assertExists()
    }

    private fun setIsolatedAppContent() {
        composeRule.setContent {
            AppRoot(
                progressStoreOverride = ProgressStore(testDataStore),
                settingsStoreOverride = SettingsStore(testDataStore),
            )
        }
    }
}

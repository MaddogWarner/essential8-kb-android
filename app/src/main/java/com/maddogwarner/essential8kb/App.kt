package com.maddogwarner.essential8kb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import com.maddogwarner.essential8kb.store.essential8DataStore
import com.maddogwarner.essential8kb.ui.about.AboutScreen
import com.maddogwarner.essential8kb.ui.audit.AuditPolicyScreen
import com.maddogwarner.essential8kb.ui.detail.ControlDetailScreen
import com.maddogwarner.essential8kb.ui.home.HomeScreen
import com.maddogwarner.essential8kb.ui.m365.Microsoft365SettingsScreen
import com.maddogwarner.essential8kb.ui.maturity.MaturityLevelScreen
import com.maddogwarner.essential8kb.ui.theme.Essential8Theme
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Home : Screen
    data class ControlDetail(val control: EssentialControl) : Screen
    data class MaturityLevelDetail(
        val control: EssentialControl,
        val level: MaturityLevel,
        val content: MaturityLevelContent,
    ) : Screen
    data object AuditPolicy : Screen
    data object Microsoft365Settings : Screen
    data object About : Screen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    progressStoreOverride: ProgressStore? = null,
    settingsStoreOverride: SettingsStore? = null,
) {
    Essential8Theme {
        Surface {
            val context = LocalContext.current
            val defaultProgressStore = remember(context) { ProgressStore(context.essential8DataStore) }
            val defaultSettingsStore = remember(context) { SettingsStore(context.essential8DataStore) }
            val progressStore = progressStoreOverride ?: defaultProgressStore
            val settingsStore = settingsStoreOverride ?: defaultSettingsStore
            val scope = rememberCoroutineScope()
            val completedStepIds by progressStore.completedStepIds.collectAsState(initial = emptySet())
            val selectedLicenseMode by settingsStore.licenseMode.collectAsState(initial = Microsoft365LicenseMode.NONE)
            val backStack = remember { mutableStateListOf<Screen>() }
            val currentScreen = backStack.lastOrNull() ?: Screen.Home

            fun navigate(screen: Screen) {
                backStack.add(screen)
            }

            fun goBack() {
                if (backStack.isNotEmpty()) {
                    backStack.removeAt(backStack.lastIndex)
                }
            }

            BackHandler(enabled = backStack.isNotEmpty()) {
                goBack()
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(currentScreen.title()) },
                        navigationIcon = {
                            if (backStack.isNotEmpty()) {
                                IconButton(onClick = ::goBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                when (val screen = currentScreen) {
                    Screen.Home -> HomeScreen(
                        controls = EssentialControlsData.all,
                        completedStepIds = completedStepIds,
                        onControlSelected = { navigate(Screen.ControlDetail(it)) },
                        onAuditPolicySelected = { navigate(Screen.AuditPolicy) },
                        onMicrosoft365Selected = { navigate(Screen.Microsoft365Settings) },
                        onAboutSelected = { navigate(Screen.About) },
                        modifier = Modifier.padding(innerPadding),
                    )

                    is Screen.ControlDetail -> ControlDetailScreen(
                        control = screen.control,
                        completedStepIds = completedStepIds,
                        onMaturityLevelSelected = { level, content ->
                            navigate(Screen.MaturityLevelDetail(screen.control, level, content))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    is Screen.MaturityLevelDetail -> MaturityLevelScreen(
                        control = screen.control,
                        level = screen.level,
                        content = screen.content,
                        completedStepIds = completedStepIds,
                        selectedLicenseMode = selectedLicenseMode,
                        onToggleStep = { stepId ->
                            scope.launch { progressStore.toggle(stepId) }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.AuditPolicy -> AuditPolicyScreen(
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.Microsoft365Settings -> Microsoft365SettingsScreen(
                        selectedMode = selectedLicenseMode,
                        onModeSelected = { mode ->
                            scope.launch { settingsStore.setLicenseMode(mode) }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.About -> AboutScreen(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

private fun Screen.title(): String =
    when (this) {
        Screen.Home -> "Essential 8 Knowledge Base"
        is Screen.ControlDetail -> "Mitigation ${control.id}"
        is Screen.MaturityLevelDetail -> "${control.name} - ${level.shortName}"
        Screen.AuditPolicy -> "Windows Audit Policy"
        Screen.Microsoft365Settings -> "M365 Additional Controls"
        Screen.About -> "About Essential 8"
    }

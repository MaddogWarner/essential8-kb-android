package com.maddogwarner.essential8kb

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.MaturityLevelContent
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.ProgressStore
import com.maddogwarner.essential8kb.store.SettingsStore
import com.maddogwarner.essential8kb.store.AuditEntry
import com.maddogwarner.essential8kb.store.essential8DataStore
import com.maddogwarner.essential8kb.ui.about.AboutScreen
import com.maddogwarner.essential8kb.ui.audit.AuditPolicyScreen
import com.maddogwarner.essential8kb.ui.audit.StepAuditHistoryScreen
import com.maddogwarner.essential8kb.ui.detail.ControlDetailScreen
import com.maddogwarner.essential8kb.ui.home.HomeScreen
import com.maddogwarner.essential8kb.ui.m365.Microsoft365SettingsScreen
import com.maddogwarner.essential8kb.ui.maturity.MaturityLevelScreen
import com.maddogwarner.essential8kb.ui.profiles.ProfilesScreen
import com.maddogwarner.essential8kb.ui.search.GlobalSearchScreen
import com.maddogwarner.essential8kb.ui.splash.SplashDialog
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
    data object GlobalSearch : Screen
    data object Profiles : Screen
    data class StepAuditHistory(
        val stepTitle: String,
        val entries: List<AuditEntry>,
    ) : Screen
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
            val progressStore = remember(progressStoreOverride, context) {
                progressStoreOverride ?: ProgressStore(context.essential8DataStore)
            }
            val settingsStore = remember(settingsStoreOverride, context) {
                settingsStoreOverride ?: SettingsStore(context.essential8DataStore)
            }
            val scope = rememberCoroutineScope()

            val stepStatuses by progressStore.stepStatuses.collectAsState(initial = emptyMap())
            val selectedLicenseMode by progressStore.licenseMode.collectAsState(initial = Microsoft365LicenseMode.NONE)
            val targetLevel by progressStore.targetMaturityLevel.collectAsState(initial = MaturityLevel.ML3)
            val osScope by progressStore.osScope.collectAsState(initial = OSScope.BOTH)
            val profiles by progressStore.profiles.collectAsState(initial = emptyList())
            val activeProfile by progressStore.activeProfile.collectAsState(initial = null)
            val showSplashOnStartup by settingsStore.showSplashOnStartup.collectAsState(initial = null)
            val referenceOnlyMode by settingsStore.referenceOnlyMode.collectAsState(initial = false)
            val deepAuditEnabled by settingsStore.deepAuditEnabled.collectAsState(initial = false)
            val multiProfileEnabled by settingsStore.multiProfileEnabled.collectAsState(initial = false)

            val backStack = remember { mutableStateListOf<Screen>() }
            val currentScreen = backStack.lastOrNull() ?: Screen.Home

            var isShowingSplash by remember { mutableStateOf(false) }
            var hasShownSplashThisSession by remember { mutableStateOf(false) }
            var showingCreateProfile by remember { mutableStateOf(false) }

            LaunchedEffect(showSplashOnStartup) {
                if (showSplashOnStartup == true && !hasShownSplashThisSession) {
                    hasShownSplashThisSession = true
                    isShowingSplash = true
                }
            }

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
                        actions = {
                            if (currentScreen == Screen.Home) {
                                IconButton(onClick = { navigate(Screen.GlobalSearch) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search",
                                    )
                                }
                            }
                            if (currentScreen == Screen.Profiles) {
                                IconButton(onClick = { showingCreateProfile = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add Profile",
                                    )
                                }
                            }
                        }
                    )
                },
            ) { innerPadding ->
                when (val screen = currentScreen) {
                    Screen.Home -> HomeScreen(
                        controls = EssentialControlsData.all,
                        stepStatuses = stepStatuses,
                        progressStore = progressStore,
                        targetLevel = targetLevel,
                        osScope = osScope,
                        onTargetLevelChanged = { level ->
                            scope.launch { progressStore.setTargetMaturityLevel(level) }
                        },
                        referenceOnlyMode = referenceOnlyMode,
                        onControlSelected = { navigate(Screen.ControlDetail(it)) },
                        onAuditPolicySelected = { navigate(Screen.AuditPolicy) },
                        onMicrosoft365Selected = { navigate(Screen.Microsoft365Settings) },
                        onAboutSelected = { navigate(Screen.About) },
                        modifier = Modifier.padding(innerPadding),
                    )

                    is Screen.ControlDetail -> ControlDetailScreen(
                        control = screen.control,
                        stepStatuses = stepStatuses,
                        progressStore = progressStore,
                        targetLevel = targetLevel,
                        osScope = osScope,
                        onMaturityLevelSelected = { level, content ->
                            navigate(Screen.MaturityLevelDetail(screen.control, level, content))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    is Screen.MaturityLevelDetail -> MaturityLevelScreen(
                        control = screen.control,
                        level = screen.level,
                        content = screen.content,
                        stepStatuses = stepStatuses,
                        progressStore = progressStore,
                        selectedLicenseMode = selectedLicenseMode,
                        osScope = osScope,
                        deepAuditEnabled = deepAuditEnabled,
                        auditTrail = activeProfile?.auditTrail.orEmpty(),
                        onStatusChanged = { stepId, state, reason, note ->
                            scope.launch { progressStore.setStatus(state, reason, note, stepId) }
                        },
                        onAuditHistorySelected = { stepTitle, entries ->
                            navigate(Screen.StepAuditHistory(stepTitle, entries))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.AuditPolicy -> AuditPolicyScreen(
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.Microsoft365Settings -> Microsoft365SettingsScreen(
                        selectedMode = selectedLicenseMode,
                        onModeSelected = { mode ->
                            scope.launch { progressStore.setLicenseMode(mode) }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.About -> AboutScreen(
                        osScope = osScope,
                        onOSScopeChanged = { selectedScope ->
                            scope.launch { progressStore.setOSScope(selectedScope) }
                        },
                        referenceOnlyMode = referenceOnlyMode,
                        onReferenceOnlyModeChanged = { referenceOnly ->
                            scope.launch { settingsStore.setReferenceOnlyMode(referenceOnly) }
                        },
                        deepAuditEnabled = deepAuditEnabled,
                        onDeepAuditEnabledChanged = { enabled ->
                            scope.launch { settingsStore.setDeepAuditEnabled(enabled) }
                        },
                        multiProfileEnabled = multiProfileEnabled,
                        onMultiProfileEnabledChanged = { enabled ->
                            scope.launch { settingsStore.setMultiProfileEnabled(enabled) }
                        },
                        activeProfileName = activeProfile?.name ?: "Default",
                        onProfilesSelected = { navigate(Screen.Profiles) },
                        onResetAppData = {
                            scope.launch {
                                progressStore.resetAll()
                                hasShownSplashThisSession = false
                                isShowingSplash = true
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.GlobalSearch -> GlobalSearchScreen(
                        onStepSelected = { control, level, content ->
                            navigate(Screen.MaturityLevelDetail(control, level, content))
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    Screen.Profiles -> ProfilesScreen(
                        profiles = profiles,
                        activeProfileId = activeProfile?.id.orEmpty(),
                        showingCreate = showingCreateProfile,
                        onCreateDismissed = { showingCreateProfile = false },
                        onSwitchProfile = { id ->
                            scope.launch {
                                progressStore.switchProfile(id)
                                goBack()
                            }
                        },
                        onCreateProfile = { name ->
                            showingCreateProfile = false
                            scope.launch {
                                val id = progressStore.createProfile(name)
                                progressStore.switchProfile(id)
                            }
                        },
                        onRenameProfile = { id, name ->
                            scope.launch { progressStore.renameProfile(id, name) }
                        },
                        onDeleteProfile = { id ->
                            scope.launch { progressStore.deleteProfile(id) }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )

                    is Screen.StepAuditHistory -> StepAuditHistoryScreen(
                        entries = screen.entries,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }

            if (isShowingSplash) {
                SplashDialog(
                    showSplashOnStartup = showSplashOnStartup == true,
                    onShowSplashOnStartupChanged = { show ->
                        scope.launch { settingsStore.setShowSplashOnStartup(show) }
                    },
                    onDismiss = { isShowingSplash = false }
                )
            }
        }
    }
}

private fun Screen.title(): String =
    when (this) {
        Screen.Home -> "Essential 8 Knowledge Base"
        is Screen.ControlDetail -> "Mitigation ${control.id}"
        is Screen.MaturityLevelDetail -> "${control.name} — ${level.shortName}"
        Screen.AuditPolicy -> "Windows Audit Policy"
        Screen.Microsoft365Settings -> "M365 Additional Controls"
        Screen.About -> "About Essential 8"
        Screen.GlobalSearch -> "Global Search"
        Screen.Profiles -> "Profiles"
        is Screen.StepAuditHistory -> stepTitle
    }

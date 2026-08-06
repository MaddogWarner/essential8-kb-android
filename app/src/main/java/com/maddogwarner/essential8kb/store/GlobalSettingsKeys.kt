package com.maddogwarner.essential8kb.store

import androidx.datastore.preferences.core.booleanPreferencesKey

internal object GlobalSettingsKeys {
    val showSplashOnStartup = booleanPreferencesKey("showSplashOnStartup")
    val referenceOnlyMode = booleanPreferencesKey("referenceOnlyMode")
    val deepAuditEnabled = booleanPreferencesKey("deepAuditEnabled")
    val multiProfileEnabled = booleanPreferencesKey("multiProfileEnabled")
}

package com.maddogwarner.essential8kb.ui.maturity

import androidx.lifecycle.ViewModel
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365AdditionalControlsData
import com.maddogwarner.essential8kb.data.Microsoft365AdditionalProtection
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode

class MaturityLevelViewModel : ViewModel() {
    fun microsoft365Protections(
        control: EssentialControl,
        level: MaturityLevel,
        licenseMode: Microsoft365LicenseMode,
    ): List<Microsoft365AdditionalProtection> =
        Microsoft365AdditionalControlsData.protections(
            control.id,
            level,
            licenseMode,
        )
}

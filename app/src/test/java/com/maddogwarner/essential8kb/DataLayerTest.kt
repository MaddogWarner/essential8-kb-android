package com.maddogwarner.essential8kb

import com.maddogwarner.essential8kb.data.AppInformation
import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365AdditionalControlsData
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.WindowsAuditPolicyData
import com.maddogwarner.essential8kb.data.allStepIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataLayerTest {
    @Test
    fun essentialControlsMatchSourceCountsAndIds() {
        val controls = EssentialControlsData.all

        assertEquals(8, controls.size)
        assertEquals(controls.size, controls.map { it.id }.toSet().size)

        val allSteps = controls.flatMap { control ->
            MaturityLevel.entries.flatMap { level -> control.content(level).steps }
        }
        val stepIds = allSteps.map { it.id }

        assertEquals(67, allSteps.size)
        assertEquals(stepIds.size, stepIds.toSet().size)
        assertTrue(stepIds.all { it.matches(Regex("^[1-8]-[1-3]-\\d+$")) })

        controls.forEach { control ->
            assertTrue(control.name.isNotBlank())
            assertTrue(control.overview.isNotBlank())
            assertTrue(control.ml0Description.isNotBlank())

            MaturityLevel.entries.forEach { level ->
                val content = control.content(level)
                assertTrue(content.summary.isNotBlank())
                assertTrue(content.steps.isNotEmpty())

                content.steps.forEachIndexed { index, step ->
                    assertEquals("${control.id}-${level.level}-$index", step.id)
                    assertTrue(step.title.isNotBlank())
                    assertTrue(step.description.isNotBlank())
                }
            }
        }
    }

    @Test
    fun allStepIdsReturnsAllMaturityLevelStepIdsInOrder() {
        EssentialControlsData.all.forEach { control ->
            val expectedStepIds = listOf(control.ml1, control.ml2, control.ml3)
                .flatMap { content -> content.steps }
                .map { step -> step.id }

            assertEquals(expectedStepIds, control.allStepIds())
        }
    }

    @Test
    fun windowsAuditPolicyMatchesSourceCount() {
        assertEquals(21, WindowsAuditPolicyData.entries.size)
        assertEquals(
            WindowsAuditPolicyData.entries.size,
            WindowsAuditPolicyData.entries.map { it.id }.toSet().size,
        )
        assertTrue(WindowsAuditPolicyData.overview.isNotBlank())
    }

    @Test
    fun microsoft365ModeRawValuesMatchIosStorage() {
        assertEquals("none", Microsoft365LicenseMode.NONE.rawValue)
        assertEquals("e3P1", Microsoft365LicenseMode.E3_P1.rawValue)
        assertEquals("e3P2", Microsoft365LicenseMode.E3_P2.rawValue)
        assertEquals("e5", Microsoft365LicenseMode.E5.rawValue)
        assertEquals(Microsoft365LicenseMode.NONE, Microsoft365LicenseMode.fromRawValue("unexpected"))
    }

    @Test
    fun microsoft365ProtectionsAreCumulativeByMode() {
        val none = Microsoft365AdditionalControlsData.protections(
            controlID = 7,
            level = MaturityLevel.ML3,
            licenseMode = Microsoft365LicenseMode.NONE,
        )
        val e3P1 = Microsoft365AdditionalControlsData.protections(
            controlID = 7,
            level = MaturityLevel.ML3,
            licenseMode = Microsoft365LicenseMode.E3_P1,
        )
        val e3P2 = Microsoft365AdditionalControlsData.protections(
            controlID = 7,
            level = MaturityLevel.ML3,
            licenseMode = Microsoft365LicenseMode.E3_P2,
        )
        val e5 = Microsoft365AdditionalControlsData.protections(
            controlID = 7,
            level = MaturityLevel.ML3,
            licenseMode = Microsoft365LicenseMode.E5,
        )

        assertTrue(none.isEmpty())
        assertTrue(e3P1.isNotEmpty())
        assertTrue(e3P2.map { it.title }.containsAll(e3P1.map { it.title }))
        assertTrue(e5.map { it.title }.containsAll(e3P2.map { it.title }))
        assertTrue(e3P2.size > e3P1.size)
        assertTrue(e5.size > e3P2.size)
        assertTrue(e5.any { it.title.contains("E5") })
    }

    @Test
    fun microsoft365UniqueProtectionCountMatchesSource() {
        val uniqueTitles = EssentialControlsData.all.flatMap { control ->
            Microsoft365AdditionalControlsData.protections(
                controlID = control.id,
                level = MaturityLevel.ML1,
                licenseMode = Microsoft365LicenseMode.E5,
            )
        }.map { it.title }.toSet()

        assertEquals(18, uniqueTitles.size)
    }

    @Test
    fun aboutAndReferencesMatchAppScope() {
        assertTrue(AppInformation.aboutDescription.contains("administrators"))
        assertTrue(AppInformation.aboutDescription.contains("quick reference"))
        assertTrue(AppInformation.privacyPolicy.contains("does not collect"))
        assertTrue(AppInformation.privacyPolicy.contains("microphone"))
        assertTrue(AppInformation.privacyPolicy.contains("camera"))
        assertTrue(AppInformation.privacyPolicy.contains("location services"))

        val urls = AppInformation.referenceLinks.map { it.url }
        assertTrue(urls.any { it.contains("cyber.gov.au") && it.contains("essential-eight") })
        assertTrue(urls.any { it.contains("cyber.gov.au") && it.contains("ism") })
        assertTrue(urls.any { it.contains("learn.microsoft.com") && it.contains("defender-endpoint") })
        assertTrue(urls.any { it.contains("learn.microsoft.com") && it.contains("conditional-access") })
        assertFalse(AppInformation.referenceLinks.any { it.hostDisplayName.isBlank() })
    }
}

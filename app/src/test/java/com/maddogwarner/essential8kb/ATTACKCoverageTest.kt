package com.maddogwarner.essential8kb

import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.data.attack.ATTACKCatalogue
import com.maddogwarner.essential8kb.data.attack.ATTACKCoverageCalculator
import com.maddogwarner.essential8kb.data.attack.ATTACKMappingData
import com.maddogwarner.essential8kb.data.attack.ATTACKRelationship
import com.maddogwarner.essential8kb.data.attack.ATTACKTactic
import com.maddogwarner.essential8kb.data.attack.CoverageStatus
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus
import com.maddogwarner.essential8kb.ui.search.matchingDetails
import com.maddogwarner.essential8kb.ui.search.matchesSearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ATTACKCoverageTest {
    @Test
    fun catalogueHasVerifiedCountAndUniqueIds() {
        assertEquals(35, ATTACKCatalogue.all.size)
        assertEquals(35, ATTACKCatalogue.all.map { it.id }.toSet().size)
    }

    @Test
    fun tacticDisplayOrderAndNamesMatchSource() {
        assertEquals(
            listOf(
                "Initial Access",
                "Execution",
                "Persistence",
                "Privilege Escalation",
                "Stealth",
                "Defense Impairment",
                "Credential Access",
                "Lateral Movement",
                "Collection",
                "Impact",
            ),
            ATTACKTactic.entries.map { it.displayName },
        )
    }

    @Test
    fun mappingsFlattenToVerifiedCountAndHaveNoOrphans() {
        assertEquals(129, ATTACKMappingData.all.size)

        val catalogueIDs = ATTACKCatalogue.all.mapTo(mutableSetOf()) { it.id }
        assertTrue(ATTACKMappingData.all.all { it.techniqueID in catalogueIDs })

        val stepIDs = EssentialControlsData.all
            .flatMap { it.steps(MaturityLevel.ML3) }
            .mapTo(mutableSetOf()) { it.id }
        assertTrue(ATTACKMappingData.all.all { it.stepID in stepIDs })
        assertEquals(65, ATTACKMappingData.all.map { it.stepID }.toSet().size)
        assertFalse("2-1-2" in ATTACKMappingData.all.map { it.stepID })
        assertFalse("6-1-2" in ATTACKMappingData.all.map { it.stepID })
    }

    @Test
    fun everyCatalogueTechniqueIsMapped() {
        val mappedIDs = ATTACKMappingData.all.mapTo(mutableSetOf()) { it.techniqueID }
        assertTrue(ATTACKCatalogue.all.all { it.id in mappedIDs })
    }

    @Test
    fun supportIsTheOnlyRelationshipExcludedFromCoverage() {
        ATTACKRelationship.entries.forEach { relationship ->
            assertEquals(
                relationship.name,
                relationship != ATTACKRelationship.SUPPORT,
                relationship.countsTowardCoverage,
            )
        }
    }

    @Test
    fun supportOnlyTechniqueReportsIndirect() {
        val coverage = coverage("T1566.002")

        assertNotNull(coverage)
        assertEquals(CoverageStatus.INDIRECT, coverage?.status)
    }

    @Test
    fun allNotApplicableContributingStepsReportNotApplicable() {
        val technique = ATTACKCatalogue.technique("T1003.001")!!
        val contributingIDs = ATTACKMappingData.mappingsForTechnique(technique.id)
            .mapTo(mutableSetOf()) { it.stepID }
        val statuses = contributingIDs.associateWith { StepStatus(StepState.NOT_APPLICABLE) }

        val coverage = ATTACKCoverageCalculator.coverage(
            technique,
            MaturityLevel.ML3,
            OSScope.BOTH,
            statuses,
        )

        assertEquals(CoverageStatus.NOT_APPLICABLE, coverage?.status)
    }

    @Test
    fun allImplementedContributingStepsReportCovered() {
        val technique = ATTACKCatalogue.technique("T1059")!!
        val contributingIDs = ATTACKMappingData.mappingsForTechnique(technique.id)
            .filter { mapping -> mapping.relationships.any { it.countsTowardCoverage } }
            .mapTo(mutableSetOf()) { it.stepID }
        val statuses = contributingIDs.associateWith { StepStatus(StepState.IMPLEMENTED) }

        val coverage = ATTACKCoverageCalculator.coverage(
            technique,
            MaturityLevel.ML3,
            OSScope.BOTH,
            statuses,
        )

        assertEquals(CoverageStatus.COVERED, coverage?.status)
    }

    @Test
    fun multiTacticTechniqueIsCountedOnceInSummary() {
        val groups = ATTACKCoverageCalculator.grouped(
            MaturityLevel.ML3,
            OSScope.BOTH,
            emptyMap(),
        )
        assertEquals(4, groups.count { group -> group.coverage.any { it.id == "T1078" } })
        assertEquals(1, ATTACKCoverageCalculator.uniqueCoverage(groups).count { it.id == "T1078" })
    }

    @Test
    fun targetMaturityAndOsScopeChangeListedTechniques() {
        val ml1 = ATTACKCoverageCalculator.allCoverage(MaturityLevel.ML1, OSScope.BOTH, emptyMap())
            .map { it.id }
        val ml3 = ATTACKCoverageCalculator.allCoverage(MaturityLevel.ML3, OSScope.BOTH, emptyMap())
            .map { it.id }
        val workstation = ATTACKCoverageCalculator.allCoverage(MaturityLevel.ML3, OSScope.WORKSTATION, emptyMap())
            .map { it.id }
        val server = ATTACKCoverageCalculator.allCoverage(MaturityLevel.ML3, OSScope.SERVER, emptyMap())
            .map { it.id }

        assertNotEquals(ml1, ml3)
        assertNotEquals(workstation, server)
    }

    @Test
    fun globalSearchFindsMappedStepsByAttackId() {
        val mappedStepIDs = ATTACKMappingData.mappingsForTechnique("T1059").mapTo(mutableSetOf()) { it.stepID }
        val matchingSteps = EssentialControlsData.all.flatMap { it.steps(MaturityLevel.ML3) }
            .filter { it.matchesSearchQuery("T1059") }

        assertTrue(matchingSteps.map { it.id }.containsAll(mappedStepIDs))
        assertTrue(matchingSteps.flatMap { it.matchingDetails("T1059") }.any {
            it == "T1059 — Command and Scripting Interpreter (Support)"
        })
    }

    private fun coverage(techniqueID: String) = ATTACKCoverageCalculator.coverage(
        ATTACKCatalogue.technique(techniqueID)!!,
        MaturityLevel.ML3,
        OSScope.BOTH,
        emptyMap(),
    )
}

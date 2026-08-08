package com.maddogwarner.essential8kb.data.attack

import com.maddogwarner.essential8kb.data.EssentialControlsData
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.OSScope
import com.maddogwarner.essential8kb.store.StepState
import com.maddogwarner.essential8kb.store.StepStatus

data class TechniqueCoverage(
    val technique: ATTACKTechnique,
    val contributingTotal: Int,
    val contributingImplemented: Int,
    val supportingTotal: Int,
    val isIndirectOnly: Boolean,
    val isNotApplicable: Boolean,
) {
    val id: String get() = technique.id
    val fraction: Double? get() = contributingTotal.takeIf { it > 0 }?.let {
        contributingImplemented.toDouble() / it.toDouble()
    }
    val status: CoverageStatus get() = when {
        isIndirectOnly -> CoverageStatus.INDIRECT
        isNotApplicable -> CoverageStatus.NOT_APPLICABLE
        contributingImplemented == contributingTotal -> CoverageStatus.COVERED
        contributingImplemented > 0 -> CoverageStatus.PARTIAL
        else -> CoverageStatus.NOT_COVERED
    }
}

enum class CoverageStatus(val displayName: String) {
    COVERED("Covered"),
    PARTIAL("Partial"),
    NOT_COVERED("Not covered"),
    INDIRECT("Indirect"),
    NOT_APPLICABLE("Not applicable"),
}

data class TacticCoverage(
    val tactic: ATTACKTactic,
    val coverage: List<TechniqueCoverage>,
)

object ATTACKCoverageCalculator {
    fun coverage(
        technique: ATTACKTechnique,
        targetLevel: MaturityLevel,
        osScope: OSScope,
        statuses: Map<String, StepStatus>,
    ): TechniqueCoverage? = coverage(
        technique = technique,
        inScopeStepIDs = inScopeStepIDs(targetLevel, osScope),
        statuses = statuses,
    )

    private fun coverage(
        technique: ATTACKTechnique,
        inScopeStepIDs: Set<String>,
        statuses: Map<String, StepStatus>,
    ): TechniqueCoverage? {
        val inScopeMappings = ATTACKMappingData.mappingsForTechnique(technique.id)
            .filter { it.stepID in inScopeStepIDs }
        if (inScopeMappings.isEmpty()) return null

        val contributingIDs = inScopeMappings
            .filter { mapping -> mapping.relationships.any(ATTACKRelationship::countsTowardCoverage) }
            .mapTo(mutableSetOf(), ATTACKMapping::stepID)
        val supportingIDs = inScopeMappings
            .filter { ATTACKRelationship.SUPPORT in it.relationships }
            .mapTo(mutableSetOf(), ATTACKMapping::stepID)
        val applicableContributing = contributingIDs.filterNot { statuses[it]?.state == StepState.NOT_APPLICABLE }
        val applicableSupporting = supportingIDs.filterNot { statuses[it]?.state == StepState.NOT_APPLICABLE }

        return TechniqueCoverage(
            technique = technique,
            contributingTotal = applicableContributing.size,
            contributingImplemented = applicableContributing.count {
                statuses[it]?.state == StepState.IMPLEMENTED
            },
            supportingTotal = applicableSupporting.size,
            isIndirectOnly = contributingIDs.isEmpty(),
            isNotApplicable = contributingIDs.isNotEmpty() && applicableContributing.isEmpty(),
        )
    }

    fun allCoverage(
        targetLevel: MaturityLevel,
        osScope: OSScope,
        statuses: Map<String, StepStatus>,
    ): List<TechniqueCoverage> {
        // Deliberately computed once for the whole catalogue pass.
        val stepIDs = inScopeStepIDs(targetLevel, osScope)
        return ATTACKCatalogue.all.mapNotNull { technique -> coverage(technique, stepIDs, statuses) }
    }

    fun techniquesForControl(
        controlID: Int,
        targetLevel: MaturityLevel,
        osScope: OSScope,
    ): List<ATTACKTechnique> {
        val control = EssentialControlsData.all.firstOrNull { it.id == controlID } ?: return emptyList()
        val stepIDs = control.steps(targetLevel, osScope).mapTo(mutableSetOf()) { it.id }
        val techniqueIDs = ATTACKMappingData.all
            .filter { it.stepID in stepIDs }
            .mapTo(mutableSetOf(), ATTACKMapping::techniqueID)
        return ATTACKCatalogue.all.filter { it.id in techniqueIDs }
    }

    fun grouped(
        targetLevel: MaturityLevel,
        osScope: OSScope,
        statuses: Map<String, StepStatus>,
        controlFilter: Int? = null,
    ): List<TacticCoverage> {
        val allowed = controlFilter?.let { controlID ->
            techniquesForControl(controlID, targetLevel, osScope).mapTo(mutableSetOf(), ATTACKTechnique::id)
        }
        val coverage = allCoverage(targetLevel, osScope, statuses)
            .filter { allowed == null || it.id in allowed }
        return ATTACKTactic.entries.mapNotNull { tactic ->
            coverage.filter { tactic in it.technique.tactics }
                .takeIf(List<TechniqueCoverage>::isNotEmpty)
                ?.let { TacticCoverage(tactic, it) }
        }
    }

    fun uniqueCoverage(groups: List<TacticCoverage>): List<TechniqueCoverage> =
        groups.flatMap(TacticCoverage::coverage).distinctBy(TechniqueCoverage::id)

    private fun inScopeStepIDs(targetLevel: MaturityLevel, osScope: OSScope): Set<String> =
        EssentialControlsData.all.flatMap { it.steps(targetLevel, osScope) }.mapTo(mutableSetOf()) { it.id }
}

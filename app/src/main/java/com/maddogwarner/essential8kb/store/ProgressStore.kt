package com.maddogwarner.essential8kb.store

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.ImplementationStep
import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

val Context.essential8DataStore: DataStore<Preferences> by preferencesDataStore(
    name = "essential8_preferences",
)

enum class StepState(val rawValue: String) {
    NOT_IMPLEMENTED("Not Implemented"),
    IMPLEMENTED("Implemented"),
    NOT_APPLICABLE("Not Applicable");

    companion object {
        fun fromRawValue(value: String?): StepState =
            entries.firstOrNull { it.rawValue == value } ?: NOT_IMPLEMENTED
    }
}

data class StepStatus(
    val state: StepState,
    val reason: String? = null,
)

data class AuditEntry(
    val id: String,
    val timestamp: Long,
    val previousState: StepState,
    val newState: StepState,
    val note: String?,
)

data class Profile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val stepProgress: Map<String, StepStatus>,
    val auditTrail: Map<String, List<AuditEntry>>,
    val targetMaturityLevelRaw: Int,
    val osScopeFilterRaw: String,
    val microsoft365LicenseModeRaw: String,
) {
    companion object {
        fun newDefault(name: String = "Default"): Profile = Profile(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            stepProgress = emptyMap(),
            auditTrail = emptyMap(),
            targetMaturityLevelRaw = MaturityLevel.ML3.level,
            osScopeFilterRaw = OSScope.BOTH.rawValue,
            microsoft365LicenseModeRaw = Microsoft365LicenseMode.NONE.rawValue,
        )
    }
}

const val AUDIT_ENTRIES_PER_STEP_CAP = 200

private data class ProfileState(
    val profiles: List<Profile>,
    val activeProfileId: String,
) {
    val activeProfile: Profile = profiles.firstOrNull { it.id == activeProfileId } ?: profiles.first()
}

class ProgressStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val migrationDefault = Profile.newDefault()

    private val profileState: Flow<ProfileState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map(::stateFromPreferences)

    val profiles: Flow<List<Profile>> = profileState.map { it.profiles }
    val activeProfileId: Flow<String> = profileState.map { it.activeProfileId }
    val activeProfile: Flow<Profile> = profileState.map { it.activeProfile }
    val stepStatuses: Flow<Map<String, StepStatus>> = activeProfile.map { it.stepProgress }
    val targetMaturityLevel: Flow<MaturityLevel> = activeProfile.map { profile ->
        MaturityLevel.entries.firstOrNull { it.level == profile.targetMaturityLevelRaw } ?: MaturityLevel.ML3
    }
    val osScope: Flow<OSScope> = activeProfile.map { OSScope.fromRawValue(it.osScopeFilterRaw) }
    val licenseMode: Flow<Microsoft365LicenseMode> = activeProfile.map {
        Microsoft365LicenseMode.fromRawValue(it.microsoft365LicenseModeRaw)
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.edit { preferences ->
                if (decodeProfiles(preferences[PROFILES_KEY]) == null) {
                    persistState(preferences, migratedState(preferences))
                } else {
                    persistState(preferences, stateFromPreferences(preferences))
                }
            }
        }
    }

    suspend fun setStatus(state: StepState, reason: String?, stepId: String) {
        setStatus(state, reason, note = null, stepId)
    }

    suspend fun setStatus(state: StepState, reason: String?, note: String?, stepId: String) {
        dataStore.edit { preferences ->
            val profileState = stateFromPreferences(preferences)
            val updatedProfiles = profileState.profiles.map { profile ->
                if (profile.id != profileState.activeProfile.id) return@map profile

                val previousState = profile.stepProgress[stepId]?.state ?: StepState.NOT_IMPLEMENTED
                val statuses = profile.stepProgress.toMutableMap()
                if (state == StepState.NOT_IMPLEMENTED) {
                    statuses.remove(stepId)
                } else {
                    statuses[stepId] = StepStatus(state, reason)
                }

                if (preferences[DEEP_AUDIT_ENABLED_KEY] != true || state == previousState) {
                    return@map profile.copy(stepProgress = statuses)
                }

                val trimmedNote = note?.trim()?.ifEmpty { null }
                val effectiveNote = if (state == StepState.NOT_APPLICABLE) {
                    trimmedNote ?: reason
                } else {
                    trimmedNote
                }
                val auditTrail = profile.auditTrail.toMutableMap()
                auditTrail[stepId] = (auditTrail[stepId].orEmpty() + AuditEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    previousState = previousState,
                    newState = state,
                    note = effectiveNote,
                )).takeLast(AUDIT_ENTRIES_PER_STEP_CAP)
                profile.copy(stepProgress = statuses, auditTrail = auditTrail)
            }
            persistState(preferences, profileState.copy(profiles = updatedProfiles))
        }
    }

    suspend fun auditEntries(stepId: String): List<AuditEntry> =
        activeProfile.first().auditTrail[stepId].orEmpty().asReversed()

    fun auditEntries(
        stepId: String,
        auditTrail: Map<String, List<AuditEntry>>,
    ): List<AuditEntry> = auditTrail[stepId].orEmpty().asReversed()

    suspend fun toggle(stepId: String) {
        val current = stepStatuses.first()[stepId]?.state ?: StepState.NOT_IMPLEMENTED
        val newState = if (current == StepState.IMPLEMENTED) {
            StepState.NOT_IMPLEMENTED
        } else {
            StepState.IMPLEMENTED
        }
        setStatus(newState, reason = null, stepId = stepId)
    }

    suspend fun setTargetMaturityLevel(level: MaturityLevel) {
        mutateActiveProfile { it.copy(targetMaturityLevelRaw = level.level) }
    }

    suspend fun setOSScope(scope: OSScope) {
        mutateActiveProfile { it.copy(osScopeFilterRaw = scope.rawValue) }
    }

    suspend fun setLicenseMode(mode: Microsoft365LicenseMode) {
        mutateActiveProfile { it.copy(microsoft365LicenseModeRaw = mode.rawValue) }
    }

    suspend fun switchProfile(id: String) {
        dataStore.edit { preferences ->
            val state = stateFromPreferences(preferences)
            if (state.profiles.any { it.id == id }) {
                persistState(preferences, state.copy(activeProfileId = id))
            }
        }
    }

    suspend fun createProfile(name: String): String {
        val trimmed = name.trim()
        val profile = Profile.newDefault(if (trimmed.isEmpty()) "New Profile" else trimmed)
        dataStore.edit { preferences ->
            val state = stateFromPreferences(preferences)
            persistState(preferences, state.copy(profiles = state.profiles + profile))
        }
        return profile.id
    }

    suspend fun renameProfile(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val state = stateFromPreferences(preferences)
            if (state.profiles.none { it.id == id }) return@edit
            persistState(
                preferences,
                state.copy(profiles = state.profiles.map { profile ->
                    if (profile.id == id) profile.copy(name = trimmed) else profile
                }),
            )
        }
    }

    suspend fun deleteProfile(id: String) {
        dataStore.edit { preferences ->
            val state = stateFromPreferences(preferences)
            if (state.profiles.size <= 1 || state.profiles.none { it.id == id }) return@edit
            val remaining = state.profiles.filterNot { it.id == id }
            val activeId = if (state.activeProfileId == id) remaining.first().id else state.activeProfileId
            persistState(preferences, ProfileState(remaining, activeId))
        }
    }

    suspend fun resetAll() {
        val profile = Profile.newDefault()
        dataStore.edit { preferences ->
            preferences.clear()
            persistState(preferences, ProfileState(listOf(profile), profile.id))
        }
    }

    fun isCompleted(stepId: String, statuses: Map<String, StepStatus>): Boolean =
        statuses[stepId]?.state == StepState.IMPLEMENTED

    fun isNotApplicable(stepId: String, statuses: Map<String, StepStatus>): Boolean =
        statuses[stepId]?.state == StepState.NOT_APPLICABLE

    fun completedCount(steps: List<ImplementationStep>, statuses: Map<String, StepStatus>): Int =
        steps.count { isCompleted(it.id, statuses) }

    fun notApplicableCount(steps: List<ImplementationStep>, statuses: Map<String, StepStatus>): Int =
        steps.count { isNotApplicable(it.id, statuses) }

    fun compliancePercentage(steps: List<ImplementationStep>, statuses: Map<String, StepStatus>): Double {
        if (steps.isEmpty()) return 100.0
        val denominator = steps.size - notApplicableCount(steps, statuses)
        if (denominator <= 0) return 100.0
        return completedCount(steps, statuses).toDouble() / denominator.toDouble() * 100.0
    }

    fun isControlComplete(
        control: EssentialControl,
        upTo: MaturityLevel,
        scope: OSScope,
        statuses: Map<String, StepStatus>,
    ): Boolean {
        val steps = control.steps(upTo, scope)
        if (steps.isEmpty()) return false
        return steps.all { step ->
            val state = statuses[step.id]?.state ?: StepState.NOT_IMPLEMENTED
            state == StepState.IMPLEMENTED || state == StepState.NOT_APPLICABLE
        }
    }

    private suspend fun mutateActiveProfile(mutation: (Profile) -> Profile) {
        dataStore.edit { preferences ->
            val state = stateFromPreferences(preferences)
            val updated = state.profiles.map { profile ->
                if (profile.id == state.activeProfile.id) mutation(profile) else profile
            }
            persistState(preferences, state.copy(profiles = updated))
        }
    }

    private fun stateFromPreferences(preferences: Preferences): ProfileState {
        val decoded = decodeProfiles(preferences[PROFILES_KEY]) ?: return migratedState(preferences)
        val storedId = preferences[ACTIVE_PROFILE_ID_KEY]
        val activeId = storedId?.takeIf { id -> decoded.any { it.id == id } } ?: decoded.first().id
        return ProfileState(decoded, activeId)
    }

    private fun migratedState(preferences: Preferences): ProfileState {
        val statuses = if (preferences[STEP_STATUSES_KEY] != null) {
            deserializeStatuses(preferences[STEP_STATUSES_KEY].orEmpty())
        } else {
            preferences[COMPLETED_STEP_IDS_KEY].orEmpty()
                .associateWith { StepStatus(StepState.IMPLEMENTED) }
        }
        val targetRaw = preferences[LEGACY_TARGET_MATURITY_LEVEL_KEY]
            ?.takeIf { raw -> MaturityLevel.entries.any { it.level == raw } }
            ?: MaturityLevel.ML3.level
        val scopeRaw = preferences[LEGACY_OS_SCOPE_KEY]
            ?.takeIf { raw -> OSScope.entries.any { it.rawValue == raw } }
            ?: OSScope.BOTH.rawValue
        val licenceRaw = preferences[LEGACY_LICENSE_MODE_KEY]
            ?.takeIf { raw -> Microsoft365LicenseMode.entries.any { it.rawValue == raw } }
            ?: Microsoft365LicenseMode.NONE.rawValue
        val profile = migrationDefault.copy(
            stepProgress = statuses,
            targetMaturityLevelRaw = targetRaw,
            osScopeFilterRaw = scopeRaw,
            microsoft365LicenseModeRaw = licenceRaw,
        )
        return ProfileState(listOf(profile), profile.id)
    }

    private fun persistState(preferences: androidx.datastore.preferences.core.MutablePreferences, state: ProfileState) {
        preferences[PROFILES_KEY] = serializeProfiles(state.profiles)
        preferences[ACTIVE_PROFILE_ID_KEY] = state.activeProfileId
    }

    private fun serializeProfiles(profiles: List<Profile>): String = JSONArray().apply {
        profiles.forEach { profile ->
            put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("createdAt", profile.createdAt)
                put("stepProgress", statusesToJson(profile.stepProgress))
                put("auditTrail", auditTrailToJson(profile.auditTrail))
                put("targetMaturityLevelRaw", profile.targetMaturityLevelRaw)
                put("osScopeFilterRaw", profile.osScopeFilterRaw)
                put("microsoft365LicenseModeRaw", profile.microsoft365LicenseModeRaw)
            })
        }
    }.toString()

    private fun decodeProfiles(json: String?): List<Profile>? {
        if (json.isNullOrBlank()) return null
        return try {
            val array = JSONArray(json)
            val decoded = buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(Profile(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        createdAt = item.getLong("createdAt"),
                        stepProgress = statusesFromJson(item.optJSONObject("stepProgress") ?: JSONObject()),
                        auditTrail = auditTrailFromJson(item.optJSONObject("auditTrail") ?: JSONObject()),
                        targetMaturityLevelRaw = item.optInt("targetMaturityLevelRaw", MaturityLevel.ML3.level),
                        osScopeFilterRaw = item.optString("osScopeFilterRaw", OSScope.BOTH.rawValue),
                        microsoft365LicenseModeRaw = item.optString(
                            "microsoft365LicenseModeRaw",
                            Microsoft365LicenseMode.NONE.rawValue,
                        ),
                    ))
                }
            }
            decoded.takeIf { profiles ->
                profiles.isNotEmpty() && profiles.map { it.id }.toSet().size == profiles.size
            }
        } catch (exception: Exception) {
            Log.e("ProgressStore", "Failed to deserialize profiles JSON", exception)
            null
        }
    }

    private fun statusesToJson(statuses: Map<String, StepStatus>): JSONObject = JSONObject().apply {
        statuses.forEach { (id, status) ->
            put(id, JSONObject().apply {
                put("state", status.state.rawValue)
                put("reason", status.reason ?: JSONObject.NULL)
            })
        }
    }

    private fun statusesFromJson(json: JSONObject): Map<String, StepStatus> = buildMap {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val status = json.optJSONObject(key) ?: continue
            put(
                key,
                StepStatus(
                    state = StepState.fromRawValue(status.optString("state")),
                    reason = if (status.isNull("reason")) null else status.optString("reason"),
                ),
            )
        }
    }

    private fun deserializeStatuses(json: String): Map<String, StepStatus> = try {
        if (json.isBlank()) emptyMap() else statusesFromJson(JSONObject(json))
    } catch (exception: Exception) {
        Log.e("ProgressStore", "Failed to deserialize step statuses JSON", exception)
        emptyMap()
    }

    private fun auditTrailToJson(auditTrail: Map<String, List<AuditEntry>>): JSONObject = JSONObject().apply {
        auditTrail.forEach { (stepId, entries) ->
            put(stepId, JSONArray().apply {
                entries.forEach { entry ->
                    put(JSONObject().apply {
                        put("id", entry.id)
                        put("timestamp", entry.timestamp)
                        put("previousState", entry.previousState.rawValue)
                        put("newState", entry.newState.rawValue)
                        put("note", entry.note ?: JSONObject.NULL)
                    })
                }
            })
        }
    }

    private fun auditTrailFromJson(json: JSONObject): Map<String, List<AuditEntry>> = buildMap {
        val keys = json.keys()
        while (keys.hasNext()) {
            val stepId = keys.next()
            val entries = json.optJSONArray(stepId) ?: continue
            put(stepId, buildList {
                for (index in 0 until entries.length()) {
                    val entry = entries.getJSONObject(index)
                    add(AuditEntry(
                        id = entry.getString("id"),
                        timestamp = entry.getLong("timestamp"),
                        previousState = StepState.fromRawValue(entry.optString("previousState")),
                        newState = StepState.fromRawValue(entry.optString("newState")),
                        note = if (entry.isNull("note")) null else entry.optString("note"),
                    ))
                }
            })
        }
    }

    companion object {
        private val COMPLETED_STEP_IDS_KEY = stringSetPreferencesKey("e8kb.stepProgress")
        private val STEP_STATUSES_KEY = stringPreferencesKey("e8kb.stepProgressDict")
        private val PROFILES_KEY = stringPreferencesKey("e8kb.profiles")
        private val ACTIVE_PROFILE_ID_KEY = stringPreferencesKey("e8kb.activeProfileID")
        private val LEGACY_TARGET_MATURITY_LEVEL_KEY = intPreferencesKey("targetMaturityLevel")
        private val LEGACY_OS_SCOPE_KEY = stringPreferencesKey("osScopeFilter")
        private val LEGACY_LICENSE_MODE_KEY = stringPreferencesKey("microsoft365LicenseMode")
        private val DEEP_AUDIT_ENABLED_KEY = booleanPreferencesKey("deepAuditEnabled")
    }
}

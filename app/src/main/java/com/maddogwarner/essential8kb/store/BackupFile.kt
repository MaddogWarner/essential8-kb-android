package com.maddogwarner.essential8kb.store

import com.maddogwarner.essential8kb.data.MaturityLevel
import com.maddogwarner.essential8kb.data.Microsoft365LicenseMode
import com.maddogwarner.essential8kb.data.OSScope
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

sealed class BackupException(message: String) : Exception(message) {
    data object FileTooLarge : BackupException(
        "The selected backup is larger than the 5 MB safety limit. Export profiles individually if an all-profiles backup is too large.",
    )

    class UnsupportedSchema(version: Int) : BackupException(
        "This backup uses schema version $version. Update the app before importing it.",
    )

    data object InvalidFile : BackupException(
        "The selected file is not a valid Essential 8 backup.",
    )
}

data class GlobalSettingsBackup(
    val showSplashOnStartup: Boolean?,
    val referenceOnlyMode: Boolean?,
    val deepAuditEnabled: Boolean?,
    val multiProfileEnabled: Boolean?,
)

data class BackupFile(
    val schemaVersion: Int,
    val appVersion: String,
    val exportedAt: Long,
    val profiles: List<Profile>,
    val globalSettings: GlobalSettingsBackup?,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
        const val MINIMUM_SUPPORTED_SCHEMA = 1
        const val MAXIMUM_FILE_SIZE = 5_242_880

        fun encode(backup: BackupFile): ByteArray {
            val capped = backup.copy(
                profiles = backup.profiles.map { profile ->
                    profile.copy(
                        auditTrail = profile.auditTrail.mapValues { (_, entries) ->
                            entries.takeLast(AUDIT_ENTRIES_PER_STEP_CAP)
                        },
                    )
                },
            )
            val bytes = capped.toJson().toString(2).toByteArray(Charsets.UTF_8)
            if (bytes.size > MAXIMUM_FILE_SIZE) throw BackupException.FileTooLarge
            return bytes
        }

        fun decode(data: ByteArray): BackupFile {
            if (data.size > MAXIMUM_FILE_SIZE) throw BackupException.FileTooLarge
            try {
                val json = JSONObject(data.toString(Charsets.UTF_8))
                val schemaVersion = json.requiredInt("schemaVersion")
                if (schemaVersion < MINIMUM_SUPPORTED_SCHEMA) throw BackupException.InvalidFile
                if (schemaVersion > CURRENT_SCHEMA_VERSION) {
                    throw BackupException.UnsupportedSchema(schemaVersion)
                }
                val backup = if (schemaVersion == 1) decodeLegacy(json) else decodeCurrent(json)
                if (backup.profiles.isEmpty() || backup.profiles.map { it.id }.toSet().size != backup.profiles.size) {
                    throw BackupException.InvalidFile
                }
                return backup
            } catch (error: BackupException) {
                throw error
            } catch (_: Exception) {
                throw BackupException.InvalidFile
            }
        }

        fun read(input: InputStream): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAXIMUM_FILE_SIZE) throw BackupException.FileTooLarge
                output.write(buffer, 0, count)
            }
            return output.toByteArray()
        }

        private fun decodeCurrent(json: JSONObject): BackupFile = BackupFile(
            schemaVersion = json.requiredInt("schemaVersion"),
            appVersion = json.requiredString("appVersion"),
            exportedAt = decodeDate(json.requiredString("exportedAt")),
            profiles = json.requiredArray("profiles").objects().map(::profileFromJson),
            globalSettings = json.optionalObject("globalSettings")?.let(::globalSettingsFromJson),
        )

        private fun decodeLegacy(json: JSONObject): BackupFile {
            val settings = json.requiredObject("settings")
            val licence = settings.optionalString("microsoft365LicenseMode")
                ?.takeIf { raw -> Microsoft365LicenseMode.entries.any { it.rawValue == raw } }
                ?: Microsoft365LicenseMode.NONE.rawValue
            val maturity = settings.optionalInt("targetMaturityLevel")
                ?.takeIf { raw -> MaturityLevel.entries.any { it.level == raw } }
                ?: MaturityLevel.ML3.level
            val scope = settings.optionalString("osScopeFilter")
                ?.takeIf { raw -> OSScope.entries.any { it.rawValue == raw } }
                ?: OSScope.BOTH.rawValue
            val profile = Profile.newDefault("Imported").copy(
                stepProgress = statusesFromJson(json.requiredObject("stepProgress")),
                targetMaturityLevelRaw = maturity,
                osScopeFilterRaw = scope,
                microsoft365LicenseModeRaw = licence,
            )
            return BackupFile(
                schemaVersion = CURRENT_SCHEMA_VERSION,
                appVersion = json.requiredString("appVersion"),
                exportedAt = decodeDate(json.requiredString("exportedAt")),
                profiles = listOf(profile),
                globalSettings = null,
            )
        }
    }
}

private fun BackupFile.toJson(): JSONObject = JSONObject().apply {
    put("schemaVersion", schemaVersion)
    put("appVersion", appVersion)
    put("exportedAt", encodeDate(exportedAt))
    put("profiles", JSONArray().apply { profiles.forEach { put(it.toBackupJson()) } })
    globalSettings?.let { put("globalSettings", it.toJson()) }
}

private fun Profile.toBackupJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("createdAt", encodeDate(createdAt))
    put("stepProgress", JSONObject().apply {
        stepProgress.forEach { (stepId, status) -> put(stepId, status.toJson()) }
    })
    put("auditTrail", JSONObject().apply {
        auditTrail.forEach { (stepId, entries) ->
            put(stepId, JSONArray().apply { entries.forEach { put(it.toJson()) } })
        }
    })
    put("targetMaturityLevelRaw", targetMaturityLevelRaw)
    put("osScopeFilterRaw", osScopeFilterRaw)
    put("microsoft365LicenseModeRaw", microsoft365LicenseModeRaw)
}

private fun StepStatus.toJson(): JSONObject = JSONObject().apply {
    put("state", state.rawValue)
    reason?.let { put("reason", it) }
}

private fun AuditEntry.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("timestamp", encodeDate(timestamp))
    put("previousState", previousState.rawValue)
    put("newState", newState.rawValue)
    note?.let { put("note", it) }
}

private fun GlobalSettingsBackup.toJson(): JSONObject = JSONObject().apply {
    showSplashOnStartup?.let { put("showSplashOnStartup", it) }
    referenceOnlyMode?.let { put("referenceOnlyMode", it) }
    deepAuditEnabled?.let { put("deepAuditEnabled", it) }
    multiProfileEnabled?.let { put("multiProfileEnabled", it) }
}

private fun profileFromJson(json: JSONObject): Profile {
    val id = json.requiredString("id").also { UUID.fromString(it) }
    return Profile(
        id = id,
        name = json.requiredString("name"),
        createdAt = decodeDate(json.requiredString("createdAt")),
        stepProgress = statusesFromJson(json.requiredObject("stepProgress")),
        auditTrail = auditTrailFromJson(json.requiredObject("auditTrail")),
        targetMaturityLevelRaw = json.requiredInt("targetMaturityLevelRaw"),
        osScopeFilterRaw = json.requiredString("osScopeFilterRaw"),
        microsoft365LicenseModeRaw = json.requiredString("microsoft365LicenseModeRaw"),
    )
}

private fun statusesFromJson(json: JSONObject): Map<String, StepStatus> = buildMap {
    json.keys().forEach { stepId ->
        val status = json.requiredObject(stepId)
        put(
            stepId,
            StepStatus(
                state = strictStepState(status.requiredString("state")),
                reason = status.optionalString("reason"),
            ),
        )
    }
}

private fun auditTrailFromJson(json: JSONObject): Map<String, List<AuditEntry>> = buildMap {
    json.keys().forEach { stepId ->
        put(stepId, json.requiredArray(stepId).objects().map { entry ->
            val id = entry.requiredString("id").also { UUID.fromString(it) }
            AuditEntry(
                id = id,
                timestamp = decodeDate(entry.requiredString("timestamp")),
                previousState = strictStepState(entry.requiredString("previousState")),
                newState = strictStepState(entry.requiredString("newState")),
                note = entry.optionalString("note"),
            )
        })
    }
}

private fun globalSettingsFromJson(json: JSONObject): GlobalSettingsBackup = GlobalSettingsBackup(
    showSplashOnStartup = json.optionalBoolean("showSplashOnStartup"),
    referenceOnlyMode = json.optionalBoolean("referenceOnlyMode"),
    deepAuditEnabled = json.optionalBoolean("deepAuditEnabled"),
    multiProfileEnabled = json.optionalBoolean("multiProfileEnabled"),
)

private fun strictStepState(rawValue: String): StepState =
    StepState.entries.firstOrNull { it.rawValue == rawValue } ?: throw BackupException.InvalidFile

private fun encodeDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).truncatedTo(ChronoUnit.SECONDS).toString()

private fun decodeDate(value: String): Long = Instant.parse(value).toEpochMilli()

private fun JSONObject.requiredString(name: String): String = get(name).let { value ->
    if (value is String) value else throw BackupException.InvalidFile
}

private fun JSONObject.requiredInt(name: String): Int = get(name).let { value ->
    if (value is Int) value else throw BackupException.InvalidFile
}

private fun JSONObject.requiredObject(name: String): JSONObject = get(name).let { value ->
    if (value is JSONObject) value else throw BackupException.InvalidFile
}

private fun JSONObject.requiredArray(name: String): JSONArray = get(name).let { value ->
    if (value is JSONArray) value else throw BackupException.InvalidFile
}

private fun JSONObject.optionalString(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return requiredString(name)
}

private fun JSONObject.optionalInt(name: String): Int? {
    if (!has(name) || isNull(name)) return null
    return requiredInt(name)
}

private fun JSONObject.optionalBoolean(name: String): Boolean? {
    if (!has(name) || isNull(name)) return null
    return get(name).let { value -> if (value is Boolean) value else throw BackupException.InvalidFile }
}

private fun JSONObject.optionalObject(name: String): JSONObject? {
    if (!has(name) || isNull(name)) return null
    return requiredObject(name)
}

private fun JSONArray.objects(): List<JSONObject> = List(length()) { index ->
    get(index).let { value -> if (value is JSONObject) value else throw BackupException.InvalidFile }
}

package com.maddogwarner.essential8kb.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maddogwarner.essential8kb.data.EssentialControl
import com.maddogwarner.essential8kb.data.allStepIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.essential8DataStore: DataStore<Preferences> by preferencesDataStore(
    name = "essential8_preferences",
)

class ProgressStore(
    private val dataStore: DataStore<Preferences>,
) {
    val completedStepIds: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[COMPLETED_STEP_IDS].orEmpty()
        }

    suspend fun toggle(stepId: String) {
        dataStore.edit { preferences ->
            val current = preferences[COMPLETED_STEP_IDS].orEmpty()
            preferences[COMPLETED_STEP_IDS] = if (stepId in current) {
                current - stepId
            } else {
                current + stepId
            }
        }
    }

    suspend fun setCompleted(stepId: String, completed: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[COMPLETED_STEP_IDS].orEmpty()
            preferences[COMPLETED_STEP_IDS] = if (completed) {
                current + stepId
            } else {
                current - stepId
            }
        }
    }

    fun isCompleted(stepId: String, completedStepIds: Set<String>): Boolean =
        stepId in completedStepIds

    fun completedCount(stepIds: Iterable<String>, completedStepIds: Set<String>): Int =
        stepIds.count { it in completedStepIds }

    fun completedCount(control: EssentialControl, completedStepIds: Set<String>): Int =
        completedCount(control.allStepIds(), completedStepIds)

    fun isControlComplete(control: EssentialControl, completedStepIds: Set<String>): Boolean {
        val stepIds = control.allStepIds()
        return stepIds.isNotEmpty() && stepIds.all { it in completedStepIds }
    }

    companion object {
        private val COMPLETED_STEP_IDS = stringSetPreferencesKey("e8kb.stepProgress")
    }
}

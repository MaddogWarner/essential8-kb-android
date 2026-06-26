package com.maddogwarner.essential8kb

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.maddogwarner.essential8kb.store.ProgressStore
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProgressStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun toggleRoundTripsCompletedStepId() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, "progress.preferences_pb") },
        )
        val store = ProgressStore(dataStore)

        store.toggle("1-1-0")
        assertEquals(setOf("1-1-0"), store.completedStepIds.first())

        store.toggle("1-1-0")
        assertEquals(emptySet<String>(), store.completedStepIds.first())

        scope.cancel()
    }
}

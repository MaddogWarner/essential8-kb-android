package com.maddogwarner.essential8kb

import com.maddogwarner.essential8kb.ui.about.sanitiseFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class AboutScreenTest {
    @Test
    fun exportFileNameFallsBackForNamesWithoutAllowedCharacters() {
        assertEquals("Profile", sanitiseFileName("🔐 !!!"))
        assertEquals("Hospital-A", sanitiseFileName("Hospital A"))
    }
}

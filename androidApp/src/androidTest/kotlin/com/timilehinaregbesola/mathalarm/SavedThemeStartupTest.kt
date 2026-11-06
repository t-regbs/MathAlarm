package com.timilehinaregbesola.mathalarm

import androidx.appcompat.app.AppCompatDelegate
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

class SavedThemeStartupTest {
    @Test
    fun savedThemeIsAppliedWhenTheApplicationStarts() {
        val expected = InstrumentationRegistry.getArguments().getString("expectedNightMode")
        assumeTrue("Run scripts/verify_android_saved_theme.py to seed the startup preference", expected != null)

        assertEquals(expected!!.toInt(), AppCompatDelegate.getDefaultNightMode())
    }
}

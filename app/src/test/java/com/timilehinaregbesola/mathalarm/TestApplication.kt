package com.timilehinaregbesola.mathalarm

import android.app.Application

/**
 * Test Application class that does NOT initialize Koin.
 * This is used by Robolectric tests to avoid Koin initialization conflicts.
 */
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Do NOT start Koin - tests will manage their own state
    }
}

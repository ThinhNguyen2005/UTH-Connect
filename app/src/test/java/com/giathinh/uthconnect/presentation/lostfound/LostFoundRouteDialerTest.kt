package com.giathinh.uthconnect.presentation.lostfound

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LostFoundRouteDialerTest {

    @Test
    fun `launchDialer catches ActivityNotFoundException and invokes onError`() {
        var errorInvoked = false
        val failingContext = object : android.content.ContextWrapper(null) {
            override fun startActivity(intent: Intent?): Unit = throw ActivityNotFoundException("No dialer available")
        }

        launchDialer(failingContext, "0901234567", intentFactory = { Intent(Intent.ACTION_DIAL) }) {
            errorInvoked = true
        }

        assertTrue("onError must be called when ActivityNotFoundException occurs", errorInvoked)
    }

    @Test
    fun `launchDialer catches SecurityException and invokes onError`() {
        var errorInvoked = false
        val failingContext = object : android.content.ContextWrapper(null) {
            override fun startActivity(intent: Intent?): Unit = throw SecurityException("Dialer permission denied")
        }

        launchDialer(failingContext, "0901234567", intentFactory = { Intent(Intent.ACTION_DIAL) }) {
            errorInvoked = true
        }

        assertTrue("onError must be called when SecurityException occurs", errorInvoked)
    }

    @Test
    fun `launchDialer succeeds without invoking onError when dialer is available`() {
        var errorInvoked = false
        var startActivityCalled = false
        val successfulContext = object : android.content.ContextWrapper(null) {
            override fun startActivity(intent: Intent?) {
                startActivityCalled = true
            }
        }

        launchDialer(successfulContext, "0901234567", intentFactory = { Intent(Intent.ACTION_DIAL) }) {
            errorInvoked = true
        }

        assertFalse("onError must not be called when startActivity succeeds", errorInvoked)
        assertTrue("startActivity must be called on successful execution", startActivityCalled)
    }
}

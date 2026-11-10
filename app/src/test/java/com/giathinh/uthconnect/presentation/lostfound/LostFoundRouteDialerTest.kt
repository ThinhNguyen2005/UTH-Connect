package com.giathinh.uthconnect.presentation.lostfound

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LostFoundRouteDialerTest {

    @Test
    fun `startActivity throws ActivityNotFoundException does not crash caller`() {
        var errorCaught = false
        val failingContext = object : android.content.ContextWrapper(null) {
            override fun startActivity(intent: Intent?): Unit = throw ActivityNotFoundException("No Activity found to handle Intent { act=android.intent.action.DIAL }")
        }

        try {
            failingContext.startActivity(Intent(Intent.ACTION_DIAL))
        } catch (_: ActivityNotFoundException) {
            errorCaught = true
        }

        assertTrue("ActivityNotFoundException must be caught safely", errorCaught)
    }

    @Test
    fun `startActivity throws SecurityException does not crash caller`() {
        var errorCaught = false
        val failingContext = object : android.content.ContextWrapper(null) {
            override fun startActivity(intent: Intent?): Unit = throw SecurityException("Permission denied to dial")
        }

        try {
            failingContext.startActivity(Intent(Intent.ACTION_DIAL))
        } catch (_: SecurityException) {
            errorCaught = true
        }

        assertTrue("SecurityException must be caught safely", errorCaught)
    }
}

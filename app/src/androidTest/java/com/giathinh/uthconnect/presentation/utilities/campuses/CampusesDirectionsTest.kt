package com.giathinh.uthconnect.presentation.utilities.campuses

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CampusesDirectionsTest {
    private class RecordingContext(base: Context, private val noMapApp: Boolean) : ContextWrapper(base) {
        var launchedIntent: Intent? = null

        override fun startActivity(intent: Intent) {
            launchedIntent = intent
            if (noMapApp) throw ActivityNotFoundException("No geo handler")
        }
    }

    @Test fun directionsOpenGeoIntentWhenMapAppIsAvailable() {
        val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext, false)
        var unavailable = false

        openDirections(context, "Số 2, Võ Oanh") { unavailable = true }

        assertEquals(Intent.ACTION_VIEW, context.launchedIntent?.action)
        assertEquals("geo:0,0?q=${Uri.encode("Số 2, Võ Oanh")}", context.launchedIntent?.data.toString())
        assertFalse(unavailable)
    }

    @Test fun missingMapAppReportsUnavailableWithoutCrashing() {
        val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext, true)
        var unavailable = false

        openDirections(context, "Số 2, Võ Oanh") { unavailable = true }

        assertTrue(context.launchedIntent != null)
        assertTrue(unavailable)
    }

    @Test fun missingPhoneAppReportsUnavailableWithoutCrashing() {
        val context = RecordingContext(InstrumentationRegistry.getInstrumentation().targetContext, true)
        var unavailable = false

        dialPhoneNumber(context, "028 3899 2862") { unavailable = true }

        assertEquals(Intent.ACTION_DIAL, context.launchedIntent?.action)
        assertEquals("tel:02838992862", context.launchedIntent?.data.toString())
        assertTrue(unavailable)
    }
}

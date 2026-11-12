package com.giathinh.uthconnect.presentation.utilities.campuses

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CampusesScreenUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var recordingContext: RecordingContext

    @Before
    fun setUp() {
        recordingContext = RecordingContext(composeRule.activity)
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides recordingContext) {
                UTHConnectTheme {
                    CampusesScreen(onBack = {})
                }
            }
        }
    }

    @Test
    fun directionsButtonOpensGeoIntentForCampusAddress() {
        composeRule.onAllNodesWithTag("campus_directions_button").onFirst().performScrollTo().performClick()

        val intent = recordingContext.lastStartedIntent
        assertNotNull(intent)
        assertEquals(Intent.ACTION_VIEW, intent?.action)
        assertEquals("geo", intent?.data?.scheme)
        assertTrue(Uri.decode(intent?.data?.toString().orEmpty()).contains("Võ Oanh"))
    }

    @Test
    fun campusHotlineButtonOpensDialIntent() {
        composeRule.onNodeWithText("028 3899 2862", useUnmergedTree = true).performClick()

        val intent = recordingContext.lastStartedIntent
        assertNotNull(intent)
        assertEquals(Intent.ACTION_DIAL, intent?.action)
        assertEquals("tel", intent?.data?.scheme)
        assertTrue(intent?.data.toString().endsWith("02838992862"))
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        var lastStartedIntent: Intent? = null
            private set

        override fun startActivity(intent: Intent) {
            lastStartedIntent = intent
        }
    }
}

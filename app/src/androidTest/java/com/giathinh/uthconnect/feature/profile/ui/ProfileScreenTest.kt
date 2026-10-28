package com.giathinh.uthconnect.feature.profile.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.giathinh.uthconnect.ui.theme.UthConnectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun profileDisplaysStudentAndProvidesLogout() {
        var loggedOut = false
        composeRule.setContent {
            UthConnectTheme {
                ProfileScreen(student = PreviewStudent, onBack = {}, onSignOut = { loggedOut = true })
            }
        }
        composeRule.onNodeWithTag("profile_full_name").assertExists()
        composeRule.onNodeWithTag("logout_button").performClick()
        assertTrue(loggedOut)
    }
}

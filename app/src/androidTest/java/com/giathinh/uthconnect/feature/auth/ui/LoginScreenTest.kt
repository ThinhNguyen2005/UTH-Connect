package com.giathinh.uthconnect.feature.auth.ui

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.giathinh.uthconnect.feature.auth.AuthUiState
import com.giathinh.uthconnect.ui.theme.UthConnectTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun loginUiAcceptsInputAndCallsSubmit() {
        var id = ""
        var password = ""
        var clicks = 0
        composeRule.setContent {
            UthConnectTheme {
                LoginScreen(
                    state = AuthUiState(isRestoring = false),
                    onIdentifierChange = { id = it },
                    onPasswordChange = { password = it },
                    onRememberMeChange = {},
                    onPasswordVisibilityClick = {},
                    onForgotPasswordClick = {},
                    onForgotPasswordDismiss = {},
                    onSignInClick = { clicks++ },
                )
            }
        }
        composeRule.onNodeWithTag("login_identifier").performTextInput("2023040123")
        composeRule.onNodeWithTag("login_password").performTextInput("UTH@123456")
        composeRule.onNodeWithTag("login_submit").performClick()
        assertEquals("2023040123", id)
        assertEquals("UTH@123456", password)
        assertEquals(1, clicks)
    }

    @Test fun loginScreenHasRememberAndSubmitControls() {
        composeRule.setContent {
            UthConnectTheme {
                LoginScreen(
                    state = AuthUiState(isRestoring = false),
                    onIdentifierChange = {}, onPasswordChange = {},
                    onRememberMeChange = {}, onPasswordVisibilityClick = {},
                    onForgotPasswordClick = {}, onForgotPasswordDismiss = {},
                    onSignInClick = {},
                )
            }
        }
        composeRule.onNodeWithTag("remember_me").assertExists()
        composeRule.onNodeWithTag("login_submit").assertExists()
    }
}

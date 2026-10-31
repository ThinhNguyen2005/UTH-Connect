package com.giathinh.uthconnect.presentation.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthScreenUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun retryButtonAppearsOnInvalidCredentialsAndEmitsAction() {
        val actions = mutableListOf<AuthAction>()
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(
                    uiState = AuthUiState(isRestoring = false, failure = AuthFailure.InvalidCredentials),
                    onAction = { actions.add(it) },
                )
            }
        }
        rule.onNodeWithTag("auth_retry").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(listOf(AuthAction.Retry), actions) }
    }

    @Test fun retryButtonNotShownWithoutFailure() {
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(uiState = AuthUiState(isRestoring = false), onAction = {})
            }
        }
        rule.onNodeWithTag("auth_retry").assertDoesNotExist()
    }
}

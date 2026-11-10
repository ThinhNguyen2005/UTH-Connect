package com.giathinh.uthconnect.presentation.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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

    @Test fun biometricElementsDoNotExistOnAuthScreen() {
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(uiState = AuthUiState(isRestoring = false), onAction = {})
            }
        }
        rule.onNodeWithTag("auth_mode_biometric").assertDoesNotExist()
        rule.onNodeWithTag("auth_biometric_card").assertDoesNotExist()
        rule.onNodeWithTag("auth_biometric_touch_zone").assertDoesNotExist()
        rule.onNodeWithTag("auth_biometric_login").assertDoesNotExist()
        rule.onNodeWithTag("auth_mode_password").assertDoesNotExist()
    }

    @Test fun forgotPasswordDialog_opens_andEmitsForgotActions() {
        val actions = mutableListOf<AuthAction>()
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        isRestoring = false,
                        screenStep = AuthScreenStep.Password,
                        forgotDialogOpen = true,
                        forgotEmail = "2023040123@ut.edu.vn",
                    ),
                    onAction = { actions.add(it) },
                )
            }
        }
        rule.onNodeWithTag("auth_forgot_email").assertIsDisplayed()
        rule.onNodeWithTag("auth_forgot_send").assertIsEnabled().performClick()
        rule.runOnIdle {
            assertEquals(listOf(AuthAction.SubmitForgotPassword), actions)
        }
    }

    @Test fun forgotPasswordDialog_successBanner_andDismiss() {
        val actions = mutableListOf<AuthAction>()
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        isRestoring = false,
                        forgotDialogOpen = true,
                        forgotSuccessNotice = true,
                    ),
                    onAction = { actions.add(it) },
                )
            }
        }
        rule.onNodeWithTag("auth_forgot_success_banner").assertIsDisplayed()
        rule.onNodeWithTag("auth_forgot_done").performClick()
        rule.runOnIdle {
            assertEquals(listOf(AuthAction.DismissForgotPassword), actions)
        }
    }

    @Test fun emailVerificationShortcut_triggersStepSelection() {
        val actions = mutableListOf<AuthAction>()
        rule.setContent {
            UTHConnectTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        isRestoring = false,
                        screenStep = AuthScreenStep.Password,
                    ),
                    onAction = { actions.add(it) },
                )
            }
        }
        rule.onNodeWithTag("auth_goto_verify_email").performScrollTo().performClick()
        rule.runOnIdle {
            assertEquals(listOf(AuthAction.SelectStep(AuthScreenStep.VerifyEmail)), actions)
        }
    }
}

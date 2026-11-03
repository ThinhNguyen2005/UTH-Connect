package com.giathinh.uthconnect.presentation.auth

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.usecase.*
import com.giathinh.uthconnect.presentation.auth.verifyemail.*
import com.giathinh.uthconnect.presentation.auth.forgotpassword.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fake chỉ ở androidTest: kiểm thử gate/UI, không xác nhận OAuth hay email thật. */
@RunWith(AndroidJUnit4::class)
class Auth3FlowUiTest {
    @get:Rule val rule = createComposeRule()
    private fun launch(backend: Auth3FakeRepository) {
        val sessions = Auth3FakeSession(backend)
        lateinit var auth: AuthViewModel
        lateinit var verify: VerifyEmailViewModel
        lateinit var forgot: ForgotPasswordViewModel
        rule.runOnUiThread {
            auth = AuthViewModel(SignInUseCase(backend, sessions), AccessStudentSessionUseCase(sessions),
                ResetPasswordUseCase(backend), SignUpUseCase(backend), SignInWithGoogleUseCase(backend, sessions), ManageAuthAccountUseCase(backend))
            verify = VerifyEmailViewModel(SendVerificationUseCase(backend), CheckVerificationUseCase(backend, sessions),
                SignOutUseCase(sessions), ManageAuthAccountUseCase(backend))
            forgot = ForgotPasswordViewModel(ResetPasswordUseCase(backend))
        }
        rule.setContent { UTHConnectTheme {
            AuthRoute(viewModel = auth, signedInContent = { Text("Main", Modifier.testTag("main")) },
                verifyContent = { remember ->
                    val state by verify.uiState.collectAsStateWithLifecycle()
                    VerifyEmailScreen(state, remember, verify::onAction)
                }, forgotContent = { back ->
                    val state by forgot.uiState.collectAsStateWithLifecycle()
                    ForgotPasswordScreen(state, forgot::onAction, back)
                })
        } }
    }
    private fun register() {
        rule.onNodeWithTag("auth_tab_signup").performClick()
        rule.onNodeWithTag("auth_identifier").performTextInput("student@ut.edu.vn")
        rule.onNodeWithTag("auth_password").performTextInput("Strong@12345")
        rule.onNodeWithTag("auth_sign_in").performScrollTo().performClick()
        rule.onNodeWithTag("verify_email").assertExists()
        rule.onNodeWithTag("main").assertDoesNotExist()
    }
    @Test fun registrationRequiresVerifiedClaimsBeforeMain() {
        val backend = Auth3FakeRepository(); launch(backend); register()
        backend.error = AuthError.EmailNotVerified
        rule.onNodeWithTag("verify_check").performClick()
        rule.onNodeWithTag("verify_error").assertExists()
        rule.onNodeWithTag("main").assertDoesNotExist()
        backend.error = null
        rule.onNodeWithTag("verify_check").performClick()
        rule.onNodeWithTag("main").assertExists()
    }
    @Test fun pendingLogoutReturnsToLoginAndCooldownDisablesResend() {
        launch(Auth3FakeRepository()); register()
        rule.onNodeWithTag("verify_resend").assertIsNotEnabled()
        rule.onNodeWithTag("verify_logout").performClick()
        rule.onNodeWithTag("auth_identifier").assertExists()
        rule.onNodeWithTag("main").assertDoesNotExist()
    }
    @Test fun forgotPasswordRejectsDomainAndUsesGenericSuccess() {
        launch(Auth3FakeRepository())
        rule.onNodeWithTag("auth_forgot_password").performScrollTo().performClick()
        rule.onNodeWithTag("forgot_email").performTextInput("student@gmail.com")
        rule.onNodeWithTag("forgot_send").performClick()
        rule.onNodeWithTag("forgot_error").assertExists()
        rule.onNodeWithTag("forgot_email").performTextReplacement("student@ut.edu.vn")
        rule.onNodeWithTag("forgot_error").assertDoesNotExist()
        rule.onNodeWithTag("forgot_send").performClick()
        rule.onNodeWithTag("forgot_sent").assertExists()
        rule.onNodeWithTag("main").assertDoesNotExist()
    }
    @Test fun googleConfigurationFailureDoesNotOpenMain() {
        launch(Auth3FakeRepository())
        rule.onNodeWithTag("auth_google").performScrollTo().performClick()
        rule.onNodeWithTag("main").assertDoesNotExist()
        rule.onNodeWithTag("auth_error").assertExists()
        rule.onNodeWithTag("auth_google").assertIsEnabled()
    }
    private fun localizedErrorScreen(language: String, verify: Boolean) {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val config = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag(language))
        }
        val localized = context.createConfigurationContext(config)
        var clicked = false
        rule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalContext provides localized,
                androidx.compose.ui.platform.LocalConfiguration provides config,
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    androidx.compose.ui.platform.LocalDensity.current.density, fontScale = 2f)) {
                UTHConnectTheme {
                    if (verify) VerifyEmailScreen(VerifyEmailUiState(email = "student@ut.edu.vn",
                        error = AuthError.PermissionDenied), false, { clicked = it is VerifyEmailAction.Check })
                    else ForgotPasswordScreen(ForgotPasswordUiState(error = AuthError.Network),
                        { clicked = it == ForgotPasswordAction.Send }, {})
                }
            }
        }
        rule.onNodeWithText(localized.getString(if (verify) com.giathinh.uthconnect.R.string.auth_permission
            else com.giathinh.uthconnect.R.string.auth_network)).assertExists()
        rule.onNodeWithTag(if (verify) "verify_check" else "forgot_send").performScrollTo().performClick()
        rule.runOnIdle { org.junit.Assert.assertTrue(clicked) }
    }
    @Test fun verifyRevocationVietnameseAtLargeFont() = localizedErrorScreen("vi", true)
    @Test fun verifyRevocationEnglishAtLargeFont() = localizedErrorScreen("en", true)
    @Test fun forgotNetworkVietnameseAtLargeFont() = localizedErrorScreen("vi", false)
    @Test fun forgotNetworkEnglishAtLargeFont() = localizedErrorScreen("en", false)

}

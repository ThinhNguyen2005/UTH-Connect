package com.giathinh.uthconnect.presentation.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.presentation.utilities.UtilitiesScreen
import com.giathinh.uthconnect.presentation.utilities.UtilitiesUiState
import com.giathinh.uthconnect.core.designsystem.theme.UTHConnectTheme
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.ChangePasswordUseCase
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.ManageStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateStudentContactUseCase
import com.giathinh.uthconnect.presentation.profile.ProfileRoute
import com.giathinh.uthconnect.presentation.profile.ProfileViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Kiểm thử Route/ViewModel tích hợp bằng backend test; không xác nhận kết nối Firebase thật. */
@RunWith(AndroidJUnit4::class)
class AuthProfileFlowUiTest {
    @get:Rule val rule = createComposeRule()

    private class Backend : AuthRepository, SessionRepository, StudentContactRepository {
        var profile = StudentProfile("2023040123", "student@ut.edu.vn", "Test Student", "CN23", "IT", "IT")
        override val session = MutableStateFlow(StudentSession())
        override suspend fun restore() { session.value = StudentSession(isRestoring = false) }
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            session.value = StudentSession(isRestoring = false, profile = profile, remembered = remember)
        }
        override suspend fun signOut() { session.value = StudentSession(isRestoring = false) }
        override suspend fun updateProfile(profile: StudentProfile) { session.value = session.value.copy(profile = profile) }
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun authenticate(identifier: String, password: String) =
            Result.success(profile.takeIf { identifier == it.email && password == "Correct@123" })
        override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(profile)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(true)
        override suspend fun updateContact(studentId: String, phone: String, personalEmail: String, currentAddress: String): Result<StudentProfile> {
            profile = profile.copy(phoneNumber = phone, personalEmail = personalEmail, currentAddress = currentAddress)
            return Result.success(profile)
        }
    }

    private fun launch(backend: Backend, withUtilities: Boolean = false) {
        lateinit var auth: AuthViewModel
        lateinit var profile: ProfileViewModel
        rule.runOnUiThread {
            auth = AuthViewModel(SignInUseCase(backend, backend), AccessStudentSessionUseCase(backend), ResetPasswordUseCase(backend))
            profile = ProfileViewModel(GetProfileUseCase(backend), ManageStudentSessionUseCase(backend),
                ChangePasswordUseCase(backend, backend), UpdateStudentContactUseCase(backend, backend))
        }
        rule.setContent {
            UTHConnectTheme {
                AuthRoute(viewModel = auth, signedInContent = {
                    if (withUtilities) {
                        val nav = rememberNavController()
                        NavHost(navController = nav, startDestination = "profile") {
                            composable("profile") {
                                ProfileRoute(viewModel = profile, onOpenUtilities = { nav.navigate("utilities") })
                            }
                            composable("utilities") {
                                UtilitiesScreen(UtilitiesUiState(isLoading = false), onAction = {},
                                    onBack = { nav.popBackStack() })
                            }
                        }
                    } else ProfileRoute(viewModel = profile)
                })
            }
        }
    }

    private fun signIn(password: String) {
        rule.onNodeWithTag("auth_identifier").performTextInput("student@ut.edu.vn")
        rule.onNodeWithTag("auth_password").performTextInput(password)
        rule.onNodeWithTag("auth_sign_in").performScrollTo().performClick()
    }

    @Test fun signInEditContactAndLogoutReturnsToAuthGate() {
        val backend = Backend()
        launch(backend)
        signIn("Correct@123")
        rule.onNodeWithTag("profile_name").assertExists()
        rule.onNodeWithTag("profile_edit_contact").performScrollTo().performClick()
        rule.onNodeWithTag("contact_phone").performScrollTo().performTextInput("0912345678")
        rule.onNodeWithTag("contact_save").performScrollTo().performClick()
        rule.runOnIdle { assertEquals("0912345678", backend.session.value.profile?.phoneNumber) }
        rule.onNodeWithTag("logout_button").performScrollTo().performClick()
        rule.onNodeWithTag("logout_retry_or_confirm").performClick()
        rule.onNodeWithTag("auth_identifier").assertExists()
        rule.onNodeWithTag("profile_name").assertDoesNotExist()
        rule.runOnIdle { assertNull(backend.session.value.profile) }
        androidx.test.espresso.Espresso.pressBackUnconditionally()
        assertNull(backend.session.value.profile)
    }

    @Test fun invalidPasswordKeepsProfileBehindAuthGate() {
        val backend = Backend()
        launch(backend)
        signIn("wrong")
        rule.onNodeWithTag("auth_error").assertExists()
        rule.onNodeWithTag("profile_name").assertDoesNotExist()
        rule.runOnIdle { assertNull(backend.session.value.profile) }
    }

    @Test fun profileOpensUtilitiesAndBackPreservesContactEditing() {
        val backend = Backend()
        launch(backend, withUtilities = true)
        signIn("Correct@123")
        rule.onNodeWithTag("profile_utilities").performScrollTo().performClick()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        rule.onNodeWithText(context.getString(R.string.utilities_title)).assertExists()
        rule.onNodeWithContentDescription(context.getString(R.string.common_back)).performClick()
        rule.onNodeWithTag("profile_name").assertExists()
        rule.onNodeWithTag("profile_edit_contact").performScrollTo().performClick()
        rule.onNodeWithTag("contact_phone").assertExists()
        rule.runOnIdle { assertEquals(backend.profile, backend.session.value.profile) }
    }
}

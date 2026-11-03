package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()

    private val demo = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")

    private class FakeSession : SessionRepository {
        var failRestore = false
        var restoreCalls = 0
        private val state = MutableStateFlow(StudentSession())
        override val session: StateFlow<StudentSession> = state
        override suspend fun restore() {
            restoreCalls++
            if (failRestore) { failRestore = false; state.value = StudentSession(isRestoring = false,
                failure = com.giathinh.uthconnect.domain.repository.SessionFailure.RestoreFailed) }
            else state.value = StudentSession(isRestoring = false)
        }
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            state.value = StudentSession(isRestoring = false, profile = profile, remembered = remember)
        }
        override suspend fun updateProfile(profile: StudentProfile) { state.value = state.value.copy(profile = profile) }
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun signOut() { state.value = StudentSession(isRestoring = false) }
    }

    private fun newViewModel(session: FakeSession, error: com.giathinh.uthconnect.domain.repository.AuthError? = null) : AuthViewModel {
        val auth = object : AuthRepository {
            override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> =
                if (error != null) Result.failure(com.giathinh.uthconnect.domain.repository.AuthException(error))
                else Result.success(demo.takeIf { identifier == it.studentId && password == "secret" })
            override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(demo)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }
        return AuthViewModel(SignInUseCase(auth, session), AccessStudentSessionUseCase(session), com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase(auth))
    }

    @Test fun `Tai man hinh dau loading sau do hien form rong`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isRestoring)
        assertNull(vm.uiState.value.student)
    }

    @Test fun `Bo trong form thi bao loi hai truong`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(com.giathinh.uthconnect.domain.validation.LoginFieldError.Required,
            vm.uiState.value.identifierError)
        assertEquals(com.giathinh.uthconnect.domain.validation.LoginFieldError.Required,
            vm.uiState.value.passwordError)
    }

    @Test fun `Dang nhap thanh cong se xoa mat khau trong state`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.TogglePassword)
        vm.onAction(AuthAction.RememberChanged(true))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.passwordVisible)
    }

    @Test fun `Sai mat khau hien thong bao va khong vao app`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrong"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)
    }
    @Test fun `wrong password emits single one off event`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        val event = async { vm.events.first() }
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrong"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthEvent.InvalidCredentials, event.await())
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)
    }


    @Test fun `Retry tries previous credentials and preserves error after failure`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrong"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)
        assertFalse(vm.uiState.value.isSigningIn)
    }

    @Test fun `Retry with corrected password signs in and clears secret`() = runTest {
        val vm = newViewModel(FakeSession())
        advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrong"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        assertEquals("", vm.uiState.value.password)
        assertNull(vm.uiState.value.failure)
    }
    @Test fun `network error is distinct from incorrect password`() = runTest {
        val vm = newViewModel(FakeSession(), com.giathinh.uthconnect.domain.repository.AuthError.Network)
        advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.Submit); advanceUntilIdle()
        assertEquals(AuthFailure.Network, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)
    }
    @Test fun `retry restore clears previous failure`() = runTest {
        val session = FakeSession().apply { failRestore = true }
        val vm = newViewModel(session); advanceUntilIdle()
        assertEquals(AuthFailure.SessionRestore, vm.uiState.value.failure)
        vm.onAction(AuthAction.RetryRestore); advanceUntilIdle()
        assertEquals(2, session.restoreCalls)
        assertNull(vm.uiState.value.failure)
    }
    @Test fun `biometric failure and unsolicited success never grant a session`() = runTest {
        val vm = newViewModel(FakeSession()); advanceUntilIdle()
        vm.onAction(AuthAction.BiometricFailed); advanceUntilIdle()
        assertEquals(AuthFailure.BiometricUnavailable, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)
        vm.onAction(AuthAction.BiometricVerified); advanceUntilIdle()
        assertNull(vm.uiState.value.student)
        assertFalse(vm.uiState.value.isSigningIn)
    }
    @Test fun `invalid reset email does not claim a reset was sent`() = runTest {
        val vm = newViewModel(FakeSession()); advanceUntilIdle()
        vm.onAction(AuthAction.ForgotPassword)
        vm.onAction(AuthAction.ResetEmailChanged("2023040123"))
        vm.onAction(AuthAction.SubmitReset); advanceUntilIdle()
        assertEquals(AuthFailure.InvalidResetEmail, vm.uiState.value.failure)
        assertFalse(vm.uiState.value.resetSent)
        assertFalse(vm.uiState.value.isResetting)
    }

}

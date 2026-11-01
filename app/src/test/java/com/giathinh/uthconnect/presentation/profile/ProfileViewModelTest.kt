package com.giathinh.uthconnect.presentation.profile

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.ChangePasswordUseCase
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.domain.usecase.ManageStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateStudentContactUseCase
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
class ProfileViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private val demo = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")
    private class FakeSession(initial: StudentProfile) : SessionRepository {
        var failNextLogout = false
        private val state = MutableStateFlow(StudentSession(isRestoring = false, profile = initial))
        override val session: StateFlow<StudentSession> = state
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            state.value = StudentSession(isRestoring = false, profile = profile, remembered = remember)
        }
        override suspend fun updateProfile(profile: StudentProfile) {
            state.value = state.value.copy(profile = profile)
        }
        override suspend fun enableBiometric(studentId: String) {
            state.value = state.value.copy(biometricStudentId = studentId)
        }
        override suspend fun disableBiometric() {
            state.value = state.value.copy(biometricStudentId = null)
        }
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun signOut() {
            if (failNextLogout) {
                failNextLogout = false
                throw IllegalStateException("Simulated storage failure")
            }
            state.value = StudentSession(isRestoring = false)
        }
    }
    private class FakeAuth(private val demo: StudentProfile) : AuthRepository, StudentContactRepository {
        override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(demo)
        override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(demo)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) =
            Result.success(oldPassword == "UTH@123456")
        override suspend fun updateContact(studentId: String, phone: String, personalEmail: String, currentAddress: String) =
            Result.success(demo.copy(phoneNumber = phone, personalEmail = personalEmail, currentAddress = currentAddress))
    }
    private fun newVm(session: FakeSession) : ProfileViewModel {
        val auth = FakeAuth(demo)
        return ProfileViewModel(GetProfileUseCase(session), ManageStudentSessionUseCase(session),
            ChangePasswordUseCase(auth, session), UpdateStudentContactUseCase(auth, session))
    }

    @Test fun `logout dialog must be confirmed`() = runTest {
        val session = FakeSession(demo)
        val vm = newVm(session)
        advanceUntilIdle()
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        vm.onAction(ProfileAction.RequestLogout)
        assertTrue(vm.uiState.value.confirmLogout)
        assertEquals("2023040123", session.session.value.profile?.studentId)
        vm.onAction(ProfileAction.CancelLogout)
        assertFalse(vm.uiState.value.confirmLogout)
        vm.onAction(ProfileAction.RequestLogout)
        vm.onAction(ProfileAction.ConfirmLogout)
        advanceUntilIdle()
        assertNull(vm.uiState.value.student)
    }

    @Test fun `password validation protects old and confirmation`() = runTest {
        val vm = newVm(FakeSession(demo)); advanceUntilIdle()
        vm.onAction(ProfileAction.OpenPassword)
        vm.onAction(ProfileAction.OldPasswordChanged("UTH@123456"))
        vm.onAction(ProfileAction.NewPasswordChanged("weak"))
        vm.onAction(ProfileAction.ConfirmPasswordChanged("different"))
        vm.onAction(ProfileAction.SubmitPassword); advanceUntilIdle()
        assertEquals(ProfilePage.ChangePassword, vm.uiState.value.page)
        assertFalse(vm.uiState.value.passwordForm.errors.valid)
        vm.onAction(ProfileAction.NewPasswordChanged("UTH@New987"))
        vm.onAction(ProfileAction.ConfirmPasswordChanged("UTH@New987"))
        vm.onAction(ProfileAction.SubmitPassword); advanceUntilIdle()
        assertEquals(ProfileNotice.PasswordChanged, vm.uiState.value.notice)
        assertEquals("", vm.uiState.value.passwordForm.current)
    }

    @Test fun `contact changes reflected on overview`() = runTest {
        val vm = newVm(FakeSession(demo)); advanceUntilIdle()
        vm.onAction(ProfileAction.OpenContact)
        vm.onAction(ProfileAction.PhoneChanged("0912345678"))
        vm.onAction(ProfileAction.EmailChanged("student@gmail.com"))
        vm.onAction(ProfileAction.AddressChanged("123 Le Loi"))
        vm.onAction(ProfileAction.SubmitContact); advanceUntilIdle()
        assertEquals("0912345678", vm.uiState.value.student?.phoneNumber)
        assertEquals(ProfilePage.Overview, vm.uiState.value.page)
    }

    @Test fun `biometric flag only enabled after explicit success action`() = runTest {
        val vm = newVm(FakeSession(demo)); advanceUntilIdle()
        vm.onAction(ProfileAction.RequestBiometricEnroll)
        assertFalse(vm.uiState.value.biometricEnabled)
        vm.onAction(ProfileAction.BiometricVerified); advanceUntilIdle()
        assertTrue(vm.uiState.value.biometricEnabled)
        vm.onAction(ProfileAction.DisableBiometric); advanceUntilIdle()
        assertFalse(vm.uiState.value.biometricEnabled)
    }
    @Test fun `logout failure shows retry and one time event then retry succeeds`() = runTest {
        val session = FakeSession(demo)
        session.failNextLogout = true
        val vm = newVm(session)
        advanceUntilIdle()
        val firstFailure = async { vm.events.first() }
        vm.onAction(ProfileAction.RequestLogout)
        vm.onAction(ProfileAction.ConfirmLogout)
        advanceUntilIdle()
        assertEquals(ProfileEvent.LogoutFailed, firstFailure.await())
        assertTrue(vm.uiState.value.confirmLogout)
        assertEquals(ProfileFailure.LogoutFailed, vm.uiState.value.failure)
        assertEquals(demo.studentId, session.session.value.profile?.studentId)

        vm.onAction(ProfileAction.RetryLogout)
        advanceUntilIdle()
        assertNull(session.session.value.profile)
        assertFalse(vm.uiState.value.confirmLogout)
        assertNull(vm.uiState.value.failure)
    }

}

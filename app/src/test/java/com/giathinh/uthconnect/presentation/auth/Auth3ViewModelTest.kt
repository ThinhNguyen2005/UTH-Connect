package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.ManageAuthAccountUseCase
import com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.SignUpUseCase
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Auth3ViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private fun vm(auth: Auth3FakeRepository, session: Auth3FakeSession) = AuthViewModel(
        SignInUseCase(auth, session), AccessStudentSessionUseCase(session), ResetPasswordUseCase(auth),
        SignUpUseCase(auth), SignInWithGoogleUseCase(auth, session), ManageAuthAccountUseCase(auth))
    @Test fun `signup rejects outside domain before repository`() = runTest {
        val auth = Auth3FakeRepository(); val vm = vm(auth, Auth3FakeSession(auth)); advanceUntilIdle()
        vm.onAction(AuthAction.ChangeMode(true)); vm.onAction(AuthAction.IdentifierChanged("user@gmail.com"))
        vm.onAction(AuthAction.PasswordChanged("Strong@12345")); vm.onAction(AuthAction.Submit); advanceUntilIdle()
        assertEquals(LoginFieldError.DomainNotAllowed, vm.uiState.value.identifierError)
        assertEquals(0, auth.registerCalls)
    }
    @Test fun `signup enters pending verification without granting main or retaining password`() = runTest {
        val auth = Auth3FakeRepository(); val session = Auth3FakeSession(auth); val vm = vm(auth, session); advanceUntilIdle()
        vm.onAction(AuthAction.ChangeMode(true)); vm.onAction(AuthAction.IdentifierChanged(" STUDENT@UT.EDU.VN "))
        vm.onAction(AuthAction.PasswordChanged("Strong@12345")); vm.onAction(AuthAction.Submit); advanceUntilIdle()
        assertEquals("student@ut.edu.vn", vm.uiState.value.pendingIdentity?.email)
        assertEquals("", vm.uiState.value.password); assertNull(session.session.value.profile)
    }
    @Test fun `network signup failure is visible and busy state clears`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.Network }; val vm = vm(auth, Auth3FakeSession(auth)); advanceUntilIdle()
        vm.onAction(AuthAction.ChangeMode(true)); vm.onAction(AuthAction.IdentifierChanged("student@ut.edu.vn"))
        vm.onAction(AuthAction.PasswordChanged("Strong@12345")); vm.onAction(AuthAction.Submit); advanceUntilIdle()
        assertEquals(AuthFailure.Network, vm.uiState.value.failure); assertFalse(vm.uiState.value.isSigningIn)
    }
    @Test fun `unverified login shows verification failure without access`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.EmailNotVerified }; val vm = vm(auth, Auth3FakeSession(auth)); advanceUntilIdle()
        vm.onAction(AuthAction.IdentifierChanged("student@ut.edu.vn")); vm.onAction(AuthAction.PasswordChanged("password"))
        vm.onAction(AuthAction.Submit); advanceUntilIdle()
        assertEquals(AuthFailure.VerificationRequired, vm.uiState.value.failure); assertNull(vm.uiState.value.student)
    }
    @Test fun `Google collision does not link or grant session`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.AccountCollision }; val session = Auth3FakeSession(auth); val vm = vm(auth, session); advanceUntilIdle()
        vm.onAction(AuthAction.RequestGoogle); vm.onAction(AuthAction.GoogleTokenReceived("test-only-token")); advanceUntilIdle()
        assertEquals(AuthFailure.AccountCollision, vm.uiState.value.failure); assertNull(session.session.value.profile)
    }
    @Test fun `Google cancellation is silent and never grants a session`() = runTest {
        val auth = Auth3FakeRepository(); val vm = vm(auth, Auth3FakeSession(auth)); advanceUntilIdle()
        vm.onAction(AuthAction.RequestGoogle); assertTrue(vm.uiState.value.isSigningIn)
        vm.onAction(AuthAction.GoogleCancelled); advanceUntilIdle()
        assertFalse(vm.uiState.value.isSigningIn); assertNull(vm.uiState.value.failure); assertNull(vm.uiState.value.student)
    }
}

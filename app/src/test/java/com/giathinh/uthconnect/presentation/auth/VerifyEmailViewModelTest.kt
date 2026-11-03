package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.AuthIdentity
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.usecase.SendVerificationUseCase
import com.giathinh.uthconnect.domain.usecase.CheckVerificationUseCase
import com.giathinh.uthconnect.domain.usecase.SignOutUseCase
import com.giathinh.uthconnect.domain.usecase.ManageAuthAccountUseCase
import com.giathinh.uthconnect.presentation.auth.verifyemail.VerifyEmailViewModel
import com.giathinh.uthconnect.presentation.auth.verifyemail.VerifyEmailAction
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerifyEmailViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private fun vm(auth: Auth3FakeRepository, session: Auth3FakeSession): VerifyEmailViewModel {
        auth.pending.value = AuthIdentity("uid-test", "student@ut.edu.vn", false)
        return VerifyEmailViewModel(SendVerificationUseCase(auth), CheckVerificationUseCase(auth, session),
            SignOutUseCase(session), ManageAuthAccountUseCase(auth))
    }
    @Test fun `unverified email cannot transition to main`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.EmailNotVerified }; val session = Auth3FakeSession(auth); val vm = vm(auth, session)
        vm.onAction(VerifyEmailAction.Check(true)); runCurrent()
        assertEquals(AuthError.EmailNotVerified, vm.uiState.value.error); assertNull(session.session.value.profile)
    }
    @Test fun `domain failure cannot transition to main`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.DomainNotAllowed }; val session = Auth3FakeSession(auth); val vm = vm(auth, session)
        vm.onAction(VerifyEmailAction.Check(true)); runCurrent()
        assertEquals(AuthError.DomainNotAllowed, vm.uiState.value.error); assertNull(session.session.value.profile)
    }
    @Test fun `network failure stays retryable without access`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.Network }; val session = Auth3FakeSession(auth); val vm = vm(auth, session)
        vm.onAction(VerifyEmailAction.Check(true)); runCurrent()
        assertEquals(AuthError.Network, vm.uiState.value.error); assertNull(session.session.value.profile)
        auth.error = null; vm.onAction(VerifyEmailAction.Check(true)); runCurrent()
        assertNotNull(session.session.value.profile)
    }
    @Test fun `resend observes sixty second cooldown and rejects duplicate requests`() = runTest {
        val auth = Auth3FakeRepository(); val vm = vm(auth, Auth3FakeSession(auth))
        assertEquals(60, vm.uiState.value.cooldownSeconds)
        vm.onAction(VerifyEmailAction.Resend); assertEquals(0, auth.sends)
        advanceTimeBy(59000); runCurrent(); assertEquals(1, vm.uiState.value.cooldownSeconds)
        advanceTimeBy(1000); runCurrent(); vm.onAction(VerifyEmailAction.Resend); runCurrent()
        assertEquals(1, auth.sends); assertEquals(60, vm.uiState.value.cooldownSeconds)
        vm.onAction(VerifyEmailAction.Resend); assertEquals(1, auth.sends)
    }
    @Test fun `logout removes pending identity and session`() = runTest {
        val auth = Auth3FakeRepository(); val session = Auth3FakeSession(auth); val vm = vm(auth, session)
        vm.onAction(VerifyEmailAction.SignOut); runCurrent()
        assertNull(auth.pending.value); assertNull(session.session.value.profile)
    }
}

package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.usecase.ResetPasswordUseCase
import com.giathinh.uthconnect.presentation.auth.forgotpassword.ForgotPasswordViewModel
import com.giathinh.uthconnect.presentation.auth.forgotpassword.ForgotPasswordAction
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ForgotPasswordViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    @Test fun `outside domain is rejected before sending`() = runTest {
        val auth = Auth3FakeRepository(); val vm = ForgotPasswordViewModel(ResetPasswordUseCase(auth))
        vm.onAction(ForgotPasswordAction.EmailChanged("user@gmail.com")); vm.onAction(ForgotPasswordAction.Send); advanceUntilIdle()
        assertEquals(AuthError.DomainNotAllowed, vm.uiState.value.error); assertEquals(0, auth.resets)
    }
    @Test fun `network failure clears loading and allows retry`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.Network }; val vm = ForgotPasswordViewModel(ResetPasswordUseCase(auth))
        vm.onAction(ForgotPasswordAction.EmailChanged("student@ut.edu.vn")); vm.onAction(ForgotPasswordAction.Send); advanceUntilIdle()
        assertEquals(AuthError.Network, vm.uiState.value.error); assertFalse(vm.uiState.value.isSending)
        auth.error = null; vm.onAction(ForgotPasswordAction.Send); advanceUntilIdle(); assertTrue(vm.uiState.value.sent)
    }
    @Test fun `success uses generic state and editing clears old notice`() = runTest {
        val vm = ForgotPasswordViewModel(ResetPasswordUseCase(Auth3FakeRepository()))
        vm.onAction(ForgotPasswordAction.EmailChanged("student@ut.edu.vn")); vm.onAction(ForgotPasswordAction.Send); advanceUntilIdle()
        assertTrue(vm.uiState.value.sent); assertNull(vm.uiState.value.error)
        vm.onAction(ForgotPasswordAction.EmailChanged("other@ut.edu.vn")); assertFalse(vm.uiState.value.sent)
    }
}

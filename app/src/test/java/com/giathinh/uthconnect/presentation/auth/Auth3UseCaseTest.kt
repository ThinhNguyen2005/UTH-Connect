package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.domain.model.AuthIdentity
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.giathinh.uthconnect.domain.usecase.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class Auth3UseCaseTest {
    @Test fun `signup rejects domain and weak password before repository`() = runTest {
        val auth = Auth3FakeRepository()
        assertTrue(SignUpUseCase(auth)("x@gmail.com", "Strong@123").isFailure)
        assertTrue(SignUpUseCase(auth)("x@ut.edu.vn", "weak").isFailure)
        assertEquals(0, auth.registerCalls)
    }
    @Test fun `signup propagates collision without opening session`() = runTest {
        val auth = Auth3FakeRepository().apply { error = AuthError.EmailAlreadyInUse }
        val result = SignUpUseCase(auth)("x@ut.edu.vn", "Strong@123")
        assertEquals(AuthError.EmailAlreadyInUse, (result.exceptionOrNull() as AuthException).reason)
    }
    @Test fun `google requires token and propagates account collision`() = runTest {
        val auth = Auth3FakeRepository(); val sessions = Auth3FakeSession(auth)
        assertTrue(SignInWithGoogleUseCase(auth, sessions)("", true).isFailure)
        auth.error = AuthError.AccountCollision
        val result = SignInWithGoogleUseCase(auth, sessions)("test-token", true)
        assertEquals(AuthError.AccountCollision, (result.exceptionOrNull() as AuthException).reason)
        assertNull(sessions.session.value.profile)
    }
    @Test fun `cancelled signup propagates cancellation`() = runTest {
        val auth = Auth3FakeRepository().apply { cancels = true }
        try { SignUpUseCase(auth)("x@ut.edu.vn", "Strong@123"); fail("Must cancel") }
        catch (_: CancellationException) { assertNull(auth.pending.value) }
    }
    @Test fun `cancelled verification session start revokes partial authentication`() = runTest {
        val auth = Auth3FakeRepository(); val sessions = Auth3FakeSession(auth).apply { startError = CancellationException("cancel") }
        try { CheckVerificationUseCase(auth, sessions)(true); fail("Must cancel") }
        catch (_: CancellationException) { assertNull(auth.pending.value); assertNull(sessions.session.value.profile) }
    }
    @Test fun `google session persistence failure cannot leave active session`() = runTest {
        val auth = Auth3FakeRepository(); val sessions = Auth3FakeSession(auth).apply { startError = IllegalStateException("disk") }
        assertTrue(SignInWithGoogleUseCase(auth, sessions)("test-token", true).isFailure)
        assertNull(sessions.session.value.profile)
    }
}

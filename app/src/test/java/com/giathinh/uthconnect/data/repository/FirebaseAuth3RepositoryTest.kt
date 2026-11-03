package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FirebaseAuth3RepositoryTest {
    private class Source : FirebaseAuthSource {
        override val changes = emptyFlow<Boolean>()
        override val tokenChanges = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
        var value = FirebaseAccount("uid", "student@ut.edu.vn", false, emptyMap(), mapOf("displayName" to "Student"))
        var calls = 0
        var signOuts = 0
        var verifyCalls = 0
        var verifyError: Exception? = null
        var error: Exception? = null
        override suspend fun identity() = value
        override suspend fun account(): FirebaseAccount { error?.let { throw it }; return value }
        override suspend fun register(email: String, password: String) { calls++; error?.let { throw it }; value = value.copy(email = email) }
        override suspend fun signInGoogle(token: String) { calls++; error?.let { throw it } }
        override suspend fun signIn(email: String, password: String) { calls++ }
        override suspend fun sendVerification() = Unit
        override suspend fun verifyUthAccount() { verifyCalls++; verifyError?.let { throw it }; value = value.copy(claims = value.claims + ("uthVerified" to true) + ("role" to "STUDENT")) }
        override fun signOut() { signOuts++ }
        override suspend fun resetPassword(email: String) = Unit
        override suspend fun changePassword(old: String, updated: String) = Unit
        override suspend fun updateContact(fields: Map<String, String>) = Unit
    }
    @Test fun `registration normalizes email and exposes pending identity only`() = runTest {
        val source = Source(); val repository = FirebaseAuthRepository(source)
        val result = repository.register(" Student@UT.EDU.VN ", "Strong@123").getOrThrow()
        assertEquals("student@ut.edu.vn", result.email)
        assertEquals(result, repository.observePendingIdentity().first())
        assertFalse(result.emailVerified)
    }
    @Test fun `outside domain registration never calls SDK`() = runTest {
        val source = Source(); val result = FirebaseAuthRepository(source).register("x@ut.edu.vn.evil.com", "Strong@123")
        assertEquals(AuthError.DomainNotAllowed, (result.exceptionOrNull() as AuthException).reason)
        assertEquals(0, source.calls)
    }
    @Test fun `verification reloads identity and requires verified email before callable`() = runTest {
        val source = Source(); val repository = FirebaseAuthRepository(source)
        assertEquals(AuthError.EmailNotVerified, (repository.checkVerification().exceptionOrNull() as AuthException).reason)
        assertEquals(0, source.verifyCalls)
        source.value = source.value.copy(emailVerified = true)
        val profile = repository.checkVerification().getOrThrow()
        assertEquals("uid", profile.authUid); assertEquals("", profile.studentId)
        assertTrue(profile.uthVerified); assertEquals(UserRole.STUDENT, profile.role)
    }
    @Test fun `Firestore role cannot override claims`() = runTest {
        val source = Source().apply { value = value.copy(emailVerified = true, claims = mapOf("uthVerified" to true, "role" to "STUDENT"), fields = mapOf("displayName" to "Student", "role" to "ADMIN")) }
        val profile = FirebaseAuthRepository(source).signInWithGoogle("test-token").getOrThrow()!!
        assertEquals(UserRole.STUDENT, profile.role)
    }
    @Test fun `missing and unknown role claims cannot become student locally`() = runTest {
        for (claims in listOf(mapOf<String, Any>("uthVerified" to true), mapOf("uthVerified" to true, "role" to "ROOT"))) {
            val source = Source().apply { value = value.copy(emailVerified = true, claims = claims) }
            val result = FirebaseAuthRepository(source).signInWithGoogle("test-token")
            assertEquals(AuthError.ApprovalRequired, (result.exceptionOrNull() as AuthException).reason)
            assertNull(result.getOrNull())
        }
    }
    @Test fun `google outside university is signed out without profile`() = runTest {
        val source = Source().apply { value = value.copy(email = "student@gmail.com", emailVerified = true) }
        val result = FirebaseAuthRepository(source).signInWithGoogle("test-token")
        assertEquals(AuthError.DomainNotAllowed, (result.exceptionOrNull() as AuthException).reason)
        assertEquals(1, source.signOuts)
    }
    @Test fun `google cancellation revokes partial credential and propagates`() = runTest {
        val source = Source().apply { error = CancellationException("cancel") }
        try { FirebaseAuthRepository(source).signInWithGoogle("test-token"); fail("Must cancel") }
        catch (_: CancellationException) { assertEquals(1, source.signOuts) }
    }
    @Test fun `explicit revoked UTH claim cannot be self renewed during sign in or verification`() = runTest {
        val source = Source().apply { value = value.copy(emailVerified = true,
            claims = mapOf("role" to "ORGANIZER", "uthVerified" to false)) }
        val repository = FirebaseAuthRepository(source)
        assertEquals(AuthError.PermissionDenied, (repository.signInWithGoogle("test-token").exceptionOrNull() as AuthException).reason)
        assertEquals(AuthError.PermissionDenied, (repository.checkVerification().exceptionOrNull() as AuthException).reason)
        assertEquals(0, source.verifyCalls)
        assertNull(repository.observePendingIdentity().first())
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `token revocation and network failure remove profile without provisioning claims`() = runTest {
        val source = Source().apply { value = value.copy(emailVerified = true,
            claims = mapOf("role" to "STUDENT", "uthVerified" to true)) }
        val repository = FirebaseAuthRepository(source)
        val profiles = mutableListOf<com.giathinh.uthconnect.domain.model.StudentProfile?>()
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            repository.observeProfileChanges().collect { profiles.add(it) }
        }
        source.tokenChanges.emit(Unit)
        assertEquals(UserRole.STUDENT, profiles.last()?.role)
        source.value = source.value.copy(claims = mapOf("role" to "ADMIN", "uthVerified" to false))
        source.tokenChanges.emit(Unit)
        assertNull(profiles.last())
        source.value = source.value.copy(claims = mapOf("role" to "ORGANIZER", "uthVerified" to true))
        source.tokenChanges.emit(Unit)
        assertEquals(UserRole.ORGANIZER, profiles.last()?.role)
        source.error = java.io.IOException("offline")
        source.tokenChanges.emit(Unit)
        assertNull(profiles.last())
        assertEquals(0, source.verifyCalls)
    }

    @Test fun `missing callable and network provisioning failure retain pending identity without profile`() = runTest {
        for (reason in listOf(AuthError.Configuration, AuthError.Network)) {
            val source = Source().apply {
                value = value.copy(emailVerified = true)
                verifyError = AuthException(reason)
            }
            val repository = FirebaseAuthRepository(source)
            val result = repository.authenticate("student@ut.edu.vn", "password")
            assertEquals(reason, (result.exceptionOrNull() as AuthException).reason)
            assertNull(result.getOrNull())
            assertTrue(repository.observePendingIdentity().first()!!.emailVerified)
        }
    }

}

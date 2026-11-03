package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthRepositoryTest {
    private class Source : FirebaseAuthSource {
        override val changes = emptyFlow<Boolean>()
        var account = FirebaseAccount("uid-a", "student@ut.edu.vn", true,
            mapOf("uthVerified" to true, "role" to "student"),
            mapOf("studentId" to "2023040123", "displayName" to "Student", "role" to "ADMIN"))
        var error: Exception? = null
        var signedOut = false
        var signInCalls = 0
        var verificationSent = false
        var resetEmail = ""
        var password = "Old@12345"
        var contact = emptyMap<String, String>()
        override suspend fun signIn(email: String, password: String) { signInCalls++; error?.let { throw it } }
        override suspend fun account(): FirebaseAccount { error?.let { throw it }; return account }
        override fun signOut() { signedOut = true }
        override suspend fun resetPassword(email: String) { error?.let { throw it }; resetEmail = email }
        override suspend fun changePassword(old: String, updated: String) {
            if (old != password) throw AuthException(AuthError.InvalidCredentials)
            password = updated
        }
        override suspend fun updateContact(fields: Map<String, String>) {
            contact = fields
            account = account.copy(fields = account.fields + fields)
        }
        override suspend fun sendVerification() { verificationSent = true }
    }

    @Test fun `profile role cannot be elevated by firestore fields`() = runTest {
        val source = Source()
        val profile = FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "password").getOrThrow()
        assertEquals(UserRole.STUDENT, profile?.role)
        assertEquals("uid-a", profile?.authUid)
    }
    @Test fun `organizer and admin come from claims only`() = runTest {
        for ((claim, role) in listOf("organizer" to UserRole.ORGANIZER, "admin" to UserRole.ADMIN)) {
            val source = Source()
            source.account = source.account.copy(claims = mapOf("uthVerified" to true, "role" to claim))
            assertEquals(role, FirebaseAuthRepository(source).restoreAuthenticatedProfile().getOrThrow()?.role)
        }
    }
    @Test fun `document verification cannot replace verified claim`() = runTest {
        val source = Source()
        source.account = source.account.copy(claims = emptyMap(), fields = source.account.fields + ("uthVerified" to true))
        val result = FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "password")
        assertEquals(AuthError.ApprovalRequired, (result.exceptionOrNull() as AuthException).reason)
        assertFalse(source.signedOut)
        assertNull(result.getOrNull())
    }
    @Test fun `unverified email sends verification and never grants profile`() = runTest {
        val source = Source()
        source.account = source.account.copy(emailVerified = false)
        val result = FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "password")
        assertEquals(AuthError.EmailNotVerified, (result.exceptionOrNull() as AuthException).reason)
        assertTrue(source.verificationSent)
        assertFalse(source.signedOut)
        assertNull(result.getOrNull())
    }
    @Test fun `student ID does not fabricate an email or contact Firebase`() = runTest {
        val source = Source()
        val result = FirebaseAuthRepository(source).authenticate("2023040123", "password")
        assertEquals(AuthError.StudentIdUnavailable, (result.exceptionOrNull() as AuthException).reason)
        assertEquals(0, source.signInCalls)
    }
    @Test fun `bad credentials return no profile`() = runTest {
        val source = Source().apply { error = AuthException(AuthError.InvalidCredentials) }
        assertNull(FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "wrong").getOrThrow())
        assertTrue(source.signedOut)
    }
    @Test fun `disabled account revokes restored authentication`() = runTest {
        val source = Source().apply { error = AuthException(AuthError.Disabled) }
        val result = FirebaseAuthRepository(source).restoreAuthenticatedProfile()
        assertEquals(AuthError.Disabled, (result.exceptionOrNull() as AuthException).reason)
        assertTrue(source.signedOut)
    }
    @Test fun `cancellation propagates and revokes partial sign in`() = runTest {
        val source = Source().apply { error = CancellationException("cancel") }
        try {
            FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "password")
            throw AssertionError("Cancellation must propagate")
        } catch (_: CancellationException) { assertTrue(source.signedOut) }
    }
    @Test fun `password change reauthenticates and old password stops working`() = runTest {
        val repository = FirebaseAuthRepository(Source())
        assertTrue(repository.changePassword("2023040123", "Old@12345", "New@12345").getOrThrow())
        assertFalse(repository.changePassword("2023040123", "Old@12345", "Next@12345").getOrThrow())
        assertTrue(repository.changePassword("2023040123", "New@12345", "Next@12345").getOrThrow())
    }
    @Test fun `contact update writes only editable fields`() = runTest {
        val source = Source()
        FirebaseAuthRepository(source).updateContact("2023040123", "0912345678", "me@example.com", "Address").getOrThrow()
        assertEquals(setOf("phoneNumber", "personalEmail", "currentAddress"), source.contact.keys)
    }
    @Test fun `reset password delegates to configured Firebase source`() = runTest {
        val source = Source()
        assertTrue(FirebaseAuthRepository(source).sendPasswordReset("student@ut.edu.vn").isSuccess)
        assertEquals("student@ut.edu.vn", source.resetEmail)
    }
    @Test fun `new repository reads saved contact from the source`() = runTest {
        val source = Source()
        FirebaseAuthRepository(source).updateContact("2023040123", "0912345678", "me@example.com", "Address").getOrThrow()
        val restored = FirebaseAuthRepository(source).restoreAuthenticatedProfile().getOrThrow()
        assertEquals("0912345678", restored?.phoneNumber)
        assertEquals("me@example.com", restored?.personalEmail)
    }
}

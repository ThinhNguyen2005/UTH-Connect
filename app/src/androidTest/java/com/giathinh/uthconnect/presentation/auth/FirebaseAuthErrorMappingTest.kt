package com.giathinh.uthconnect.presentation.auth

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.data.repository.FirebaseAccount
import com.giathinh.uthconnect.data.repository.FirebaseAuthRepository
import com.giathinh.uthconnect.data.repository.FirebaseAuthSource
import com.giathinh.uthconnect.data.repository.SdkFirebaseAuthSource
import com.google.firebase.FirebaseApp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exception SDK cần Android runtime; các ca này không truy cập Firebase project. */
@RunWith(AndroidJUnit4::class)
class FirebaseAuthErrorMappingTest {
    private class Source(private val error: Exception) : FirebaseAuthSource {
        override val changes = emptyFlow<Boolean>()
        var signedOut = false
        override suspend fun signIn(email: String, password: String): Unit = throw error
        override suspend fun account(): FirebaseAccount? = throw error
        override fun signOut() { signedOut = true }
        override suspend fun resetPassword(email: String): Unit = throw error
        override suspend fun changePassword(old: String, updated: String): Unit = throw error
        override suspend fun updateContact(fields: Map<String, String>): Unit = throw error
        override suspend fun sendVerification() = Unit
    }
    @Test fun sdkRegistrationAndCollisionErrorsAreTypedWithoutGrantingProfile() = runBlocking {
        mapOf("ERROR_EMAIL_ALREADY_IN_USE" to AuthError.EmailAlreadyInUse,
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" to AuthError.AccountCollision,
            "ERROR_CREDENTIAL_ALREADY_IN_USE" to AuthError.AccountCollision,
            "ERROR_WEAK_PASSWORD" to AuthError.WeakPassword).forEach { (code, expected) ->
            val result = FirebaseAuthRepository(Source(FirebaseAuthException(code, "Test SDK error")))
                .authenticate("student@ut.edu.vn", "password")
            assertEquals(expected, (result.exceptionOrNull() as AuthException).reason)
            assertNull(result.getOrNull())
        }
    }
    @Test fun sdkInvalidCredentialsNeverGrantProfile() = runBlocking {
        val source = Source(FirebaseAuthException("ERROR_INVALID_CREDENTIAL", "Invalid"))
        assertNull(FirebaseAuthRepository(source).authenticate("student@ut.edu.vn", "wrong").getOrThrow())
        assertTrue(source.signedOut)
    }
    @Test fun sdkNetworkErrorIsReportedAsNetwork() = runBlocking {
        val result = FirebaseAuthRepository(Source(FirebaseNetworkException("Offline")))
            .authenticate("student@ut.edu.vn", "password")
        assertEquals(AuthError.Network, (result.exceptionOrNull() as AuthException).reason)
    }
    @Test fun sdkDisabledAccountRevokesSession() = runBlocking {
        val source = Source(FirebaseAuthException("ERROR_USER_DISABLED", "Disabled"))
        val result = FirebaseAuthRepository(source).restoreAuthenticatedProfile()
        assertEquals(AuthError.Disabled, (result.exceptionOrNull() as AuthException).reason)
        assertTrue(source.signedOut)
    }
    @Test fun sdkMissingResetAccountDoesNotDiscloseExistence() = runBlocking {
        val source = Source(FirebaseAuthException("ERROR_USER_NOT_FOUND", "Missing"))
        assertTrue(FirebaseAuthRepository(source).sendPasswordReset("missing@ut.edu.vn").isSuccess)
    }
    @Test fun missingProjectConfigurationCannotGrantAuthentication() = runBlocking {
        assumeTrue(FirebaseApp.getApps(InstrumentationRegistry.getInstrumentation().targetContext).isEmpty())
        val result = FirebaseAuthRepository(SdkFirebaseAuthSource(
            javax.inject.Provider { com.google.firebase.auth.FirebaseAuth.getInstance() },
            javax.inject.Provider { com.google.firebase.firestore.FirebaseFirestore.getInstance() })).authenticate("student@ut.edu.vn", "password")
        assertEquals(AuthError.Configuration, (result.exceptionOrNull() as AuthException).reason)
    }
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.GoogleSignInUnsupportedException
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** A Google session must never become a remembered-login grant, verified or not. */
class SignInWithGoogleUseCaseTest {
    private val student = StudentProfile("2023040123", "minh.anh@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")

    private class RecordingSession : SessionRepository {
        private val current = MutableStateFlow(StudentSession(isRestoring = false))
        override val session: StateFlow<StudentSession> = current
        var startedWith: StudentProfile? = null
        var startedWithRemember: Boolean? = null
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            startedWith = profile
            startedWithRemember = remember
            current.value = StudentSession(isRestoring = false, profile = profile, remembered = remember)
        }
        override suspend fun updateProfile(profile: StudentProfile) { current.value = current.value.copy(profile = profile) }
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun signOut() { current.value = StudentSession(isRestoring = false) }
    }

    private class FakeAuth(
        private val outcome: suspend () -> Result<StudentProfile>,
    ) : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(null)
        override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String) = outcome()
    }

    @Test fun `Verified Google identity opens a non-remembered session`() = runBlocking {
        val session = RecordingSession()
        val result = SignInWithGoogleUseCase(
            FakeAuth { Result.success(student) }, session,
        )("id-token", "raw-nonce")
        assertEquals(SignInWithGoogleResult.Success(student), result)
        assertEquals(student, session.startedWith)
        assertEquals(false, session.startedWithRemember)
    }

    @Test fun `Unverified MSSV is pending, never a hard error`() = runBlocking {
        val session = RecordingSession()
        val auth = FakeAuth {
            Result.failure(StudentVerificationRequiredException("minh.anh@ut.edu.vn", "Nguyễn Minh Anh"))
        }
        val result = SignInWithGoogleUseCase(auth, session)("id-token", "raw-nonce")
        assertEquals(
            SignInWithGoogleResult.VerificationPending("minh.anh@ut.edu.vn", "Nguyễn Minh Anh"),
            result,
        )
        assertEquals(null, session.startedWith)
    }

    @Test fun `Backends without Google report unsupported`() = runBlocking {
        // The interface default is what Firebase and demo repositories actually use.
        val auth = object : AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(null)
            override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
        }
        val result = SignInWithGoogleUseCase(auth, RecordingSession())("id-token", "raw-nonce")
        assertEquals(SignInWithGoogleResult.Unsupported, result)
    }

    @Test fun `Provider failure becomes Rejected`() = runBlocking {
        val session = RecordingSession()
        val auth = FakeAuth { Result.failure(IllegalStateException("invalid nonce")) }
        val result = SignInWithGoogleUseCase(auth, session)("id-token", "raw-nonce")
        assertEquals(SignInWithGoogleResult.Rejected, result)
        assertEquals(null, session.startedWith)
    }

    @Test fun `Google sign-in keeps the unsupported exception type reachable`() = runBlocking {
        val result = SignInWithGoogleUseCase(
            FakeAuth { Result.failure(GoogleSignInUnsupportedException()) }, RecordingSession(),
        )("id-token", "raw-nonce")
        assertEquals(SignInWithGoogleResult.Unsupported, result)
    }
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInUseCaseTest {
    private val student = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")
    private val auth = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> =
            Result.success(student.takeIf {
                (identifier == student.studentId || identifier.equals(student.email, true)) && password == "correct"
            })
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(student)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
    }
    private val session = object : SessionRepository {
        private val current = MutableStateFlow(StudentSession(isRestoring = false))
        override val session: StateFlow<StudentSession> = current
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            current.value = StudentSession(isRestoring = false, profile = profile, remembered = remember)
        }
        override suspend fun updateProfile(profile: StudentProfile) { current.value = current.value.copy(profile = profile) }
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun signOut() { current.value = StudentSession(isRestoring = false) }
    }

    @Test fun `Hop le thi login va nhan dung Profile`() = runBlocking {
        val result = SignInUseCase(auth, session)("demo@ut.edu.vn", "correct", true)
        assertTrue(result is SignInResult.Success)
        assertEquals("2023040123", session.session.value.profile?.studentId)
        assertTrue(session.session.value.remembered)
    }
    @Test fun `Sai mat khau khong tao session`() = runBlocking {
        val result = SignInUseCase(auth, session)("2023040123", "wrong", true)
        assertEquals(SignInResult.InvalidCredentials, result)
        assertEquals(null, session.session.value.profile)
    }
    @Test fun `Validate chay truoc Repository`() = runBlocking {
        val result = SignInUseCase(auth, session)("gmail.com", "", false)
        assertEquals(SignInResult.InvalidForm(LoginFieldError.InvalidIdentifier, LoginFieldError.Required), result)
    }
}

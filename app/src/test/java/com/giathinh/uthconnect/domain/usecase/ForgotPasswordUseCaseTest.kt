package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ForgotPasswordUseCaseTest {

    private fun createAuthRepo(
        onReset: (String) -> Result<Unit> = { Result.success(Unit) },
    ) = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
        override suspend fun sendPasswordReset(email: String): Result<Unit> = onReset(email)
    }

    @Test
    fun `quen mat khau thanh cong voi email sinh vien`() = runBlocking {
        var recordedEmail = ""
        val repo = createAuthRepo { email ->
            recordedEmail = email
            Result.success(Unit)
        }
        val useCase = ForgotPasswordUseCase(repo)
        val result = useCase("2151010001@ut.edu.vn")
        assertEquals(ForgotPasswordOutcome.Success, result)
        assertEquals("2151010001@ut.edu.vn", recordedEmail)
    }

    @Test
    fun `quen mat khau voi MSSV tu dong gan domain ut edu vn`() = runBlocking {
        var recordedEmail = ""
        val repo = createAuthRepo { email ->
            recordedEmail = email
            Result.success(Unit)
        }
        val useCase = ForgotPasswordUseCase(repo)
        val result = useCase("2023040123")
        assertEquals(ForgotPasswordOutcome.Success, result)
        assertEquals("2023040123@ut.edu.vn", recordedEmail)
    }

    @Test
    fun `tu choi email sai dinh dang hoac khong thuoc truong`() = runBlocking {
        val useCase = ForgotPasswordUseCase(createAuthRepo())
        val result = useCase("user@gmail.com")
        assertTrue(result is ForgotPasswordOutcome.InvalidEmail)
    }

    @Test
    fun `bat loi mat mang IOException`() = runBlocking {
        val repo = createAuthRepo { Result.failure(IOException("Connection timed out")) }
        val useCase = ForgotPasswordUseCase(repo)
        val result = useCase("student@ut.edu.vn")
        assertEquals(ForgotPasswordOutcome.NetworkError, result)
    }

    @Test
    fun `bat loi rate limit 429`() = runBlocking {
        val repo = createAuthRepo { Result.failure(IllegalStateException("429 Too Many Requests (rate limit exceeded)")) }
        val useCase = ForgotPasswordUseCase(repo)
        val result = useCase("student@ut.edu.vn")
        assertEquals(ForgotPasswordOutcome.RateLimited, result)
    }

    @Test
    fun `anti-enumeration tra ve Success khi tai khoan khong ton tai`() = runBlocking {
        val repo = createAuthRepo { Result.failure(IllegalStateException("User not found")) }
        val useCase = ForgotPasswordUseCase(repo)
        val result = useCase("student@ut.edu.vn")
        assertEquals(ForgotPasswordOutcome.Success, result)
    }

    @Test
    fun `unsupported backend does not report success`() = runBlocking {
        val useCase = ForgotPasswordUseCase(createAuthRepo { Result.failure(UnsupportedOperationException()) })
        assertTrue(useCase("student@ut.edu.vn") is ForgotPasswordOutcome.Failure)
    }

    @Test
    fun `backend errors do not report success`() = runBlocking {
        val useCase = ForgotPasswordUseCase(createAuthRepo { Result.failure(IllegalStateException("server unavailable")) })
        assertTrue(useCase("student@ut.edu.vn") is ForgotPasswordOutcome.Failure)
    }
}

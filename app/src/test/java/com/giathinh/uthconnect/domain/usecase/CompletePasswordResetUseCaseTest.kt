package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletePasswordResetUseCaseTest {

    private fun createAuthRepo(
        onComplete: (String, String, String) -> Result<Unit> = { _, _, _ -> Result.success(Unit) },
    ) = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
        override suspend fun completePasswordReset(
            email: String,
            otp: String,
            newPassword: String,
            recoveryToken: String?,
        ): Result<Unit> = onComplete(email, otp, newPassword)
    }

    @Test
    fun `dat lai mat khau thanh cong voi mat khau manh`() = runBlocking {
        var recordedPassword = ""
        val repo = createAuthRepo { email, otp, pass ->
            assertEquals("student@ut.edu.vn", email)
            assertEquals("123456", otp)
            recordedPassword = pass
            Result.success(Unit)
        }
        val useCase = CompletePasswordResetUseCase(repo)
        val result = useCase("student@ut.edu.vn", "123456", "StrongPassword123!", "StrongPassword123!")
        assertEquals(CompletePasswordResetOutcome.Success, result)
        assertEquals("StrongPassword123!", recordedPassword)
    }

    @Test
    fun `tu choi mat khau yeu khong co chu hoa hoac ky tu dac biet`() = runBlocking {
        val useCase = CompletePasswordResetUseCase(createAuthRepo())
        val result = useCase("student@ut.edu.vn", "123456", "weakpassword", "weakpassword")
        assertTrue(result is CompletePasswordResetOutcome.InvalidForm)
    }

    @Test
    fun `tu choi khi mat khau xac nhan khong khop`() = runBlocking {
        val useCase = CompletePasswordResetUseCase(createAuthRepo())
        val result = useCase("student@ut.edu.vn", "123456", "StrongPassword123!", "DifferentPassword123!")
        assertTrue(result is CompletePasswordResetOutcome.InvalidForm)
        val form = result as CompletePasswordResetOutcome.InvalidForm
        assertEquals("Mật khẩu xác nhận không khớp", form.confirmError)
    }

    @Test
    fun `xu ly token het han tu backend`() = runBlocking {
        val repo = createAuthRepo { _, _, _ -> Result.failure(IllegalStateException("Auth token has expired")) }
        val useCase = CompletePasswordResetUseCase(repo)
        val result = useCase("student@ut.edu.vn", "123456", "StrongPassword123!", "StrongPassword123!")
        assertEquals(CompletePasswordResetOutcome.TokenExpired, result)
    }

    @Test
    fun `rejects missing OTP without sending to server`() = runBlocking {
        var called = false
        val repo = createAuthRepo { _, _, _ ->
            called = true
            Result.success(Unit)
        }
        val result = CompletePasswordResetUseCase(repo)(
            "student@ut.edu.vn",
            "",
            "StrongPassword123!",
            "StrongPassword123!",
        )
        assertTrue(result is CompletePasswordResetOutcome.Failure)
        assertTrue(!called)
    }

    @Test
    fun `dat lai mat khau thanh cong qua recovery token khong can otp`() = runBlocking {
        var recordedToken: String? = null
        val repo = object : AuthRepository {
            override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
            override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
            override suspend fun completePasswordReset(
                email: String,
                otp: String,
                newPassword: String,
                recoveryToken: String?,
            ): Result<Unit> {
                recordedToken = recoveryToken
                return Result.success(Unit)
            }
        }
        val useCase = CompletePasswordResetUseCase(repo)
        val result = useCase("student@ut.edu.vn", "", "StrongPassword123!", "StrongPassword123!", "valid-token")
        assertEquals(CompletePasswordResetOutcome.Success, result)
        assertEquals("valid-token", recordedToken)
    }
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletePasswordResetUseCaseTest {

    private fun createAuthRepo(
        onComplete: (String) -> Result<Unit> = { Result.success(Unit) },
    ) = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
        override suspend fun completePasswordReset(newPassword: String): Result<Unit> = onComplete(newPassword)
    }

    @Test
    fun `dat lai mat khau thanh cong voi mat khau manh`() = runBlocking {
        var recordedPassword = ""
        val repo = createAuthRepo { pass ->
            recordedPassword = pass
            Result.success(Unit)
        }
        val useCase = CompletePasswordResetUseCase(repo)
        val result = useCase("StrongPassword123!", "StrongPassword123!")
        assertEquals(CompletePasswordResetOutcome.Success, result)
        assertEquals("StrongPassword123!", recordedPassword)
    }

    @Test
    fun `tu choi mat khau yeu khong co chu hoa hoac ky tu dac biet`() = runBlocking {
        val useCase = CompletePasswordResetUseCase(createAuthRepo())
        val result = useCase("weakpassword", "weakpassword")
        assertTrue(result is CompletePasswordResetOutcome.InvalidForm)
    }

    @Test
    fun `tu choi khi mat khau xac nhan khong khop`() = runBlocking {
        val useCase = CompletePasswordResetUseCase(createAuthRepo())
        val result = useCase("StrongPassword123!", "DifferentPassword123!")
        assertTrue(result is CompletePasswordResetOutcome.InvalidForm)
        val form = result as CompletePasswordResetOutcome.InvalidForm
        assertEquals("Mật khẩu xác nhận không khớp", form.confirmError)
    }

    @Test
    fun `xu ly token het han tu backend`() = runBlocking {
        val repo = createAuthRepo { Result.failure(IllegalStateException("Auth token has expired")) }
        val useCase = CompletePasswordResetUseCase(repo)
        val result = useCase("StrongPassword123!", "StrongPassword123!")
        assertEquals(CompletePasswordResetOutcome.TokenExpired, result)
    }
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.VerificationRequestResult
import com.giathinh.uthconnect.domain.repository.VerificationStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitVerificationRequestUseCaseTest {

    private fun createAuthRepo(
        onSubmit: (String) -> Result<VerificationRequestResult> = {
            Result.success(VerificationRequestResult("req-123", VerificationStatus.PENDING))
        },
    ) = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
        override suspend fun submitVerificationRequest(studentId: String): Result<VerificationRequestResult> = onSubmit(studentId)
    }

    @Test
    fun `gui yeu cau xac minh MSSV thanh cong`() = runBlocking {
        var recordedMssv = ""
        val repo = createAuthRepo { mssv ->
            recordedMssv = mssv
            Result.success(VerificationRequestResult("req-456", VerificationStatus.PENDING))
        }
        val useCase = SubmitVerificationRequestUseCase(repo)
        val result = useCase("2151010001")
        assertTrue(result is SubmitVerificationOutcome.Success)
        assertEquals("req-456", (result as SubmitVerificationOutcome.Success).requestId)
        assertEquals("2151010001", recordedMssv)
    }

    @Test
    fun `MSSV qua ngan bi tu choi`() = runBlocking {
        val useCase = SubmitVerificationRequestUseCase(createAuthRepo())
        val result = useCase("123")
        assertTrue(result is SubmitVerificationOutcome.InvalidStudentId)
    }

    @Test
    fun `MSSV da xac minh roi bao AlreadyVerified`() = runBlocking {
        val repo = createAuthRepo {
            Result.failure(IllegalStateException("STUDENT_ALREADY_VERIFIED"))
        }
        val useCase = SubmitVerificationRequestUseCase(repo)
        val result = useCase("2151010001")
        assertEquals(SubmitVerificationOutcome.AlreadyVerified, result)
    }

    @Test
    fun `MSSV da bi nguoi khac lay bao AlreadyTaken`() = runBlocking {
        val repo = createAuthRepo {
            Result.failure(IllegalStateException("STUDENT_ID_ALREADY_TAKEN"))
        }
        val useCase = SubmitVerificationRequestUseCase(repo)
        val result = useCase("2151010001")
        assertEquals(SubmitVerificationOutcome.AlreadyTaken, result)
    }
}

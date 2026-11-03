package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResetPasswordUseCaseTest {
    private class Auth : AuthRepository {
        var email: String? = null
        var cancel = false
        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            if (cancel) throw CancellationException("cancel")
            this.email = email
            return Result.success(Unit)
        }
        override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(null)
        override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
    }
    @Test fun `valid university email is normalized before sending`() = runTest {
        val auth = Auth()
        assertTrue(ResetPasswordUseCase(auth)(" student@ut.edu.vn ").isSuccess)
        assertEquals("student@ut.edu.vn", auth.email)
    }
    @Test fun `student ID cannot be used as password reset email`() = runTest {
        val auth = Auth()
        try { ResetPasswordUseCase(auth)("2023040123"); throw AssertionError("Must reject ID") }
        catch (_: IllegalArgumentException) { assertEquals(null, auth.email) }
    }
    @Test fun `reset does not swallow cancellation`() = runTest {
        val auth = Auth().apply { cancel = true }
        try { ResetPasswordUseCase(auth)("student@ut.edu.vn"); throw AssertionError("Must cancel") }
        catch (_: CancellationException) { }
    }
}

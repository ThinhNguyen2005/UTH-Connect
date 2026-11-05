package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SignUpResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignUpUseCaseTest {

    private fun createAuthRepo(
        onSignUp: (String, String, String, String) -> Result<SignUpResult> = { email, _, _, _ ->
            Result.success(SignUpResult(needsEmailConfirmation = true, email = email))
        },
    ) = object : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(null)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(false)
        override suspend fun signUp(email: String, password: String, displayName: String, faculty: String): Result<SignUpResult> = onSignUp(email, password, displayName, faculty)
    }

    @Test
    fun `dang ky thanh cong voi email ut edu vn`() = runBlocking {
        val useCase = SignUpUseCase(createAuthRepo())
        val result = useCase(
            identifier = "student@ut.edu.vn",
            password = "securePassword123",
            confirmPassword = "securePassword123",
            displayName = "Nguyễn Văn A",
            faculty = "CNTT",
        )
        assertTrue(result is SignUpOutcome.Success)
        val success = result as SignUpOutcome.Success
        assertEquals("student@ut.edu.vn", success.email)
        assertTrue(success.needsEmailConfirmation)
    }

    @Test
    fun `dang ky thanh cong voi email uth edu vn`() = runBlocking {
        val useCase = SignUpUseCase(createAuthRepo())
        val result = useCase(
            identifier = "student@uth.edu.vn",
            password = "securePassword123",
            confirmPassword = "securePassword123",
            displayName = "Trần Thị B",
            faculty = "Kinh tế",
        )
        assertTrue(result is SignUpOutcome.Success)
        assertEquals("student@uth.edu.vn", (result as SignUpOutcome.Success).email)
    }

    @Test
    fun `dang ky voi MSSV 10 so tu dong gan duoi ut edu vn`() = runBlocking {
        var recordedEmail = ""
        val repo = createAuthRepo { email, _, _, _ ->
            recordedEmail = email
            Result.success(SignUpResult(needsEmailConfirmation = false, email = email))
        }
        val useCase = SignUpUseCase(repo)
        val result = useCase(
            identifier = "2151010001",
            password = "securePassword123",
            confirmPassword = "securePassword123",
            displayName = "Lê Văn C",
            faculty = "Điện - Điện tử",
        )
        assertTrue(result is SignUpOutcome.Success)
        assertEquals("2151010001@ut.edu.vn", recordedEmail)
    }

    @Test
    fun `tu choi email ngoai truong nhu gmail com`() = runBlocking {
        val useCase = SignUpUseCase(createAuthRepo())
        val result = useCase(
            identifier = "attacker@gmail.com",
            password = "securePassword123",
            confirmPassword = "securePassword123",
            displayName = "Hacker",
            faculty = "CNTT",
        )
        assertTrue(result is SignUpOutcome.InvalidForm)
        val form = result as SignUpOutcome.InvalidForm
        assertTrue(form.emailError?.contains("Chỉ chấp nhận email sinh viên") == true)
    }

    @Test
    fun `tu choi khi mat khau xac nhan khong khop`() = runBlocking {
        val useCase = SignUpUseCase(createAuthRepo())
        val result = useCase(
            identifier = "student@ut.edu.vn",
            password = "password123",
            confirmPassword = "password456",
            displayName = "Sinh viên",
            faculty = "CNTT",
        )
        assertTrue(result is SignUpOutcome.InvalidForm)
        assertEquals("Mật khẩu xác nhận không khớp", (result as SignUpOutcome.InvalidForm).confirmError)
    }

    @Test
    fun `tu choi khi mat khau qua ngan`() = runBlocking {
        val useCase = SignUpUseCase(createAuthRepo())
        val result = useCase(
            identifier = "student@ut.edu.vn",
            password = "123",
            confirmPassword = "123",
            displayName = "Sinh viên",
            faculty = "CNTT",
        )
        assertTrue(result is SignUpOutcome.InvalidForm)
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", (result as SignUpOutcome.InvalidForm).passwordError)
    }

    @Test
    fun `may chu bao loi DOMAIN_RESTRICTION thi tra ve DomainRestriction outcome`() = runBlocking {
        val repo = createAuthRepo { _, _, _, _ ->
            Result.failure(IllegalStateException("DOMAIN_RESTRICTION: Only @ut.edu.vn or @uth.edu.vn addresses are allowed"))
        }
        val useCase = SignUpUseCase(repo)
        val result = useCase(
            identifier = "student@ut.edu.vn",
            password = "password123",
            confirmPassword = "password123",
            displayName = "Sinh viên",
            faculty = "CNTT",
        )
        assertTrue(result is SignUpOutcome.DomainRestriction)
    }
}

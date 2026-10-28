package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.auth.DemoAuthRepository
import com.giathinh.uthconnect.domain.validation.InputError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseTest {
    private val login = LoginUseCase(DemoAuthRepository())

    @Test fun `correct MSSV and password reaches profile`() = runBlocking {
        val result = login("2023040123", "UTH@123456")
        assertTrue(result is LoginResult.Success)
        assertEquals("2023040123", (result as LoginResult.Success).profile.studentId)
    }

    @Test fun `correct school email can sign in`() = runBlocking {
        val result = login("  DEMO@UT.EDU.VN ", "UTH@123456")
        assertTrue(result is LoginResult.Success)
    }

    @Test fun `wrong password is rejected`() = runBlocking {
        assertEquals(LoginResult.InvalidCredentials, login("2023040123", "wrong"))
    }

    @Test fun `form validation happens before checking credentials`() = runBlocking {
        val result = login("abc", " ")
        assertTrue(result is LoginResult.InvalidForm)
        result as LoginResult.InvalidForm
        assertEquals(InputError.INVALID_IDENTIFIER, result.identifierError)
        assertEquals(InputError.REQUIRED, result.passwordError)
    }
}

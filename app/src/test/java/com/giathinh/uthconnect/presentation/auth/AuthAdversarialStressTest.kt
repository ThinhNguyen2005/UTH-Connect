package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import com.giathinh.uthconnect.domain.validation.LoginValidator
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Adversarial Stress & Edge Case Test Harness for Authentication UI & State Machine.
 *
 * Authored by Challenger 1 to empirically stress-test:
 * 1. Rapid language toggling and state immutability
 * 2. Boundary and adversarial input validation (whitespace MSSV, 7/8/12/13 digits, SQL injection, domain spoofing)
 * 3. Form error clearance and self-healing password retry loops
 * 4. Language toggling and UI state persistence
 * 5. Double-submit and race condition resilience
 * 6. Accessibility touch target constants (>= 48dp)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthAdversarialStressTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val validStudent = StudentProfile(
        studentId = "2023040123",
        email = "2023040123@ut.edu.vn",
        displayName = "Nguyen Van Stress",
        className = "CN23A",
        major = "CNTT",
        faculty = "Khoa CNTT",
        role = UserRole.STUDENT,
        uthVerified = true,
    )

    private class CountingAuthRepository(
        private val student: StudentProfile,
        val authCallCount: AtomicInteger = AtomicInteger(0),
    ) : AuthRepository {
        var delayResponse: Boolean = false

        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> {
            authCallCount.incrementAndGet()
            val valid = (identifier.trim() == student.studentId || identifier.trim().equals(student.email, ignoreCase = true)) && password == "correct_password"
            return Result.success(student.takeIf { valid })
        }

        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = Result.success(student)
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = Result.success(true)
    }

    private class InMemorySessionRepository : SessionRepository {
        private val _session = MutableStateFlow(StudentSession(isRestoring = false))
        override val session: StateFlow<StudentSession> = _session

        override suspend fun restore() {
            _session.value = _session.value.copy(isRestoring = false)
        }

        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            _session.value = StudentSession(
                isRestoring = false,
                profile = profile,
                remembered = remember,
                biometricStudentId = if (remember) profile.studentId else null,
            )
        }

        override suspend fun updateProfile(profile: StudentProfile) {
            _session.value = _session.value.copy(profile = profile)
        }

        override suspend fun enableBiometric(studentId: String) {
            _session.value = _session.value.copy(biometricStudentId = studentId)
        }

        override suspend fun disableBiometric() {
            _session.value = _session.value.copy(biometricStudentId = null)
        }

        override suspend fun unlockWithBiometric() {
            val bioId = _session.value.biometricStudentId ?: return
            _session.value = _session.value.copy(
                profile = StudentProfile(
                    studentId = bioId,
                    email = "$bioId@ut.edu.vn",
                    displayName = "Sinh vien",
                    className = "CN23A",
                    major = "CNTT",
                    faculty = "Khoa CNTT",
                ),
            )
        }

        override suspend fun signOut() {
            _session.value = StudentSession(isRestoring = false, profile = null)
        }
    }

    private fun createStressHarness(
        auth: CountingAuthRepository = CountingAuthRepository(validStudent),
        session: InMemorySessionRepository = InMemorySessionRepository(),
    ): Triple<AuthViewModel, CountingAuthRepository, InMemorySessionRepository> {
        val signInUseCase = SignInUseCase(auth, session)
        val accessSessionUseCase = AccessStudentSessionUseCase(session)
        val signInWithGoogleUseCase = SignInWithGoogleUseCase(auth, session)
        val vm = AuthViewModel(signInUseCase, accessSessionUseCase, signInWithGoogleUseCase)
        return Triple(vm, auth, session)
    }

    // ───────────────────────────────────────── 1. RAPID LANGUAGE TOGGLING ─────────────────────────────────────────

    @Test
    fun `rapid language toggling 50 times preserves entered credentials and leaves state consistent`() = runTest {
        val (vm, _, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("secret_input"))
        vm.onAction(AuthAction.RememberChanged(true))

        for (i in 1..50) {
            val lang = if (i % 2 == 1) "EN" else "VI"
            vm.onAction(AuthAction.SelectLanguage(lang))
            assertEquals(lang, vm.uiState.value.selectedLanguage)
        }

        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals("VI", state.selectedLanguage)
        assertEquals("2023040123", state.identifier)
        assertEquals("secret_input", state.password)
        assertTrue(state.rememberMe)
        assertNull(state.failure)
        assertFalse(state.isSigningIn)
    }

    @Test
    fun `retyping password or switching account clears transient failure message`() = runTest {
        val (vm, _, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrong_pass"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)

        // Typing a new password immediately dismisses the failure banner
        vm.onAction(AuthAction.PasswordChanged("new_attempt"))
        assertNull(vm.uiState.value.failure)

        // Re-trigger failure and test SwitchAccount clearance
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)

        vm.onAction(AuthAction.SwitchAccount)
        assertNull(vm.uiState.value.failure)
        assertEquals("", vm.uiState.value.identifier)
        assertEquals("", vm.uiState.value.password)
    }

    // ───────────────────────────────────────── 2. ADVERSARIAL VALIDATION ─────────────────────────────────────────

    private val validator = LoginValidator()

    @Test
    fun `adversarial MSSV inputs - empty, whitespaces, boundary lengths, and injections`() {
        // Whitespace variations must yield Required, NOT crash or InvalidIdentifier
        assertEquals(LoginFieldError.Required, validator.identifierError(""))
        assertEquals(LoginFieldError.Required, validator.identifierError(" "))
        assertEquals(LoginFieldError.Required, validator.identifierError("   \t\n  "))

        // Boundary digit tests (Valid range is 8 to 12 digits)
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1234567")) // 7 digits (too short)
        assertNull(validator.identifierError("12345678")) // 8 digits (exact lower bound)
        assertNull(validator.identifierError("123456789")) // 9 digits
        assertNull(validator.identifierError("1234567890")) // 10 digits
        assertNull(validator.identifierError("12345678901")) // 11 digits
        assertNull(validator.identifierError("123456789012")) // 12 digits (exact upper bound)
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1234567890123")) // 13 digits (too long)
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("12345678901234567890")) // 20 digits

        // Whitespace padded valid MSSV should be accepted after trim
        assertNull(validator.identifierError("  2023040123  "))
        assertNull(validator.identifierError("\t2023040123\n"))

        // Alphanumeric, special characters, and code injections
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("20230401A"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("2023-040123"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("2023 040123"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("' OR 1=1 --"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("<script>alert('xss')</script>"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("null"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("undefined"))
    }

    @Test
    fun `adversarial Email inputs - subdomains, spoofing, and domain traversal`() {
        // Valid official student domain
        assertNull(validator.identifierError("sv2023@ut.edu.vn"))
        assertNull(validator.identifierError("nguyen.va23@ut.edu.vn"))
        assertNull(validator.identifierError("STUDENT.OFFICIAL@UT.EDU.VN")) // case insensitive

        // Domain spoofing attacks
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("attacker@ut.edu.vn.com"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("attacker@ut.edu.vn.evil.io"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("attacker@subdomain.ut.edu.vn"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("attacker@notut.edu.vn"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("attacker@fake-ut.edu.vn"))
        assertNull(validator.identifierError("attacker@uth.edu.vn")) // UTH university domain is accepted
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("@ut.edu.vn")) // empty local-part
    }

    @Test
    fun `adversarial Password inputs - blanks, whitespaces, and symbols`() {
        assertEquals(LoginFieldError.Required, validator.passwordError(""))
        assertEquals(LoginFieldError.Required, validator.passwordError("   "))
        assertEquals(LoginFieldError.Required, validator.passwordError("\t\n"))

        // Non-blank passwords with special chars and spaces should be allowed
        assertNull(validator.passwordError("p@ssw0rd!#$"))
        assertNull(validator.passwordError("pass phrase with spaces"))
        assertNull(validator.passwordError("   pass   ")) // Contains non-whitespace chars
    }

    // ───────────────────────────────────────── 3. PASSWORD RETRY CYCLE ─────────────────────────────────────────

    @Test
    fun `password retry cycle - typing immediately dismisses error and retry with correct password succeeds`() = runTest {
        val (vm, auth, _) = createStressHarness()
        advanceUntilIdle()

        // Step 1: Submit wrong credentials
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("bad_attempt_1"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        assertEquals(1, auth.authCallCount.get())
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)

        // Step 2: User starts typing new password -> error should immediately clear
        vm.onAction(AuthAction.PasswordChanged("bad_attempt_2"))
        assertNull(vm.uiState.value.failure)
        assertNull(vm.uiState.value.passwordError)

        // Step 3: User submits again with still incorrect password
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(2, auth.authCallCount.get())
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)

        // Step 4: User clicks Retry without modifying password -> repeats attempt
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()
        assertEquals(3, auth.authCallCount.get())
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)

        // Step 5: User enters correct password and retries -> Success
        vm.onAction(AuthAction.PasswordChanged("correct_password"))
        assertNull(vm.uiState.value.failure)
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()

        assertEquals(4, auth.authCallCount.get())
        assertNull(vm.uiState.value.failure)
        assertEquals("", vm.uiState.value.password) // Password wiped on success
        assertNotNull(vm.uiState.value.student)
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
    }

    // ───────────────────────────────────────── 4. LANGUAGE TOGGLING ─────────────────────────────────────────

    @Test
    fun `language toggling preserves UI state and inputs across multiple changes`() = runTest {
        val (vm, _, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("my_secret"))
        vm.onAction(AuthAction.RememberChanged(true))
        vm.onAction(AuthAction.TogglePassword) // Show password
        assertTrue(vm.uiState.value.passwordVisible)

        // Toggle back and forth
        vm.onAction(AuthAction.SelectLanguage("EN"))
        assertEquals("EN", vm.uiState.value.selectedLanguage)
        assertEquals("2023040123", vm.uiState.value.identifier)
        assertEquals("my_secret", vm.uiState.value.password)
        assertTrue(vm.uiState.value.rememberMe)
        assertTrue(vm.uiState.value.passwordVisible)

        vm.onAction(AuthAction.SelectLanguage("VI"))
        assertEquals("VI", vm.uiState.value.selectedLanguage)
        assertEquals("2023040123", vm.uiState.value.identifier)
        assertEquals("my_secret", vm.uiState.value.password)
        assertTrue(vm.uiState.value.rememberMe)
        assertTrue(vm.uiState.value.passwordVisible)
    }

    // ───────────────────────────────────────── 5. DOUBLE SUBMIT & RACE CONDITION RESILIENCE ─────────────────────────────────────────

    @Test
    fun `double submit while isSigningIn drops duplicate action`() = runTest {
        val (vm, auth, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("correct_password"))

        // First submit triggers sign-in
        vm.onAction(AuthAction.Submit)
        // Second submit before advanceUntilIdle (while in flight or concurrently)
        vm.onAction(AuthAction.Submit)
        vm.onAction(AuthAction.Retry)

        advanceUntilIdle()

        // Auth repository should only have been called once because isSigningIn blocks re-entry
        assertEquals(1, auth.authCallCount.get())
        assertNotNull(vm.uiState.value.student)
    }

    @Test
    fun `submit when already logged in is a no-op`() = runTest {
        val (vm, auth, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("correct_password"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        assertEquals(1, auth.authCallCount.get())
        assertNotNull(vm.uiState.value.student)

        // Additional submits after login succeeded must be ignored
        vm.onAction(AuthAction.Submit)
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()

        assertEquals(1, auth.authCallCount.get())
    }

    // ───────────────────────────────────────── 6. BIOMETRIC STATE TRANSITIONS & RESET ─────────────────────────────────────────

    @Test
    fun `switch account reset lifecycle preserves clean password step`() = runTest {
        val (vm, _, _) = createStressHarness()
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("temporary_pass"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)

        // SwitchAccount resets state to pristine entry point
        vm.onAction(AuthAction.SwitchAccount)
        advanceUntilIdle()

        assertEquals(AuthScreenStep.Password, vm.uiState.value.screenStep)
        assertEquals("", vm.uiState.value.identifier)
        assertEquals("", vm.uiState.value.password)
        assertNull(vm.uiState.value.failure)
        assertFalse(vm.uiState.value.isSigningIn)
    }

    // ───────────────────────────────────────── 7. ACCESSIBILITY CONTRACT (MIN 48DP) ─────────────────────────────────────────

    @Test
    fun `strict accessibility compliance - all interactive dimensions are at least 48dp`() {
        assertTrue("MinTouchTarget must be >= 48dp", UthDimens.Size.MinTouchTarget.value >= 48f)
        assertTrue("ButtonHeight must be >= 48dp", UthDimens.Size.ButtonHeight.value >= 48f)
        assertTrue("SegmentedControlHeight must be >= 48dp", UthDimens.Size.SegmentedControlHeight.value >= 48f)
        assertTrue("BrandLogoMonogram must be >= 48dp", UthDimens.Size.BrandLogoMonogram.value >= 48f)
        assertTrue("LanguagePillWidth must be >= 48dp", UthDimens.Size.LanguagePillWidth.value >= 48f)
    }
}

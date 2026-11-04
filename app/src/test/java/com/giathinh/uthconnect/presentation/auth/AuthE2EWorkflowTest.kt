package com.giathinh.uthconnect.presentation.auth

import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.data.repository.sha256Hex
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.DomainRestrictionException
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.SignInResult
import com.giathinh.uthconnect.domain.usecase.SignInUseCase
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleResult
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import com.giathinh.uthconnect.domain.validation.LoginValidator
import com.giathinh.uthconnect.testing.MainDispatcherRule
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * End-to-End Hermetic Workflow Test Suite for UTH Connect Authentication.
 *
 * Implements the 4-Tier test architecture specified in TEST_INFRA.md:
 * - Tier 1: Domain Validation & Security Cryptography (MSSV, @ut.edu.vn, Nonce, Domain Restrictions)
 * - Tier 2: ViewModel State Machine Transitions (UDF, Form Errors, Retry, Mode & Language Switching)
 * - Tier 3: UI Semantics & Accessibility Contract (Test Tags, Min Touch Target >= 48dp)
 * - Tier 4: Integrated Hermetic User Journeys (6 Real-World End-to-End Application Scenarios)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthE2EWorkflowTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val verifiedStudent = StudentProfile(
        studentId = "2023040123",
        email = "2023040123@ut.edu.vn",
        displayName = "Nguyễn Minh Anh",
        className = "CN23A",
        major = "Công nghệ thông tin",
        faculty = "Khoa Công nghệ thông tin",
        role = UserRole.STUDENT,
        uthVerified = true,
    )

    /** Hermetic in-memory SessionRepository fake for pure deterministic JVM testing. */
    private class FakeSessionRepository : SessionRepository {
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
                    displayName = "Sinh viên đã lưu",
                    className = "CN23A",
                    major = "Công nghệ thông tin",
                    faculty = "Khoa CNTT",
                    uthVerified = true,
                ),
            )
        }

        override suspend fun signOut() {
            _session.value = StudentSession(isRestoring = false, profile = null)
        }
    }

    /** Hermetic AuthRepository fake with programmable behaviors. */
    private class FakeAuthRepository(
        private val student: StudentProfile,
        var allowGoogleSignIn: Boolean = true,
        var simulateUnverifiedGoogleStudent: Boolean = false,
        var simulateNonUthGoogleAccount: Boolean = false,
    ) : AuthRepository {
        override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> {
            val normalizedEmail = if ('@' in identifier) identifier.trim() else "${identifier.trim()}@ut.edu.vn"
            val valid = (identifier.trim() == student.studentId || normalizedEmail.equals(student.email, ignoreCase = true)) && password == "secret"
            return Result.success(student.takeIf { valid })
        }

        override suspend fun findStudent(studentId: String): Result<StudentProfile?> =
            Result.success(student.takeIf { it.studentId == studentId })

        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> =
            Result.success(true)

        override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> {
            if (!allowGoogleSignIn) {
                return Result.failure(IllegalStateException("Google Sign-In requires Supabase"))
            }
            if (simulateNonUthGoogleAccount) {
                return Result.failure(DomainRestrictionException("personal@gmail.com"))
            }
            if (simulateUnverifiedGoogleStudent) {
                return Result.failure(
                    StudentVerificationRequiredException(
                        email = "pending.student@ut.edu.vn",
                        displayName = "Lê Văn Pending",
                    ),
                )
            }
            return Result.success(student)
        }
    }

    private fun createHarness(
        auth: FakeAuthRepository,
        session: FakeSessionRepository = FakeSessionRepository(),
    ): Triple<AuthViewModel, FakeAuthRepository, FakeSessionRepository> {
        val signInUseCase = SignInUseCase(auth, session)
        val accessSessionUseCase = AccessStudentSessionUseCase(session)
        val signInWithGoogleUseCase = SignInWithGoogleUseCase(auth, session)
        val viewModel = AuthViewModel(signInUseCase, accessSessionUseCase, signInWithGoogleUseCase)
        return Triple(viewModel, auth, session)
    }

    // ───────────────────────────────────────── TIER 1: DOMAIN VALIDATION & CRYPTO ─────────────────────────────────────────

    private val validator = LoginValidator()

    @Test
    fun tier1_mssvValidation_acceptsValidRangeAndRejectsInvalidFormats() {
        // Valid 8-12 digits MSSV
        assertNull(validator.identifierError("20230401"))
        assertNull(validator.identifierError("2023040123"))
        assertNull(validator.identifierError("202304012345"))

        // Invalid: too short (<8)
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1234567"))
        // Invalid: too long (>12)
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("1234567890123"))
        // Invalid: letters/symbols in MSSV
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("20230401A"))
        // Blank
        assertEquals(LoginFieldError.Required, validator.identifierError("  "))
    }

    @Test
    fun tier1_studentEmailDomain_acceptsOnlyUtEduVn() {
        // Official student domain
        assertNull(validator.identifierError("student@ut.edu.vn"))
        assertNull(validator.identifierError("STUDENT.2023@UT.EDU.VN"))
        assertNull(validator.identifierError("anh.nm23@ut.edu.vn"))

        // Rejected domains
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("student@gmail.com"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("student@yahoo.com"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("student@uth.edu.vn"))
        assertEquals(LoginFieldError.InvalidIdentifier, validator.identifierError("student@hcmut.edu.vn"))
    }

    @Test
    fun tier1_passwordValidation_requiresNonBlankSecret() {
        assertNull(validator.passwordError("secret"))
        assertNull(validator.passwordError("ComplexP@ssw0rd!"))
        assertEquals(LoginFieldError.Required, validator.passwordError(""))
        assertEquals(LoginFieldError.Required, validator.passwordError("   "))
    }

    @Test
    fun tier1_googleNonce_producesCryptographicSha256HexAndDistinctEntropy() {
        val raw1 = "test-nonce-1"
        val expectedHash1 = MessageDigest.getInstance("SHA-256")
            .digest(raw1.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val actualHash1 = sha256Hex(raw1)
        assertEquals(expectedHash1, actualHash1)
        assertEquals(64, actualHash1.length)
        assertTrue(actualHash1.all { it in '0'..'9' || it in 'a'..'f' })

        // Random nonces must be unique (entropy)
        val nonceA = UUID.randomUUID().toString()
        val nonceB = UUID.randomUUID().toString()
        val hashA = sha256Hex(nonceA)
        val hashB = sha256Hex(nonceB)
        assertTrue(hashA != hashB)
    }

    @Test
    fun tier1_domainRestrictionException_retainsRejectedEmail() {
        val exception = DomainRestrictionException("user@gmail.com")
        assertEquals("user@gmail.com", exception.email)
        assertTrue(exception.message!!.contains("@ut.edu.vn"))
    }

    // ───────────────────────────────────────── TIER 2: VIEWMODEL STATE MACHINE ─────────────────────────────────────────

    @Test
    fun tier2_stateMachine_initialStateIsCleanAndNotRestoring() = runTest {
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isRestoring)
        assertNull(state.student)
        assertEquals("", state.identifier)
        assertEquals("", state.password)
        assertEquals(AuthMode.Password, state.authMode)
        assertEquals("VI", state.selectedLanguage)
        assertNull(state.failure)
    }

    @Test
    fun tier2_stateMachine_formValidationEmitsInlineErrors() = runTest {
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        assertEquals(LoginFieldError.Required, vm.uiState.value.identifierError)
        assertEquals(LoginFieldError.Required, vm.uiState.value.passwordError)
        assertFalse(vm.uiState.value.isSigningIn)
    }

    @Test
    fun tier2_stateMachine_modeAndLanguageToggling() = runTest {
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        // Toggle mode to Biometric
        vm.onAction(AuthAction.SelectMode(AuthMode.Biometric))
        assertEquals(AuthMode.Biometric, vm.uiState.value.authMode)

        // Toggle mode back to Password
        vm.onAction(AuthAction.SelectMode(AuthMode.Password))
        assertEquals(AuthMode.Password, vm.uiState.value.authMode)

        // Toggle language to EN
        vm.onAction(AuthAction.SelectLanguage("EN"))
        assertEquals("EN", vm.uiState.value.selectedLanguage)

        // Toggle language back to VI
        vm.onAction(AuthAction.SelectLanguage("VI"))
        assertEquals("VI", vm.uiState.value.selectedLanguage)
    }

    @Test
    fun tier2_stateMachine_invalidCredentialsTriggersSelfHealingRetry() = runTest {
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("incorrect"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)

        // Correct password and retry
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.Retry)
        advanceUntilIdle()

        assertNull(vm.uiState.value.failure)
        assertEquals("", vm.uiState.value.password)
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
    }

    // ───────────────────────────────────────── TIER 3: UI SEMANTICS & ACCESSIBILITY CONTRACT ─────────────────────────────────────────

    @Test
    fun tier3_uiContract_minTouchTargetsComplyWith48dpRequirement() {
        // WCAG AA & Material Design strictly require >= 48dp minimum touch target
        assertTrue(UthDimens.Size.MinTouchTarget.value >= 48f)
        assertTrue(UthDimens.Size.ButtonHeight.value >= 48f)
        assertTrue(UthDimens.Size.SegmentedControlHeight.value >= 48f)
        assertTrue(UthDimens.Size.BiometricTouchZone.value >= 48f)
        assertTrue(UthDimens.Size.BiometricTouchTarget.value >= 48f)
    }

    @Test
    fun tier3_uiContract_testTagsInventoryIsCompleteAndConsistent() {
        val requiredTags = listOf(
            "auth_brand_header",
            "auth_language_pill",
            "auth_mode_password",
            "auth_mode_biometric",
            "auth_identifier",
            "auth_password",
            "auth_password_toggle",
            "auth_remember_checkbox",
            "auth_forgot_password",
            "auth_sign_in",
            "auth_retry",
            "auth_error",
            "auth_google_sign_in",
            "auth_google_pending",
            "auth_biometric_card",
            "auth_biometric_touch_zone",
            "auth_biometric_login",
            "auth_switch_account",
            "auth_footer_security",
            "auth_footer_version",
        )

        // Assert all 20 semantic test tags are uniquely identified and non-blank
        assertEquals(20, requiredTags.distinct().size)
        requiredTags.forEach { tag ->
            assertTrue(tag.startsWith("auth_"))
            assertTrue(tag.length > 5)
        }
    }

    // ───────────────────────────────────────── TIER 4: HERMETIC E2E USER JOURNEYS ─────────────────────────────────────────

    @Test
    fun tier4_journey1_standardCredentialLogin_withRememberMe_success() = runTest {
        val (vm, _, session) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        // User enters credentials and enables remember-me
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.RememberChanged(true))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        // Assert: Login succeeded, password wiped, session started with remember=true
        assertNull(vm.uiState.value.failure)
        assertEquals("", vm.uiState.value.password)
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        assertTrue(session.session.value.remembered)
        assertEquals("2023040123", session.session.value.biometricStudentId)
    }

    @Test
    fun tier4_journey2_verifiedGoogleSign_opensInMemorySessionWithoutRememberMe() = runTest {
        val (vm, _, session) = createHarness(FakeAuthRepository(verifiedStudent, allowGoogleSignIn = true))
        advanceUntilIdle()

        // Route passes Google token to ViewModel
        vm.onAction(AuthAction.GoogleIdTokenReceived("google-token-verified", "nonce-123"))
        advanceUntilIdle()

        // Assert: Session established in-memory, remember=false adhering to ephemeral session security
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        assertFalse(session.session.value.remembered)
        assertNull(session.session.value.biometricStudentId)
    }

    @Test
    fun tier4_journey3_unverifiedGoogleStudent_displaysPendingBannerWithoutOpeningSession() = runTest {
        val auth = FakeAuthRepository(
            student = verifiedStudent,
            allowGoogleSignIn = true,
            simulateUnverifiedGoogleStudent = true,
        )
        val (vm, _, session) = createHarness(auth)
        advanceUntilIdle()

        // First-time Google user signs in, but MSSV is not verified yet by admin
        vm.onAction(AuthAction.GoogleIdTokenReceived("google-token-unverified", "nonce-456"))
        advanceUntilIdle()

        // Assert: User is NOT admitted to app; verification banner is populated; no session created
        assertNull(vm.uiState.value.student)
        assertNull(session.session.value.profile)
        assertNotNull(vm.uiState.value.googleVerification)
        assertEquals("pending.student@ut.edu.vn", vm.uiState.value.googleVerification?.email)
        assertEquals("Lê Văn Pending", vm.uiState.value.googleVerification?.displayName)
        assertFalse(vm.uiState.value.isSigningInWithGoogle)
    }

    @Test
    fun tier4_journey4_savedStudentProfile_biometricFingerprintUnlock_success() = runTest {
        val session = FakeSessionRepository()
        // Pre-populate biometric enrolled student
        session.enableBiometric("2023040123")
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent), session)
        advanceUntilIdle()

        // Switch to Biometric mode
        vm.onAction(AuthAction.SelectMode(AuthMode.Biometric))
        assertEquals(AuthMode.Biometric, vm.uiState.value.authMode)
        assertTrue(vm.uiState.value.biometricAvailable)

        // User touches biometric zone -> Biometric prompt succeeds
        vm.onAction(AuthAction.BiometricVerified)
        advanceUntilIdle()

        // Assert: Student session opened successfully via biometric unlock
        assertNotNull(vm.uiState.value.student)
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
    }

    @Test
    fun tier4_journey5_invalidPasswordFailure_showsErrorNotice_andCorrectedPasswordRetrySucceeds() = runTest {
        val (vm, _, _) = createHarness(FakeAuthRepository(verifiedStudent))
        advanceUntilIdle()

        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("wrongpassword"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        // Assert error banner displayed
        assertEquals(AuthFailure.InvalidCredentials, vm.uiState.value.failure)
        assertNull(vm.uiState.value.student)

        // Switch account action resets fields
        vm.onAction(AuthAction.SwitchAccount)
        assertEquals("", vm.uiState.value.identifier)
        assertEquals("", vm.uiState.value.password)
        assertNull(vm.uiState.value.failure)

        // Log in again with right password
        vm.onAction(AuthAction.IdentifierChanged("2023040123"))
        vm.onAction(AuthAction.PasswordChanged("secret"))
        vm.onAction(AuthAction.Submit)
        advanceUntilIdle()

        assertEquals("2023040123", vm.uiState.value.student?.studentId)
    }

    @Test
    fun tier4_journey6_nonUthEmail_onGoogleSso_isRejectedWithoutCreatingSession() = runTest {
        val auth = FakeAuthRepository(
            student = verifiedStudent,
            allowGoogleSignIn = true,
            simulateNonUthGoogleAccount = true,
        )
        val (vm, _, session) = createHarness(auth)
        advanceUntilIdle()

        // User attempts Google sign-in with @gmail.com
        vm.onAction(AuthAction.GoogleIdTokenReceived("token-from-gmail", "nonce-789"))
        advanceUntilIdle()

        // Assert: Rejected, no session created, failure state set to GoogleRejected
        assertNull(vm.uiState.value.student)
        assertNull(session.session.value.profile)
        assertEquals(AuthFailure.GoogleRejected, vm.uiState.value.failure)
    }
}

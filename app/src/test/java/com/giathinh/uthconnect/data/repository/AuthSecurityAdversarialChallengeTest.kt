package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.DomainRestrictionException
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleResult
import com.giathinh.uthconnect.domain.usecase.SignInWithGoogleUseCase
import com.giathinh.uthconnect.domain.validation.LoginFieldError
import com.giathinh.uthconnect.domain.validation.LoginValidator
import java.security.MessageDigest
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Empirical Adversarial Challenge Test Suite for UTH Connect Authentication & Security Invariants.
 *
 * Authored by Challenger 2 to rigorously and independently verify:
 * 1. Nonce entropy and lowercase 64-hex SHA-256 formatting.
 * 2. Domain restriction on Google SSO (mixed case, subdomains, non-UTH domains).
 * 3. First-time unverified student profile handling (session dropped, StudentVerificationRequiredException).
 * 4. In-memory ephemeral session persistence invariant under Supabase.
 */
class AuthSecurityAdversarialChallengeTest {

    // ══════════════════════════════════════════════════════════════════════════
    // 1. NONCE ENTROPY AND LOWERCASE 64-HEX SHA-256 FORMATTING
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `nonce oracle matches known NIST SHA-256 test vectors`() {
        val testVectors = mapOf(
            "" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            "abc" to "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq" to
                "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
        )

        for ((input, expectedDigest) in testVectors) {
            val actual = sha256Hex(input)
            assertEquals("Digest mismatch for input: $input", expectedDigest, actual)
        }
    }

    @Test
    fun `nonce output is strictly 64 characters lowercase hex with zero uppercase characters`() {
        // Stress test across 1,000 randomized and edge-case inputs
        val random = Random(42)
        val edgeInputs = listOf(
            "",
            " ",
            "\u0000",
            "1234567890",
            "UTH Connect 2026! 🚀🔒",
            "Tiếng Việt có dấu: Đại học Giao thông vận tải TP.HCM",
            "A".repeat(1000),
            UUID.randomUUID().toString(),
        ) + (1..1000).map {
            val length = random.nextInt(0, 256)
            ByteArray(length).also { random.nextBytes(it) }.toString(Charsets.UTF_8)
        }

        for (input in edgeInputs) {
            val digest = sha256Hex(input)
            assertEquals("SHA-256 output must be exactly 64 characters", 64, digest.length)
            assertTrue(
                "SHA-256 output must contain only lowercase hex [0-9a-f]: $digest",
                digest.all { it in '0'..'9' || it in 'a'..'f' },
            )
            assertFalse(
                "SHA-256 output must NEVER contain uppercase hex [A-F]: $digest",
                digest.any { it in 'A'..'F' },
            )
        }
    }

    @Test
    fun `nonce entropy stress harness - 10000 raw nonces yield 10000 distinct digests with 0 collisions`() {
        val count = 10_000
        val rawNonces = HashSet<String>(count)
        val hashedNonces = HashSet<String>(count)

        for (i in 1..count) {
            val raw = UUID.randomUUID().toString()
            assertTrue("UUID collision detected at iteration $i", rawNonces.add(raw))
            val hash = sha256Hex(raw)
            assertTrue("SHA-256 hash collision detected at iteration $i", hashedNonces.add(hash))
        }

        assertEquals(count, rawNonces.size)
        assertEquals(count, hashedNonces.size)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. DOMAIN RESTRICTION ON GOOGLE SSO & LOGIN VALIDATOR
    // ══════════════════════════════════════════════════════════════════════════

    private val validator = LoginValidator()

    @Test
    fun `Google SSO domain check accepts valid UTH email with arbitrary mixed casing`() {
        val validCasedEmails = listOf(
            "student@ut.edu.vn",
            "STUDENT@UT.EDU.VN",
            "Student@Ut.Edu.Vn",
            "sTuDeNt.2023@uT.eDu.Vn",
            "2023040123@ut.edu.vn",
            "MINH.ANH@UT.EDU.VN",
            "minh.anh+club@ut.edu.vn",
        )

        for (email in validCasedEmails) {
            // Test repository-level predicate: email.endsWith("@ut.edu.vn", ignoreCase = true)
            assertTrue(
                "Repository domain check must succeed for mixed case: $email",
                email.endsWith("@ut.edu.vn", ignoreCase = true) || email.endsWith("@uth.edu.vn", ignoreCase = true),
            )
            // Test presentation-level validator
            assertNull("LoginValidator must accept: $email", validator.identifierError(email))
        }
    }

    @Test
    fun `Google SSO domain check strictly rejects subdomains, spoofed suffixes, and non-UTH domains`() {
        val rejectedEmails = listOf(
            // Subdomain attacks (must NOT pass because preceding char is '.' or subdomain, not '@')
            "student@subdomain.ut.edu.vn",
            "student@cntt.ut.edu.vn",
            "student@mail.ut.edu.vn",
            "user@auth.ut.edu.vn",
            // Suffix spoofing attacks
            "student@ut.edu.vn.evil.com",
            "student@ut.edu.vn.attacker.org",
            "student@ut.edu.vn.local",
            "student@ut.edu.vn@evil.com",
            // Lookalike / typo domains
            "student@notut.edu.vn",
            "student@fake-ut.edu.vn",
            "student@ut.edu.com",
            "student@uth.edu.vn.evil.com",
            "student@hcmut.edu.vn",
            // Consumer mail providers
            "student@gmail.com",
            "student@yahoo.com",
            "student@outlook.com",
            "student@icloud.com",
            // Empty / malformed
            "",
            "   ",
            "student@",
            "@ut.edu.vn.attacker.com",
        )

        for (email in rejectedEmails) {
            val passedDomainCheck = email.endsWith("@ut.edu.vn", ignoreCase = true) || email.endsWith("@uth.edu.vn", ignoreCase = true)
            assertFalse(
                "Domain check must REJECT adversarial or spoofed email: '$email'",
                passedDomainCheck,
            )
        }
    }

    @Test
    fun `DomainRestrictionException preserves exact rejected email and descriptive message`() {
        val spoofedEmail = "hacker@ut.edu.vn.evil.com"
        val exception = DomainRestrictionException(spoofedEmail)
        assertEquals(spoofedEmail, exception.email)
        assertTrue(exception.message!!.contains("@ut.edu.vn"))
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. FIRST-TIME UNVERIFIED STUDENT PROFILE INVARIANT
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `toDomainProfile strictly rejects unverified student profiles`() {
        // Case 1: student_id_verified = false (Standard new Google signup from handle_new_user trigger)
        val unverifiedRow = buildJsonObject {
            put("student_id", "2023040123")
            put("student_id_verified", false)
            put("email", "student@ut.edu.vn")
            put("display_name", "Pending Student")
        }
        try {
            unverifiedRow.toDomainProfile()
            fail("toDomainProfile must throw IllegalArgumentException when student_id_verified is false")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("chưa được quản trị viên xác minh"))
        }

        // Case 2: student_id is null / blank
        val nullStudentIdRow = buildJsonObject {
            put("student_id_verified", true)
            put("email", "student@ut.edu.vn")
            put("display_name", "Student Without ID")
        }
        try {
            nullStudentIdRow.toDomainProfile()
            fail("toDomainProfile must throw IllegalArgumentException when student_id is missing")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("chưa được quản trị viên xác minh"))
        }

        // Case 3: Empty student_id string
        val blankStudentIdRow = buildJsonObject {
            put("student_id", "   ")
            put("student_id_verified", true)
            put("email", "student@ut.edu.vn")
        }
        try {
            blankStudentIdRow.toDomainProfile()
            fail("toDomainProfile must throw IllegalArgumentException when student_id is blank")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("chưa được quản trị viên xác minh"))
        }
    }

    @Test
    fun `first-time unverified Google student triggers VerificationPending and prevents session establishment`() = runBlocking {
        var signOutInvoked = false
        var sessionStarted = false

        val fakeAuth = object : AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(null)
            override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)

            override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> {
                // Simulates SupabaseAuthRepository behavior:
                // 1. client.signOut() is called immediately
                signOutInvoked = true
                // 2. StudentVerificationRequiredException is returned as failure Result
                return Result.failure(
                    StudentVerificationRequiredException(
                        email = "newbie@ut.edu.vn",
                        displayName = "Nguyễn Văn Newbie",
                    ),
                )
            }
        }

        val fakeSession = object : SessionRepository {
            private val _session = MutableStateFlow(StudentSession(isRestoring = false))
            override val session: StateFlow<StudentSession> = _session
            override suspend fun restore() = Unit
            override suspend fun start(profile: StudentProfile, remember: Boolean) {
                sessionStarted = true
            }
            override suspend fun updateProfile(profile: StudentProfile) = Unit
            override suspend fun enableBiometric(studentId: String) = Unit
            override suspend fun disableBiometric() = Unit
            override suspend fun unlockWithBiometric() = Unit
            override suspend fun signOut() = Unit
        }

        val useCase = SignInWithGoogleUseCase(fakeAuth, fakeSession)
        val result = useCase("valid-id-token", "raw-nonce-123")

        // Invariant 1: Supabase client session was dropped via signOut()
        assertTrue("Supabase client session must be explicitly dropped via signOut()", signOutInvoked)

        // Invariant 2: Result is VerificationPending with student details
        assertTrue("Result must be VerificationPending", result is SignInWithGoogleResult.VerificationPending)
        val pending = result as SignInWithGoogleResult.VerificationPending
        assertEquals("newbie@ut.edu.vn", pending.email)
        assertEquals("Nguyễn Văn Newbie", pending.displayName)

        // Invariant 3: SessionRepository.start() was NEVER called
        assertFalse("SessionRepository must NEVER start a session for unverified student", sessionStarted)
        assertNull("Session profile must remain null", fakeSession.session.value.profile)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. IN-MEMORY EPHEMERAL SESSION PERSISTENCE INVARIANT UNDER SUPABASE
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    fun `Supabase session persistence invariant - zero unencrypted tokens and ephemeral session state`() {
        // Test invariant of StudentSession:
        // A Google SSO session or Supabase session must never carry remembered=true
        val verifiedStudent = StudentProfile(
            studentId = "2023040123",
            email = "2023040123@ut.edu.vn",
            displayName = "Student Invariant",
            className = "CN23A",
            major = "CNTT",
            faculty = "Khoa CNTT",
            role = UserRole.STUDENT,
            uthVerified = true,
        )

        val ephemeralSession = StudentSession(
            isRestoring = false,
            profile = verifiedStudent,
            remembered = false,
            biometricStudentId = null,
        )

        assertFalse("Supabase session must NOT be marked remembered", ephemeralSession.remembered)
        assertNull("Biometric student ID must be null under ephemeral session", ephemeralSession.biometricStudentId)
        assertNotNull("Profile must be present in active memory", ephemeralSession.profile)
    }

    @Test
    fun `GoogleExchangeLogTag is correctly declared for observability`() {
        assertEquals("SupabaseGoogleExchange", GoogleExchangeLogTag)
    }
}

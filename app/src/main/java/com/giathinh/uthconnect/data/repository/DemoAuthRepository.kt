package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Two isolated demo accounts, without any UTH identity/role verification server. */
@Singleton
class DemoAuthRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AuthRepository, StudentContactRepository {
    private val prefs = context.getSharedPreferences("uth_auth_demo_account", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private val student = StudentProfile(
        studentId = "2023040123",
        email = "demo@ut.edu.vn",
        displayName = "Nguyễn Minh Anh",
        className = "CN23A",
        major = "Công nghệ thông tin",
        faculty = "Khoa Công nghệ thông tin",
        // This is simulated verified status for UI/testing ONLY; not an official UTH verification.
        uthVerified = true,
    )
    private val organizer = StudentProfile(
        studentId = "2023040678",
        email = "organizer@ut.edu.vn",
        displayName = "Trần Gia Bảo",
        className = "CN23B",
        major = "Công nghệ thông tin",
        faculty = "Khoa Công nghệ thông tin",
        role = UserRole.ORGANIZER,
        uthVerified = true,
    )

    private fun baseFor(id: String): StudentProfile? = when (id) {
        student.studentId -> student
        organizer.studentId -> organizer
        else -> null
    }

    /** Keep original V2 student preference keys unchanged for backwards compatibility. */
    private fun key(base: StudentProfile, field: String): String =
        if (base.studentId == student.studentId) field else "organizer_$field"

    private fun profile(base: StudentProfile): StudentProfile = base.copy(
        phoneNumber = prefs.getString(key(base, "phone"), "").orEmpty(),
        personalEmail = prefs.getString(key(base, "personal_email"), "").orEmpty(),
        currentAddress = prefs.getString(key(base, "address"), "").orEmpty(),
    )

    private fun passwordMatches(base: StudentProfile, candidate: String): Boolean {
        val encodedSalt = prefs.getString(key(base, "password_salt"), null)
        val encodedHash = prefs.getString(key(base, "password_hash"), null)
        if (encodedSalt == null || encodedHash == null) {
            val initial = if (base.studentId == student.studentId) STUDENT_DEMO_PASSWORD else ORGANIZER_DEMO_PASSWORD
            return MessageDigest.isEqual(candidate.toByteArray(Charsets.UTF_8), initial.toByteArray(Charsets.UTF_8))
        }
        val salt = Base64.getDecoder().decode(encodedSalt)
        val expected = Base64.getDecoder().decode(encodedHash)
        return MessageDigest.isEqual(expected, derivePassword(candidate, salt))
    }

    private fun derivePassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 180_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> =
        withContext(Dispatchers.Default) {
            runCatching {
                mutex.withLock {
                    val base = listOf(student, organizer).firstOrNull {
                        identifier == it.studentId || identifier.equals(it.email, ignoreCase = true)
                    }
                    base?.takeIf { passwordMatches(it, password) }?.let(::profile)
                }
            }
        }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = runCatching {
        mutex.withLock { baseFor(studentId)?.let(::profile) }
    }

    override suspend fun changePassword(
        studentId: String, oldPassword: String, newPassword: String,
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            mutex.withLock {
                val base = baseFor(studentId) ?: return@withLock false
                if (!passwordMatches(base, oldPassword)) return@withLock false
                val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
                val hash = derivePassword(newPassword, salt)
                check(prefs.edit()
                    .putString(key(base, "password_salt"), Base64.getEncoder().encodeToString(salt))
                    .putString(key(base, "password_hash"), Base64.getEncoder().encodeToString(hash))
                    .commit()) { "Password storage failed" }
                true
            }
        }
    }

    override suspend fun updateContact(
        studentId: String, phone: String, personalEmail: String, currentAddress: String,
    ): Result<StudentProfile> = runCatching {
        mutex.withLock {
            val base = requireNotNull(baseFor(studentId)) { "Unknown student" }
            check(prefs.edit()
                .putString(key(base, "phone"), phone)
                .putString(key(base, "personal_email"), personalEmail)
                .putString(key(base, "address"), currentAddress)
                .commit()) { "Contact storage failed" }
            profile(base)
        }
    }

    companion object {
        const val STUDENT_DEMO_PASSWORD = "UTH@123456"
        const val ORGANIZER_DEMO_PASSWORD = "UTH@Org123456"
        /** Backwards compatibility for V2 demos referencing the original constant. */
        const val DEMO_PASSWORD = STUDENT_DEMO_PASSWORD
    }
}

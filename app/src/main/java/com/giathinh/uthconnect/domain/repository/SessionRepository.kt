package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val session: StateFlow<StudentSession>
    suspend fun restore()
    suspend fun start(profile: StudentProfile, remember: Boolean)
    suspend fun updateProfile(profile: StudentProfile)
    /** Only invoke after Android BiometricPrompt returned successful authentication. */
    suspend fun enableBiometric(studentId: String)
    suspend fun disableBiometric()
    /** Only invoke after Android BiometricPrompt returned successful authentication. */
    suspend fun unlockWithBiometric()
    /** Removes both remembered and biometric sign-in grants for this device. */
    suspend fun signOut()
}

data class StudentSession(
    val isRestoring: Boolean = true,
    val profile: StudentProfile? = null,
    val remembered: Boolean = false,
    val failure: SessionFailure? = null,
    /** A pending biometric credential; does NOT mean the user is authenticated. */
    val biometricStudentId: String? = null,
)

enum class SessionFailure { RestoreFailed, WriteFailed }

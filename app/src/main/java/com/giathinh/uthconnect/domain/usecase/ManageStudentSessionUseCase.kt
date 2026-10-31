package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Authenticated profile-side session management. Native biometric verification is in Route. */
class ManageStudentSessionUseCase @Inject constructor(
    private val sessions: SessionRepository,
) {
    fun observeBiometricEnabled(): Flow<Boolean> = sessions.session
        .map { it.profile != null && it.biometricStudentId != null }
        .distinctUntilChanged()

    suspend fun enableBiometricAfterPrompt() {
        val studentId = sessions.session.value.profile?.studentId ?: error("No signed-in student")
        sessions.enableBiometric(studentId)
    }
    suspend fun disableBiometric() = sessions.disableBiometric()
    suspend fun signOut() = sessions.signOut()
}

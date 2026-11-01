package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Shared session operations, so AuthViewModel never talks to a repository directly. */
class AccessStudentSessionUseCase @Inject constructor(
    private val sessions: SessionRepository,
) {
    fun observe(): StateFlow<StudentSession> = sessions.session
    suspend fun restore() = sessions.restore()
    /** Caller must first receive a successful Android BiometricPrompt response. */
    suspend fun unlockAfterBiometricPrompt() = sessions.unlockWithBiometric()
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Shared session operations, so AuthViewModel never talks to a repository directly. */
class AccessStudentSessionUseCase @Inject constructor(
    private val sessions: SessionRepository,
) {
    fun observe(): StateFlow<StudentSession> = sessions.session
    suspend fun restore() = sessions.restore()
}

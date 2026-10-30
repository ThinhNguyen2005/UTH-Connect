package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.flow.StateFlow

/** Session tách khỏi giao diện. Không lưu mật khẩu vào bộ nhớ lâu dài. */
interface SessionRepository {
    val session: StateFlow<StudentSession>
    suspend fun restore()
    suspend fun start(profile: StudentProfile, remember: Boolean)
    suspend fun signOut()
}

data class StudentSession(
    val isRestoring: Boolean = true,
    val profile: StudentProfile? = null,
    val remembered: Boolean = false,
    val failure: SessionFailure? = null,
)

enum class SessionFailure { RestoreFailed, WriteFailed }

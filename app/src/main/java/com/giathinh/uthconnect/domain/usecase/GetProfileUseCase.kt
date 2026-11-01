package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Profile lấy từ phiên hiện có, không dùng một người dùng mẫu cố định ở UI. */
class GetProfileUseCase @Inject constructor(private val session: SessionRepository) {
    fun observe(): Flow<StudentProfile?> = session.session.map { it.profile }
}

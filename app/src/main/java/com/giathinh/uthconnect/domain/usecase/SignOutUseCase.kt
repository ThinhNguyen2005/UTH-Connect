package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.SessionRepository
import javax.inject.Inject

class SignOutUseCase @Inject constructor(private val sessions: SessionRepository) {
    suspend operator fun invoke() = sessions.signOut()
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject

class SendVerificationUseCase @Inject constructor(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.sendVerification()
}

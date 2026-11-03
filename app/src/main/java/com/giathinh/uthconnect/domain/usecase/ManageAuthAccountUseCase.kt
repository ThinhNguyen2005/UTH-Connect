package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject

class ManageAuthAccountUseCase @Inject constructor(private val auth: AuthRepository) {
    fun observePendingIdentity() = auth.observePendingIdentity()
    suspend fun currentIdentity() = auth.currentIdentity()
}

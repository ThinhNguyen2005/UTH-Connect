package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

class ToggleResolveListingUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    suspend operator fun invoke(
        id: String,
        isResolved: Boolean,
    ): Result<Unit> = repository.toggleResolved(id, isResolved)
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

/**
 * Use case to toggle the resolved status of a listing.
 */
class ToggleResolveListingUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    /**
     * Updates the resolved flag of a listing.
     *
     * @param id Unique identifier of the listing.
     * @param isResolved New resolved status boolean.
     * @return Result indicating success or failure.
     */
    suspend operator fun invoke(
        id: String,
        isResolved: Boolean,
    ): Result<Unit> = repository.toggleResolved(id, isResolved)
}

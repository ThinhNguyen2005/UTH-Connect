package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

/**
 * Use case to retrieve all lost and found listings.
 */
class GetLostFoundListingsUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    /**
     * Executes the query to fetch all listings.
     *
     * @return Result containing list of [LostFoundListing].
     */
    suspend operator fun invoke(): Result<List<LostFoundListing>> = repository.getListings()
}

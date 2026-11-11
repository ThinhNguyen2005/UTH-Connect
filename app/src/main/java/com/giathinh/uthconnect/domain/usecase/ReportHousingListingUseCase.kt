package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class ReportHousingListingUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(listingId: String, reason: String): Result<Unit> = repository.reportListing(listingId = listingId, reason = reason)
}

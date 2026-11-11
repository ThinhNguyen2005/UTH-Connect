package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class CreateHousingListingUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(listing: HousingListing): Result<HousingListing> = repository.createListing(listing)
}

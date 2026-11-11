package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails
import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class CreateHousingListingUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(
        listing: HousingListing,
        privateDetails: HousingPrivateDetails,
    ): Result<HousingListing> = repository.createListing(listing, privateDetails)
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class GetHousingListingDetailUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(id: String): Result<HousingListing> = repository.getListingById(id)
}

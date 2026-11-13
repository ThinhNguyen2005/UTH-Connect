package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class GetHousingListingsUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(): Result<List<HousingListing>> = repository.getListings()
}

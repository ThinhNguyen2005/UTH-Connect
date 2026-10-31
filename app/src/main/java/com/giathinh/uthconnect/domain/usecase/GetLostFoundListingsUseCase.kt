package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

class GetLostFoundListingsUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    suspend operator fun invoke(): Result<List<LostFoundListing>> = repository.getListings()
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class MarkHousingListingFulfilledUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.markListingFulfilled(id)
}

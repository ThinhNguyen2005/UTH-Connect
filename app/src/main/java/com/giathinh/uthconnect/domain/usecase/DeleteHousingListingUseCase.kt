package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.HousingRepository
import javax.inject.Inject

class DeleteHousingListingUseCase @Inject constructor(
    private val repository: HousingRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.deleteListing(id)
}

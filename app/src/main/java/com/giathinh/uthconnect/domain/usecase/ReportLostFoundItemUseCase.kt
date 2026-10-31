package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

class ReportLostFoundItemUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    suspend operator fun invoke(item: LostFoundListing): Result<LostFoundListing> {
        return repository.reportItem(item)
    }
}

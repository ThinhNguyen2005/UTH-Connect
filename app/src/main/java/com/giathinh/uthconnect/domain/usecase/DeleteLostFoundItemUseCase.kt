package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

/**
 * Use case to delete a lost or found item owned by the current user.
 */
class DeleteLostFoundItemUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    suspend operator fun invoke(
        id: String,
        imageUrl: String? = null,
    ): Result<Unit> = repository.deleteItem(id = id, imageUrl = imageUrl)
}

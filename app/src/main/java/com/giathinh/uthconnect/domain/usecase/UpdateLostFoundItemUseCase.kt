package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

/**
 * Use case to update an existing lost or found item owned by the current user.
 */
class UpdateLostFoundItemUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    suspend operator fun invoke(
        item: LostFoundListing,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = "image/jpeg",
        removeExistingImage: Boolean = false,
    ): Result<LostFoundListing> = repository.updateItem(
        item = item,
        imageBytes = imageBytes,
        imageMimeType = imageMimeType,
        removeExistingImage = removeExistingImage,
    )
}

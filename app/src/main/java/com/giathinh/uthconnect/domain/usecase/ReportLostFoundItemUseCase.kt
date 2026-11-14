package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import javax.inject.Inject

/**
 * Use case to report a new lost or found item.
 */
class ReportLostFoundItemUseCase @Inject constructor(
    private val repository: LostFoundRepository,
) {
    /**
     * Reports an item with optional attached image bytes.
     *
     * @param item The metadata of the item.
     * @param imageBytes Optional raw byte array of the attached photo.
     * @param imageMimeType Optional MIME type of the attached photo.
     * @return Result containing the created [LostFoundListing].
     */
    suspend operator fun invoke(
        item: LostFoundListing,
        imageBytes: ByteArray? = null,
        imageMimeType: String? = "image/jpeg",
    ): Result<LostFoundListing> = repository.reportItem(item, imageBytes, imageMimeType)
}

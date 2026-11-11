package com.giathinh.uthconnect.presentation.housing.detail

import com.giathinh.uthconnect.domain.model.HousingContactRequest
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingPrivateDetails

enum class HousingDetailFailure {
    NotFound,
    LoadFailed,
}

data class HousingDetailUiState(
    val isLoading: Boolean = true,
    val listing: HousingListing? = null,
    val failure: HousingDetailFailure? = null,
    val isActionLoading: Boolean = false,
    /** Request gần nhất của người đọc (phía người hỏi); null nếu chưa hỏi. */
    val myRequest: HousingContactRequest? = null,
    /** Danh sách request phía chủ tin, mới nhất trước. */
    val contactRequests: List<HousingContactRequest> = emptyList(),
    /** null = chưa mở khóa: UI chỉ hiện khu vực xấp xỉ, ẩn SĐT/Zalo/địa chỉ đầy đủ. */
    val privateDetails: HousingPrivateDetails? = null,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && listing == null

    val isOwner: Boolean get() = listing?.isOwner == true

    val isRevealed: Boolean get() = privateDetails != null
}

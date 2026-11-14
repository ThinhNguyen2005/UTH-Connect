package com.giathinh.uthconnect.presentation.housing.detail

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus

enum class HousingDetailFailure {
    NotFound,
    LoadFailed,
}

data class HousingDetailUiState(
    val isLoading: Boolean = true,
    val listing: HousingListing? = null,
    val failure: HousingDetailFailure? = null,
    val isActionLoading: Boolean = false,
    /** Người xem là chủ tin (trùng MSSV với phiên đăng nhập) — mới hiện khối quản lý tin. */
    val isOwner: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && listing == null

    /** Chỉ chủ tin mới thấy banner lý do bị từ chối — người xem khác không cần biết tin bị duyệt trượt. */
    val showRejectionBanner: Boolean
        get() = isOwner && listing?.status == HousingListingStatus.REJECTED
}

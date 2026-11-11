package com.giathinh.uthconnect.presentation.housing.detail

import com.giathinh.uthconnect.domain.model.HousingListing

enum class HousingDetailFailure {
    NotFound,
    LoadFailed,
}

data class HousingDetailUiState(
    val isLoading: Boolean = true,
    val listing: HousingListing? = null,
    val failure: HousingDetailFailure? = null,
    val isActionLoading: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && failure == null && listing == null
}

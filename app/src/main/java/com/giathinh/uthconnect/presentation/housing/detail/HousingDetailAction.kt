package com.giathinh.uthconnect.presentation.housing.detail

sealed interface HousingDetailAction {
    data object Retry : HousingDetailAction
    data object RequestContact : HousingDetailAction
    data object CancelContact : HousingDetailAction
    data class RespondContact(val requestId: String, val accept: Boolean) : HousingDetailAction
    data object CallHost : HousingDetailAction
    data object OpenZalo : HousingDetailAction
    data object OpenMap : HousingDetailAction
    data object MarkFulfilled : HousingDetailAction
}

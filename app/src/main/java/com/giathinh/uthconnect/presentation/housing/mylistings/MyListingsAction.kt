package com.giathinh.uthconnect.presentation.housing.mylistings

/** Ý định của người dùng trên màn "Tin của tôi" — Screen phát, ViewModel xử lý. */
sealed interface MyListingsAction {
    /** Tải lại có spinner — dùng cho nút thử lại khi màn lỗi chặn luồng. */
    data object Load : MyListingsAction

    /** Nạp lại nền khi màn hình resume (duyệt tin ở web xong quay về) — không nháy spinner. */
    data object Refresh : MyListingsAction

    /** Mở chi tiết tin — Route nhận event rồi điều hướng, Screen không giữ NavController. */
    data class OpenListing(val listingId: String) : MyListingsAction
}

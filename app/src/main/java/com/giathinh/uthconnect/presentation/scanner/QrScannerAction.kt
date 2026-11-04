package com.giathinh.uthconnect.presentation.scanner

/** Ý định của người quét, do `QrScannerScreen` phát ra và `QrScannerRoute` xử lý. */
sealed interface QrScannerAction {
    /** Đóng màn quét, trả về danh sách sự kiện. */
    data object Dismiss : QrScannerAction

    /** Xin lại quyền camera trong app. */
    data object RequestPermission : QrScannerAction

    /** Mở trang quyền của ứng dụng trong Cài đặt hệ thống. */
    data object OpenPermissionSettings : QrScannerAction

    /** Bind lại camera sau một lần thất bại (app khác vừa nhả camera, xoay máy…). */
    data object RetryCamera : QrScannerAction

    data object ToggleTorch : QrScannerAction

    /** Người dùng đã xem xong kết quả một lượt, cho phép quét mã tiếp. */
    data object ContinueScanning : QrScannerAction
}

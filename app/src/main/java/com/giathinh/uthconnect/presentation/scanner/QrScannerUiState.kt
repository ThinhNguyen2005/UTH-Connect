package com.giathinh.uthconnect.presentation.scanner

/**
 * Trạng thái hiển thị của một lượt quét, tách riêng vì `EventsUiState` chỉ biết
 * kết quả gọi server còn máy quét phải biết thêm quyền camera và tình trạng bind
 * camera. Screen chỉ vẽ từ object này; mọi quyết định nghiệp vụ ở `QrScannerRoute`.
 */
enum class QrScannerOutcome {
    /** Chưa có kết quả, khung ngắm đang hoạt động. */
    Idle,

    /** Server đang trả lời cho mã vừa đọc. */
    Processing,

    /** `check_in_ticket_qr` chấp nhận mã. */
    Success,

    /** Server từ chối (mã lạ, chưa tới giờ, quét trùng…) hoặc camera lỗi. */
    Failure,
}

data class QrScannerUiState(
    /** Người dùng đã trả lời hộp thoại quyền camera hay chưa. */
    val permissionAsked: Boolean = false,
    val permissionGranted: Boolean = false,

    /** Camera không bind được (thiếu camera sau, app khác đang giữ camera…). */
    val cameraError: String? = null,

    val outcome: QrScannerOutcome = QrScannerOutcome.Idle,

    /** Chuỗi kết quả lấy từ `EventsUiState.message`/`error` — màn quét chỉ hiển thị, không tự đoán. */
    val resultText: String? = null,

    /** Số lượt điểm danh thành công trong phiên quét này. */
    val checkedInCount: Int = 0,

    val torchOn: Boolean = false,
    val torchAvailable: Boolean = false,
) {
    /** Đã bấm từ chối nên xin lại từ app vô ích, phải đưa người dùng sang Cài đặt. */
    val permissionBlocked: Boolean
        get() = permissionAsked && !permissionGranted

    /** Khung ngắm chỉ "sống" khi camera chạy thật và đang chờ một mã khác. */
    val scanning: Boolean
        get() = permissionGranted && cameraError == null &&
            (outcome == QrScannerOutcome.Idle || outcome == QrScannerOutcome.Failure)
}

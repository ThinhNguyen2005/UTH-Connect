package com.giathinh.uthconnect.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.model.ServerErrorCode

/**
 * Dịch mã lỗi server thành câu người dùng đọc được, nói rõ *phải làm gì tiếp*.
 *
 * Dùng ở màn hình (không dùng trong ViewModel) vì `stringResource` cần Composition;
 * ViewModel chỉ phát `ServerErrorCode`. Với `Unknown` ta giữ nguyên chuỗi gốc để
 * lập trình viên còn dấu vết debug, nếu không có chuỗi nào mới dùng câu chung.
 */
@Composable
fun ServerErrorCode.messageText(fallback: String? = null): String = when (this) {
    ServerErrorCode.InvalidQr -> stringResource(R.string.common_error_invalid_qr)
    ServerErrorCode.QrExpired -> stringResource(R.string.common_error_qr_expired)
    ServerErrorCode.CheckInNotOpen -> stringResource(R.string.common_error_checkin_not_open)
    ServerErrorCode.AlreadyCheckedIn -> stringResource(R.string.common_error_already_checked_in)
    ServerErrorCode.NotRegistered -> stringResource(R.string.common_error_not_registered)
    ServerErrorCode.TicketUnavailable -> stringResource(R.string.common_error_ticket_unavailable)
    ServerErrorCode.StudentNotVerified -> stringResource(R.string.common_error_student_not_verified)
    ServerErrorCode.EventNotFound -> stringResource(R.string.common_error_event_not_found)
    ServerErrorCode.EventFull -> stringResource(R.string.common_error_event_full)
    ServerErrorCode.RegistrationClosed -> stringResource(R.string.common_error_registration_closed)
    ServerErrorCode.EventLocked -> stringResource(R.string.common_error_event_locked)
    ServerErrorCode.EventAlreadyStarted -> stringResource(R.string.common_error_event_already_started)
    ServerErrorCode.InvalidEvent -> stringResource(R.string.common_error_invalid_event)
    ServerErrorCode.InvalidEventStatus -> stringResource(R.string.common_error_invalid_event_status)
    ServerErrorCode.InvalidEventTransition -> stringResource(R.string.common_error_invalid_event_transition)
    ServerErrorCode.InvalidStudentId -> stringResource(R.string.common_error_invalid_student_id)
    ServerErrorCode.StudentIdAlreadyVerified -> stringResource(R.string.common_error_student_id_already_verified)
    ServerErrorCode.UserNotFound -> stringResource(R.string.common_error_user_not_found)
    ServerErrorCode.AdminCannotDemoteSelf -> stringResource(R.string.common_error_admin_cannot_demote_self)
    ServerErrorCode.Forbidden -> stringResource(R.string.common_error_forbidden)
    ServerErrorCode.AuthRequired -> stringResource(R.string.common_error_auth_required)
    ServerErrorCode.HousingOpenLimit -> stringResource(R.string.common_error_housing_open_limit)
    ServerErrorCode.HousingNotFound -> stringResource(R.string.common_error_housing_not_found)
    ServerErrorCode.HousingStatusRpcOnly -> stringResource(R.string.common_error_housing_status_rpc_only)
    ServerErrorCode.HousingReportReasonRequired -> stringResource(R.string.common_error_housing_report_reason_required)
    ServerErrorCode.HousingImageBucketInvalid -> stringResource(R.string.common_error_housing_image_bucket_invalid)
    ServerErrorCode.HousingZaloUrlInvalid -> stringResource(R.string.common_error_housing_zalo_url_invalid)
    ServerErrorCode.Network -> stringResource(R.string.common_error_network)
    ServerErrorCode.Unknown -> fallback?.takeIf { it.isNotBlank() } ?: stringResource(R.string.common_error_unknown)
}

package com.giathinh.uthconnect.presentation.attendance

import com.giathinh.uthconnect.domain.model.AttendanceEntry
import com.giathinh.uthconnect.domain.model.AttendanceRoster
import com.giathinh.uthconnect.domain.model.ServerErrorCode

enum class AttendanceFilter { ALL, PRESENT, MISSING }

/** Trạng thái lỗi của màn danh sách — Screen chọn câu chữ từ enum, không so chuỗi. */
enum class AttendanceFailure { LoadFailed, ExportFailed }

data class AttendanceUiState(
    val loading: Boolean = true,
    val roster: AttendanceRoster? = null,
    val query: String = "",
    val filter: AttendanceFilter = AttendanceFilter.ALL,
    /** Loại lỗi vừa xảy ra; `null` là màn đang sạch. */
    val failure: AttendanceFailure? = null,
    /** Mã lỗi server (`FORBIDDEN`…), chỉ có khi `failure == LoadFailed`. */
    val errorCode: ServerErrorCode? = null,
    /** Chuỗi thô từ tầng data — chỉ dùng làm fallback khi `errorCode == Unknown`. */
    val errorDetail: String? = null,
) {
    val visibleEntries: List<AttendanceEntry> get() = roster?.entries.orEmpty().filter { entry ->
        (filter == AttendanceFilter.ALL || entry.checkedIn == (filter == AttendanceFilter.PRESENT)) &&
            (
                query.isBlank() || entry.displayName.contains(query.trim(), ignoreCase = true) ||
                    entry.studentId.contains(query.trim(), ignoreCase = true)
                )
    }
}

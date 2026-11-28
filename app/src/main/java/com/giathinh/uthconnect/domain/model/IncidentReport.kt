package com.giathinh.uthconnect.domain.model

/** Trạng thái do máy chủ quản lý; client không tự quyết định chuyển trạng thái. */
enum class IncidentStatus {
    SUBMITTED,
    ACKNOWLEDGED,
    IN_PROGRESS,
    RESOLVED,
    REOPENED,
    CLOSED,
    REJECTED,
    DUPLICATE,
    CANCELLED,
}

enum class IncidentCategory {
    DOOR_LOCK,
    ELECTRICAL,
    WATER_PLUMBING,
    AC_FAN,
    PROJECTOR_IT,
    FURNITURE,
    HYGIENE,
    SAFETY,
    OTHER,
}

enum class IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

data class IncidentDraft(
    val clientRequestId: String,
    val campus: String,
    val roomCode: String,
    val locationDetail: String?,
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val title: String,
    val description: String,
    val imagePaths: List<String>,
    val isAnonymousPublic: Boolean,
)

/** Nội dung hiển thị; danh tính người báo cáo do backend quyết định theo quyền. */
data class IncidentReport(
    val id: String,
    val code: String,
    val campus: String,
    val roomCode: String,
    val locationDetail: String?,
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val title: String,
    val description: String,
    val imagePaths: List<String>,
    val isAnonymousPublic: Boolean,
    val status: IncidentStatus,
    val createdAt: String,
    val updatedAt: String,
    val rowVersion: Long,
)

data class IncidentStatusLog(
    val fromStatus: IncidentStatus?,
    val toStatus: IncidentStatus,
    val note: String?,
    val createdAt: String,
)

/** Public feed intentionally excludes the reporter identity and description. */
data class IncidentSummary(
    val id: String,
    val code: String,
    val campus: String,
    val roomCode: String,
    val category: IncidentCategory,
    val severity: IncidentSeverity,
    val title: String,
    val status: IncidentStatus,
    val createdAt: String,
    val confirmationCount: Long,
)

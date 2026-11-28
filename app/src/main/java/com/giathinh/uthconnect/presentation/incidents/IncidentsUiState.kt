package com.giathinh.uthconnect.presentation.incidents

import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.model.IncidentSummary
import com.giathinh.uthconnect.domain.validation.IncidentFieldError
import java.util.UUID

const val MAX_INCIDENT_IMAGES = 4

enum class IncidentTab {
    MY_REPORTS,
    CAMPUS_FEED,
}

enum class IncidentErrorCode {
    LOAD_FAILED,
    DETAIL_LOAD_FAILED,
    UPLOAD_FAILED,
    CREATE_FAILED,
    RATE_LIMIT,
    INVALID_IMAGE,
    CONFLICT,
    SELF_CONFIRM,
    NOTE_REQUIRED,
    INVALID_TRANSITION,
    AUTH_REQUIRED,
}

data class IncidentFormState(
    val clientRequestId: String = UUID.randomUUID().toString(),
    val campus: String = "CS1",
    val roomCode: String = "",
    val locationDetail: String = "",
    val category: IncidentCategory = IncidentCategory.OTHER,
    val severity: IncidentSeverity = IncidentSeverity.MEDIUM,
    val title: String = "",
    val description: String = "",
    val imagePaths: List<String> = emptyList(),
    val isAnonymousPublic: Boolean = false,
    val isUploadingImage: Boolean = false,
    val isSubmitting: Boolean = false,
    val campusError: IncidentFieldError? = null,
    val roomError: IncidentFieldError? = null,
    val titleError: IncidentFieldError? = null,
    val descriptionError: IncidentFieldError? = null,
    val imageError: IncidentFieldError? = null,
)

data class IncidentsUiState(
    val selectedTab: IncidentTab = IncidentTab.MY_REPORTS,
    val campusFilter: String? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val myReports: List<IncidentReport> = emptyList(),
    val openFeed: List<IncidentSummary> = emptyList(),
    val selectedReport: IncidentReport? = null,
    val statusHistory: List<IncidentStatusLog> = emptyList(),
    val isLoadingDetail: Boolean = false,
    val isTransitioning: Boolean = false,
    val confirmingIds: Set<String> = emptySet(),
    val confirmedIds: Set<String> = emptySet(),
    val reopenNote: String = "",
    val showCreateSheet: Boolean = false,
    val form: IncidentFormState = IncidentFormState(),
    val error: IncidentErrorCode? = null,
)

sealed interface IncidentsAction {
    data object Refresh : IncidentsAction
    data class SelectTab(val tab: IncidentTab) : IncidentsAction
    data class SelectCampusFilter(val campus: String?) : IncidentsAction
    data object OpenCreateSheet : IncidentsAction
    data object DismissCreateSheet : IncidentsAction
    data class CampusChanged(val campus: String) : IncidentsAction
    data class RoomCodeChanged(val roomCode: String) : IncidentsAction
    data class LocationDetailChanged(val detail: String) : IncidentsAction
    data class CategoryChanged(val category: IncidentCategory) : IncidentsAction
    data class SeverityChanged(val severity: IncidentSeverity) : IncidentsAction
    data class TitleChanged(val title: String) : IncidentsAction
    data class DescriptionChanged(val description: String) : IncidentsAction
    data class AnonymousChanged(val anonymous: Boolean) : IncidentsAction
    data class UploadCompressedImage(val bytes: ByteArray) : IncidentsAction
    data class ImageCompressionFailed(
        val error: IncidentErrorCode = IncidentErrorCode.INVALID_IMAGE,
    ) : IncidentsAction
    data class RemoveImage(val path: String) : IncidentsAction
    data object SubmitReport : IncidentsAction
    data class OpenDetail(val reportId: String) : IncidentsAction
    data object DismissDetail : IncidentsAction
    data class ReopenNoteChanged(val note: String) : IncidentsAction
    data class CancelReport(val report: IncidentReport) : IncidentsAction
    data class CloseResolvedReport(val report: IncidentReport) : IncidentsAction
    data class ReopenResolvedReport(val report: IncidentReport) : IncidentsAction
    data class ConfirmOpenIncident(val incidentId: String) : IncidentsAction
    data object DismissError : IncidentsAction
}

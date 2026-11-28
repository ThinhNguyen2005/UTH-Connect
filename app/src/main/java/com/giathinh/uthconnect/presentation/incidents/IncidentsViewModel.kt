package com.giathinh.uthconnect.presentation.incidents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import com.giathinh.uthconnect.domain.usecase.CreateIncidentReportUseCase
import com.giathinh.uthconnect.domain.usecase.InvalidIncidentReportException
import com.giathinh.uthconnect.domain.validation.IncidentFieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IncidentsViewModel @Inject constructor(
    private val repository: IncidentRepository,
    private val createIncidentReport: CreateIncidentReportUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(IncidentsUiState())
    val uiState: StateFlow<IncidentsUiState> = mutableState.asStateFlow()

    init {
        refresh(initial = true)
    }

    fun onAction(action: IncidentsAction) {
        val nextState = mutableState.value.nextStateFor(action)
        if (nextState != null) {
            mutableState.value = nextState
            return
        }
        when (action) {
            IncidentsAction.Refresh -> refresh(initial = false)

            is IncidentsAction.SelectCampusFilter -> {
                mutableState.update { it.copy(campusFilter = action.campus) }
                refresh(initial = false)
            }

            is IncidentsAction.UploadCompressedImage -> uploadImage(action.bytes)

            IncidentsAction.SubmitReport -> submitReport()

            is IncidentsAction.OpenDetail -> openDetail(action.reportId)

            is IncidentsAction.CancelReport -> transitionSelected(action.report, IncidentStatus.CANCELLED, null)

            is IncidentsAction.CloseResolvedReport -> transitionSelected(action.report, IncidentStatus.CLOSED, null)

            is IncidentsAction.ReopenResolvedReport -> reopenReport(action.report)

            is IncidentsAction.ConfirmOpenIncident -> confirmOpenIncident(action.incidentId)

            else -> Unit
        }
    }

    private fun refresh(initial: Boolean) {
        val currentCampus = mutableState.value.campusFilter
        val hasExisting = mutableState.value.myReports.isNotEmpty() ||
            mutableState.value.openFeed.isNotEmpty()
        mutableState.update {
            it.copy(
                isLoading = initial && !hasExisting,
                isRefreshing = !initial || hasExisting,
                error = null,
            )
        }
        viewModelScope.launch {
            val myDeferred = async { repository.getMyReports() }
            val openDeferred = async { repository.getOpenReports(currentCampus) }
            val myResult = myDeferred.await()
            val openResult = openDeferred.await()
            val failure = myResult.exceptionOrNull() ?: openResult.exceptionOrNull()
            mutableState.update { state ->
                state.copy(
                    isLoading = false,
                    isRefreshing = false,
                    myReports = myResult.getOrElse { state.myReports },
                    openFeed = openResult.getOrElse { state.openFeed },
                    error = failure?.let { mapIncidentError(it, IncidentErrorCode.LOAD_FAILED) },
                )
            }
        }
    }

    private fun uploadImage(bytes: ByteArray) {
        val currentForm = mutableState.value.form
        if (currentForm.isUploadingImage || currentForm.isSubmitting) return
        if (currentForm.imagePaths.size >= MAX_INCIDENT_IMAGES) {
            mutableState.update {
                it.copy(form = currentForm.copy(imageError = IncidentFieldError.INVALID_IMAGE_COUNT))
            }
            return
        }
        mutableState.update {
            it.copy(form = currentForm.copy(isUploadingImage = true, imageError = null))
        }
        viewModelScope.launch {
            repository.uploadIncidentImage(bytes)
                .onSuccess { key ->
                    mutableState.update { state ->
                        val nextPaths = (state.form.imagePaths + key).distinct().take(MAX_INCIDENT_IMAGES)
                        state.copy(form = state.form.copy(isUploadingImage = false, imagePaths = nextPaths))
                    }
                }
                .onFailure { error ->
                    mutableState.update { state ->
                        state.copy(
                            form = state.form.copy(isUploadingImage = false),
                            error = mapIncidentError(error, IncidentErrorCode.UPLOAD_FAILED),
                        )
                    }
                }
        }
    }

    private fun submitReport() {
        val form = mutableState.value.form
        if (form.isSubmitting || form.isUploadingImage) return
        mutableState.update {
            it.copy(
                form = form.copy(
                    isSubmitting = true,
                    campusError = null,
                    roomError = null,
                    titleError = null,
                    descriptionError = null,
                    imageError = null,
                ),
            )
        }
        val draft = IncidentDraft(
            clientRequestId = form.clientRequestId,
            campus = form.campus,
            roomCode = form.roomCode,
            locationDetail = form.locationDetail.trim().ifEmpty { null },
            category = form.category,
            severity = form.severity,
            title = form.title,
            description = form.description,
            imagePaths = form.imagePaths,
            isAnonymousPublic = form.isAnonymousPublic,
        )
        viewModelScope.launch {
            createIncidentReport(draft)
                .onSuccess { created ->
                    mutableState.update { state ->
                        state.copy(
                            showCreateSheet = false,
                            form = IncidentFormState(),
                            myReports = listOf(created) + state.myReports.filterNot { it.id == created.id },
                            error = null,
                        )
                    }
                    refresh(initial = false)
                }
                .onFailure { error -> handleCreateFailure(error) }
        }
    }

    private fun handleCreateFailure(error: Throwable) {
        if (error is InvalidIncidentReportException) {
            val v = error.validation
            mutableState.update { state ->
                state.copy(
                    form = state.form.copy(
                        isSubmitting = false,
                        campusError = v.campusError,
                        roomError = v.roomError,
                        titleError = v.titleError,
                        descriptionError = v.descriptionError,
                        imageError = v.imageError,
                    ),
                )
            }
            return
        }
        mutableState.update { state ->
            state.copy(
                form = state.form.copy(isSubmitting = false),
                error = mapIncidentError(error, IncidentErrorCode.CREATE_FAILED),
            )
        }
    }

    private fun openDetail(reportId: String) {
        val cached = mutableState.value.myReports.firstOrNull { it.id == reportId }
        mutableState.update {
            it.copy(
                selectedReport = cached,
                statusHistory = emptyList(),
                isLoadingDetail = true,
                reopenNote = "",
                error = null,
            )
        }
        viewModelScope.launch {
            val reportDeferred = async { repository.getReport(reportId) }
            val historyDeferred = async { repository.getStatusHistory(reportId) }
            val reportResult = reportDeferred.await()
            val historyResult = historyDeferred.await()
            val failure = reportResult.exceptionOrNull() ?: historyResult.exceptionOrNull()
            mutableState.update { state ->
                val loaded = reportResult.getOrNull() ?: state.selectedReport
                state.copy(
                    selectedReport = loaded,
                    statusHistory = historyResult.getOrDefault(emptyList()),
                    isLoadingDetail = false,
                    error = failure?.let { mapIncidentError(it, IncidentErrorCode.DETAIL_LOAD_FAILED) },
                )
            }
        }
    }

    private fun reopenReport(report: IncidentReport) {
        val note = mutableState.value.reopenNote.trim()
        if (note.isEmpty()) {
            mutableState.update { it.copy(error = IncidentErrorCode.NOTE_REQUIRED) }
            return
        }
        transitionSelected(report = report, target = IncidentStatus.REOPENED, note = note)
    }

    private fun transitionSelected(
        report: IncidentReport,
        target: IncidentStatus,
        note: String?,
    ) {
        if (mutableState.value.isTransitioning) return
        mutableState.update { it.copy(isTransitioning = true, error = null) }
        viewModelScope.launch {
            repository.transitionReport(
                id = report.id,
                expectedVersion = report.rowVersion.toInt(),
                targetStatus = target,
                note = note,
            ).onSuccess { newVersion ->
                val updated = report.copy(status = target, rowVersion = newVersion.toLong())
                mutableState.update { state ->
                    state.copy(
                        isTransitioning = false,
                        selectedReport = updated,
                        reopenNote = "",
                        myReports = state.myReports.map { if (it.id == updated.id) updated else it },
                    )
                }
                openDetail(report.id)
                refresh(initial = false)
            }.onFailure { error ->
                mutableState.update { state ->
                    state.copy(
                        isTransitioning = false,
                        error = mapIncidentError(error, IncidentErrorCode.INVALID_TRANSITION),
                    )
                }
            }
        }
    }

    private fun confirmOpenIncident(incidentId: String) {
        val state = mutableState.value
        if (incidentId in state.confirmingIds || incidentId in state.confirmedIds) return
        mutableState.update { it.copy(confirmingIds = it.confirmingIds + incidentId, error = null) }
        viewModelScope.launch {
            repository.confirmReport(incidentId)
                .onSuccess { updatedCount ->
                    mutableState.update { current ->
                        current.copy(
                            confirmingIds = current.confirmingIds - incidentId,
                            confirmedIds = current.confirmedIds + incidentId,
                            openFeed = current.openFeed.map { item ->
                                if (item.id == incidentId) {
                                    item.copy(confirmationCount = updatedCount)
                                } else {
                                    item
                                }
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update { current ->
                        current.copy(
                            confirmingIds = current.confirmingIds - incidentId,
                            error = mapIncidentError(error, IncidentErrorCode.LOAD_FAILED),
                        )
                    }
                }
        }
    }
}

private fun IncidentsUiState.nextStateFor(action: IncidentsAction): IncidentsUiState? {
    val updatedForm = form.nextFormFor(action)
    return if (updatedForm != null) {
        copy(form = updatedForm)
    } else {
        when (action) {
            is IncidentsAction.SelectTab -> copy(selectedTab = action.tab)

            IncidentsAction.OpenCreateSheet -> copy(showCreateSheet = true, error = null)

            IncidentsAction.DismissCreateSheet ->
                if (!form.isSubmitting && !form.isUploadingImage) copy(showCreateSheet = false) else this

            is IncidentsAction.ImageCompressionFailed -> copy(error = action.error)

            IncidentsAction.DismissDetail ->
                copy(selectedReport = null, statusHistory = emptyList(), reopenNote = "")

            is IncidentsAction.ReopenNoteChanged -> copy(reopenNote = action.note)

            IncidentsAction.DismissError -> copy(error = null)

            else -> null
        }
    }
}

private fun IncidentFormState.nextFormFor(action: IncidentsAction): IncidentFormState? = when (action) {
    is IncidentsAction.CampusChanged -> copy(campus = action.campus, campusError = null)
    is IncidentsAction.RoomCodeChanged -> copy(roomCode = action.roomCode, roomError = null)
    is IncidentsAction.LocationDetailChanged -> copy(locationDetail = action.detail)
    is IncidentsAction.CategoryChanged -> copy(category = action.category)
    is IncidentsAction.SeverityChanged -> copy(severity = action.severity)
    is IncidentsAction.TitleChanged -> copy(title = action.title, titleError = null)
    is IncidentsAction.DescriptionChanged -> copy(description = action.description, descriptionError = null)
    is IncidentsAction.AnonymousChanged -> copy(isAnonymousPublic = action.anonymous)
    is IncidentsAction.RemoveImage -> copy(imagePaths = imagePaths - action.path, imageError = null)
    else -> null
}

internal fun mapIncidentError(error: Throwable, fallback: IncidentErrorCode): IncidentErrorCode {
    val message = error.message.orEmpty()
    return when {
        "INCIDENT_RATE_LIMIT" in message -> IncidentErrorCode.RATE_LIMIT

        "INCIDENT_INVALID_IMAGE" in message -> IncidentErrorCode.INVALID_IMAGE

        "INCIDENT_CONFLICT" in message -> IncidentErrorCode.CONFLICT

        "INCIDENT_SELF_CONFIRM" in message -> IncidentErrorCode.SELF_CONFIRM

        "INCIDENT_NOTE_REQUIRED" in message -> IncidentErrorCode.NOTE_REQUIRED

        "INCIDENT_INVALID_TRANSITION" in message -> IncidentErrorCode.INVALID_TRANSITION

        "INCIDENT_UNAUTHENTICATED" in message || "sign-in required" in message ->
            IncidentErrorCode.AUTH_REQUIRED

        else -> fallback
    }
}

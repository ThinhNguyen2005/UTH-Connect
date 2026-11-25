package com.giathinh.uthconnect.presentation.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.data.repository.SupabaseHttpException
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.usecase.DeleteLostFoundItemUseCase
import com.giathinh.uthconnect.domain.usecase.GetLostFoundListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportLostFoundItemUseCase
import com.giathinh.uthconnect.domain.usecase.ToggleResolveListingUseCase
import com.giathinh.uthconnect.domain.usecase.UpdateLostFoundItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel managing state and user interactions for the Lost & Found feature.
 * Coordinates listing loading, filtering, creation, editing, deletion, and status resolution.
 */
@HiltViewModel
class LostFoundViewModel @Inject constructor(
    private val getListingsUseCase: GetLostFoundListingsUseCase,
    private val reportItemUseCase: ReportLostFoundItemUseCase,
    private val updateItemUseCase: UpdateLostFoundItemUseCase,
    private val deleteItemUseCase: DeleteLostFoundItemUseCase,
    private val toggleResolveListingUseCase: ToggleResolveListingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LostFoundUiState())
    val uiState: StateFlow<LostFoundUiState> = _uiState.asStateFlow()

    private val inFlightResolvingIds = mutableSetOf<String>()

    init {
        loadListings()
    }

    /**
     * Dispatches user actions to state reducers and background use cases.
     */
    @Suppress("CyclomaticComplexMethod")
    fun onAction(action: LostFoundAction) {
        when (action) {
            is LostFoundAction.SelectKind -> _uiState.update { it.copy(selectedKind = action.kind) }

            is LostFoundAction.SelectCategory -> _uiState.update { it.copy(selectedCategory = action.category) }

            is LostFoundAction.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = action.query) }

            is LostFoundAction.SetOnlyMine -> _uiState.update { it.copy(onlyMine = action.onlyMine) }

            is LostFoundAction.SetShowResolved -> _uiState.update { it.copy(showResolved = action.showResolved) }

            LostFoundAction.ResetFilters -> _uiState.update {
                it.copy(
                    selectedCategory = ItemCategory.ALL,
                    searchQuery = "",
                    onlyMine = false,
                    showResolved = true,
                )
            }

            LostFoundAction.Refresh -> loadListings(isRefresh = true)

            is LostFoundAction.ToggleResolveListing -> toggleResolveListing(action.id)

            LostFoundAction.OpenReportDialog -> _uiState.update {
                it.copy(
                    isReportDialogOpen = true,
                    reportForm = ReportFormState(kind = it.selectedKind),
                )
            }

            is LostFoundAction.OpenEditDialog -> openEditDialog(action.listing)

            LostFoundAction.CloseReportDialog -> _uiState.update {
                it.copy(
                    isReportDialogOpen = false,
                    reportForm = ReportFormState(),
                )
            }

            is LostFoundAction.RequestDeleteListing -> _uiState.update {
                it.copy(pendingDeleteListing = action.listing)
            }

            LostFoundAction.CancelDeleteListing -> _uiState.update {
                it.copy(pendingDeleteListing = null)
            }

            LostFoundAction.ConfirmDeleteListing -> confirmDeleteListing()

            LostFoundAction.SubmitReport -> submitReport()

            LostFoundAction.DismissMessage -> _uiState.update { it.copy(userMessage = null) }

            LostFoundAction.ShowDialerUnavailable -> _uiState.update {
                it.copy(userMessage = UserMessage.DialerUnavailable)
            }

            LostFoundAction.Retry -> loadListings()

            else -> handleFormFieldAction(action)
        }
    }

    private fun openEditDialog(listing: LostFoundListing) {
        _uiState.update {
            it.copy(
                isReportDialogOpen = true,
                reportForm = ReportFormState(
                    editingListingId = listing.id,
                    title = listing.title,
                    description = listing.description,
                    kind = listing.kind,
                    category = listing.category,
                    location = listing.location,
                    contactName = listing.contactName,
                    contactPhone = listing.contactPhone,
                    existingImageUrl = listing.imageUrl,
                    removeExistingImage = false,
                ),
            )
        }
    }

    private fun handleFormFieldAction(action: LostFoundAction) {
        _uiState.update { state ->
            val updatedForm = when (action) {
                is LostFoundAction.UpdateReportTitle -> state.reportForm.copy(title = action.title, titleError = null)

                is LostFoundAction.UpdateReportDescription -> state.reportForm.copy(
                    description = action.description,
                    descriptionError = null,
                )

                is LostFoundAction.UpdateReportKind -> state.reportForm.copy(kind = action.kind)

                is LostFoundAction.UpdateReportCategory -> state.reportForm.copy(category = action.category)

                is LostFoundAction.UpdateReportLocation -> state.reportForm.copy(
                    location = action.location,
                    locationError = null,
                )

                is LostFoundAction.UpdateReportContactName -> state.reportForm.copy(contactName = action.contactName)

                is LostFoundAction.UpdateReportContactPhone -> state.reportForm.copy(
                    contactPhone = action.contactPhone,
                    phoneError = null,
                )

                is LostFoundAction.UpdateReportImage -> state.reportForm.copy(
                    imageBytes = action.bytes,
                    imageMimeType = action.mimeType,
                    imagePreviewUri = action.previewUri,
                    removeExistingImage = false,
                    isProcessingImage = false,
                )

                is LostFoundAction.SetReportImageProcessing -> state.reportForm.copy(
                    isProcessingImage = action.isProcessing,
                )

                LostFoundAction.RemoveReportImage -> state.reportForm.copy(
                    imageBytes = null,
                    imageMimeType = null,
                    imagePreviewUri = null,
                    existingImageUrl = null,
                    removeExistingImage = true,
                    isProcessingImage = false,
                )

                else -> state.reportForm
            }
            state.copy(reportForm = updatedForm)
        }
    }

    private fun loadListings(isRefresh: Boolean = false) {
        viewModelScope.launch {
            val uid = getListingsUseCase.currentUserId()
            _uiState.update {
                if (isRefresh) {
                    it.copy(isRefreshing = true, currentUserId = uid)
                } else {
                    it.copy(isLoading = true, isError = false, currentUserId = uid)
                }
            }
            getListingsUseCase().fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isError = false,
                            currentUserId = getListingsUseCase.currentUserId(),
                            listings = items,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isError = if (isRefresh) it.isError else true,
                        )
                    }
                },
            )
        }
    }

    private fun toggleResolveListing(id: String) {
        if (!inFlightResolvingIds.add(id)) return
        val currentItem = _uiState.value.listings.find { it.id == id }
        if (currentItem == null) {
            inFlightResolvingIds.remove(id)
            return
        }
        val newStatus = !currentItem.isResolved
        _uiState.update { current ->
            current.copy(
                listings = current.listings.map { item ->
                    if (item.id == id) item.copy(isResolved = newStatus) else item
                },
            )
        }
        viewModelScope.launch {
            try {
                toggleResolveListingUseCase(id, newStatus).onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            userMessage = error.toMutationUserMessage(UserMessage.UpdateStatusFailed),
                            listings = current.listings.map { item ->
                                if (item.id == id) item.copy(isResolved = !newStatus) else item
                            },
                        )
                    }
                }
            } finally {
                inFlightResolvingIds.remove(id)
            }
        }
    }

    private fun confirmDeleteListing() {
        val target = _uiState.value.pendingDeleteListing ?: return
        if (target.id in _uiState.value.deletingIds) return
        _uiState.update {
            it.copy(
                pendingDeleteListing = null,
                deletingIds = it.deletingIds + target.id,
            )
        }
        viewModelScope.launch {
            deleteItemUseCase(target.id, target.imageUrl).fold(
                onSuccess = {
                    _uiState.update { current ->
                        current.copy(
                            listings = current.listings.filterNot { it.id == target.id },
                            deletingIds = current.deletingIds - target.id,
                            userMessage = UserMessage.DeleteSuccess,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { current ->
                        current.copy(
                            deletingIds = current.deletingIds - target.id,
                            userMessage = error.toMutationUserMessage(UserMessage.DeleteFailed),
                        )
                    }
                },
            )
        }
    }

    private fun submitReport() {
        val form = _uiState.value.reportForm
        if (form.isSubmitting || form.isProcessingImage) return

        val titleError = if (form.title.isBlank()) FormValidationError.TitleRequired else null
        val locationError = if (form.location.isBlank()) FormValidationError.LocationRequired else null
        val phone = form.contactPhone.trim().replace(" ", "").replace("-", "")
        val phoneError = when {
            phone.isBlank() -> FormValidationError.PhoneRequired
            !Regex("^(0[0-9]{9,10}|\\+84[0-9]{9,10})$").matches(phone) -> FormValidationError.PhoneInvalid
            else -> null
        }

        if (titleError != null || locationError != null || phoneError != null) {
            _uiState.update {
                it.copy(
                    reportForm = it.reportForm.copy(
                        titleError = titleError,
                        descriptionError = null,
                        locationError = locationError,
                        phoneError = phoneError,
                    ),
                )
            }
            return
        }

        // Mark submitting before scheduling the coroutine so repeated taps cannot enqueue two requests.
        _uiState.update { it.copy(reportForm = it.reportForm.copy(isSubmitting = true)) }
        val editingId = form.editingListingId
        if (editingId == null) {
            submitCreate(form = form, normalizedPhone = phone)
        } else {
            submitUpdate(editingId = editingId, form = form, normalizedPhone = phone)
        }
    }

    private fun submitCreate(form: ReportFormState, normalizedPhone: String) {
        viewModelScope.launch {
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val newListing = LostFoundListing(
                title = form.title.trim(),
                description = form.description.trim(),
                kind = form.kind,
                category = form.category,
                location = form.location.trim(),
                date = today,
                contactName = form.contactName.trim(),
                contactPhone = normalizedPhone,
            )
            reportItemUseCase(
                item = newListing,
                imageBytes = form.imageBytes,
                imageMimeType = form.imageMimeType,
            ).fold(
                onSuccess = { created ->
                    _uiState.update { state ->
                        state.copy(
                            listings = listOf(created) + state.listings,
                            selectedKind = created.kind,
                            selectedCategory = ItemCategory.ALL,
                            searchQuery = "",
                            isReportDialogOpen = false,
                            reportForm = ReportFormState(),
                            userMessage = UserMessage.PostSuccess,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            reportForm = it.reportForm.copy(isSubmitting = false),
                            userMessage = error.toMutationUserMessage(UserMessage.PostFailed),
                        )
                    }
                },
            )
        }
    }

    private fun submitUpdate(editingId: String, form: ReportFormState, normalizedPhone: String) {
        viewModelScope.launch {
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val existing = _uiState.value.listings.find { it.id == editingId }
            val updatedPayload = LostFoundListing(
                id = editingId,
                title = form.title.trim(),
                description = form.description.trim(),
                kind = form.kind,
                category = form.category,
                location = form.location.trim(),
                date = existing?.date ?: today,
                contactName = form.contactName.trim(),
                contactPhone = normalizedPhone,
                imageUrl = form.existingImageUrl,
                isResolved = existing?.isResolved ?: false,
                userId = existing?.userId ?: _uiState.value.currentUserId,
            )
            updateItemUseCase(
                item = updatedPayload,
                imageBytes = form.imageBytes,
                imageMimeType = form.imageMimeType,
                removeExistingImage = form.removeExistingImage,
            ).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        val updatedList = if (state.listings.any { it.id == updated.id }) {
                            state.listings.map { if (it.id == updated.id) updated else it }
                        } else {
                            listOf(updated) + state.listings
                        }
                        state.copy(
                            listings = updatedList,
                            selectedKind = updated.kind,
                            isReportDialogOpen = false,
                            reportForm = ReportFormState(),
                            userMessage = UserMessage.UpdateSuccess,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            reportForm = it.reportForm.copy(isSubmitting = false),
                            userMessage = error.toMutationUserMessage(UserMessage.PostFailed),
                        )
                    }
                },
            )
        }
    }

    private fun Throwable.toMutationUserMessage(fallback: UserMessage): UserMessage {
        var cursor: Throwable? = this
        while (cursor != null) {
            if (cursor is SupabaseHttpException) {
                return when (cursor.status) {
                    HttpURLConnection.HTTP_UNAUTHORIZED -> UserMessage.AuthRequired
                    HttpURLConnection.HTTP_FORBIDDEN -> UserMessage.Forbidden
                    else -> fallback
                }
            }
            cursor = cursor.cause
        }
        val msg = message.orEmpty()
        return when {
            msg.contains("sign-in required", ignoreCase = true) ||
                msg.contains("session expired", ignoreCase = true) ||
                msg.contains("AuthRequired", ignoreCase = true) -> UserMessage.AuthRequired

            msg.contains("forbidden", ignoreCase = true) ||
                msg.contains("permission", ignoreCase = true) -> UserMessage.Forbidden

            else -> fallback
        }
    }
}

package com.giathinh.uthconnect.presentation.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.data.repository.SupabaseHttpException
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.usecase.GetLostFoundListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportLostFoundItemUseCase
import com.giathinh.uthconnect.domain.usecase.ToggleResolveListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel managing the state and user interactions for the Lost & Found feature.
 * Coordinates listings loading, real-time filtering, item reporting, and status resolution.
 */
@HiltViewModel
class LostFoundViewModel @Inject constructor(
    private val getListingsUseCase: GetLostFoundListingsUseCase,
    private val reportItemUseCase: ReportLostFoundItemUseCase,
    private val toggleResolveListingUseCase: ToggleResolveListingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LostFoundUiState())
    val uiState: StateFlow<LostFoundUiState> = _uiState.asStateFlow()

    private val inFlightResolvingIds = mutableSetOf<String>()

    init {
        loadListings()
    }

    /**
     * Dispatches user actions to appropriate state reducers and background use cases.
     *
     * @param action The user intent or interaction to process.
     */
    fun onAction(action: LostFoundAction) {
        when (action) {
            is LostFoundAction.SelectKind -> {
                _uiState.update { it.copy(selectedKind = action.kind) }
            }

            is LostFoundAction.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = action.category) }
            }

            is LostFoundAction.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = action.query) }
            }

            is LostFoundAction.ToggleResolveListing -> {
                if (!inFlightResolvingIds.add(action.id)) return
                val currentItem = _uiState.value.listings.find { it.id == action.id }
                if (currentItem == null) {
                    inFlightResolvingIds.remove(action.id)
                    return
                }
                val newStatus = !currentItem.isResolved
                _uiState.update { current ->
                    current.copy(
                        listings = current.listings.map { item ->
                            if (item.id == action.id) {
                                item.copy(isResolved = newStatus)
                            } else {
                                item
                            }
                        },
                    )
                }
                viewModelScope.launch {
                    try {
                        toggleResolveListingUseCase(action.id, newStatus).onFailure { error ->
                            _uiState.update { current ->
                                val userMsg = when {
                                    error.isAuthenticationError() -> UserMessage.AuthRequired
                                    error.isForbiddenError() -> UserMessage.Forbidden
                                    else -> current.userMessage
                                }
                                current.copy(
                                    userMessage = userMsg,
                                    listings = current.listings.map { item ->
                                        if (item.id == action.id) {
                                            item.copy(isResolved = !newStatus)
                                        } else {
                                            item
                                        }
                                    },
                                )
                            }
                        }
                    } finally {
                        inFlightResolvingIds.remove(action.id)
                    }
                }
            }

            LostFoundAction.OpenReportDialog -> {
                _uiState.update {
                    it.copy(
                        isReportDialogOpen = true,
                        reportForm = ReportFormState(kind = it.selectedKind),
                    )
                }
            }

            LostFoundAction.CloseReportDialog -> {
                _uiState.update { it.copy(isReportDialogOpen = false) }
            }

            is LostFoundAction.UpdateReportTitle -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(title = action.title, titleError = null))
                }
            }

            is LostFoundAction.UpdateReportDescription -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(description = action.description, descriptionError = null))
                }
            }

            is LostFoundAction.UpdateReportKind -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(kind = action.kind))
                }
            }

            is LostFoundAction.UpdateReportCategory -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(category = action.category))
                }
            }

            is LostFoundAction.UpdateReportLocation -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(location = action.location, locationError = null))
                }
            }

            is LostFoundAction.UpdateReportContactName -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(contactName = action.contactName))
                }
            }

            is LostFoundAction.UpdateReportContactPhone -> {
                _uiState.update {
                    it.copy(reportForm = it.reportForm.copy(contactPhone = action.contactPhone, phoneError = null))
                }
            }

            is LostFoundAction.UpdateReportImage -> {
                _uiState.update {
                    it.copy(
                        reportForm = it.reportForm.copy(
                            imageBytes = action.bytes,
                            imageMimeType = action.mimeType,
                            imagePreviewUri = action.previewUri,
                        ),
                    )
                }
            }

            LostFoundAction.RemoveReportImage -> {
                _uiState.update {
                    it.copy(
                        reportForm = it.reportForm.copy(
                            imageBytes = null,
                            imageMimeType = null,
                            imagePreviewUri = null,
                        ),
                    )
                }
            }

            LostFoundAction.SubmitReport -> submitReport()

            LostFoundAction.DismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }

            LostFoundAction.Retry -> loadListings()
        }
    }

    /**
     * Loads the latest lost and found items from the data source asynchronously.
     */
    private fun loadListings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            getListingsUseCase().fold(
                onSuccess = { items ->
                    _uiState.update { it.copy(isLoading = false, isError = false, listings = items) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                },
            )
        }
    }

    /**
     * Validates and submits the current item report form.
     */
    private fun submitReport() {
        val form = _uiState.value.reportForm
        if (form.isSubmitting) return

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

        // Mark submitting before scheduling the coroutine so repeated taps cannot enqueue two posts.
        _uiState.update { it.copy(reportForm = it.reportForm.copy(isSubmitting = true)) }
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
                contactPhone = phone,
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
                    val userMsg = when {
                        error.isAuthenticationError() -> UserMessage.AuthRequired
                        error.isForbiddenError() -> UserMessage.Forbidden
                        else -> UserMessage.PostFailed
                    }
                    _uiState.update {
                        it.copy(
                            reportForm = it.reportForm.copy(isSubmitting = false),
                            userMessage = userMsg,
                        )
                    }
                },
            )
        }
    }
}

/**
 * Traverses the throwable cause hierarchy to find an underlying [SupabaseHttpException] if present.
 */
private tailrec fun Throwable.findSupabaseHttpException(): SupabaseHttpException? = when (this) {
    is SupabaseHttpException -> this
    else -> cause?.findSupabaseHttpException()
}

/**
 * Checks if the throwable corresponds to an unauthenticated session (HTTP 401).
 */
private fun Throwable.isAuthenticationError(): Boolean {
    val httpException = findSupabaseHttpException()
    if (httpException != null) {
        return httpException.status == java.net.HttpURLConnection.HTTP_UNAUTHORIZED
    }
    return message?.contains("sign-in required", ignoreCase = true) == true ||
        message?.contains("session expired", ignoreCase = true) == true ||
        message?.contains("AuthRequired", ignoreCase = true) == true
}

/**
 * Checks if the throwable corresponds to insufficient access permission (HTTP 403 Forbidden).
 */
private fun Throwable.isForbiddenError(): Boolean {
    val httpException = findSupabaseHttpException()
    return httpException?.status == java.net.HttpURLConnection.HTTP_FORBIDDEN
}

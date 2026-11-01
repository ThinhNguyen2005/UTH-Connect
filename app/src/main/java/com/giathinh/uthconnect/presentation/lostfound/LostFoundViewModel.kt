package com.giathinh.uthconnect.presentation.lostfound

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.usecase.GetLostFoundListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportLostFoundItemUseCase
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

@HiltViewModel
class LostFoundViewModel @Inject constructor(
    private val getListingsUseCase: GetLostFoundListingsUseCase,
    private val reportItemUseCase: ReportLostFoundItemUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LostFoundUiState())
    val uiState: StateFlow<LostFoundUiState> = _uiState.asStateFlow()

    init {
        loadListings()
    }

    fun onAction(action: LostFoundAction) {
        when (action) {
            is LostFoundAction.SelectKind -> {
                _uiState.update { it.copy(selectedKind = action.kind) }
            }
            is LostFoundAction.SelectCategory -> {
                _uiState.update { it.copy(selectedCategory = action.category) }
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
            LostFoundAction.SubmitReport -> submitReport()
            LostFoundAction.DismissMessage -> {
                _uiState.update { it.copy(userMessage = null) }
            }
            LostFoundAction.Retry -> loadListings()
        }
    }

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

    private fun submitReport() {
        val form = _uiState.value.reportForm
        if (form.isSubmitting) return

        val titleError = if (form.title.isBlank()) FormValidationError.TitleRequired else null
        val currentDescLength = form.description.trim().length
        val descriptionError = if (currentDescLength < 20) {
            FormValidationError.DescriptionTooShort(currentDescLength)
        } else null

        val locationError = if (form.location.isBlank()) FormValidationError.LocationRequired else null
        val phone = form.contactPhone.trim().replace(" ", "").replace("-", "")
        val phoneError = when {
            phone.isBlank() -> FormValidationError.PhoneRequired
            !Regex("^(0[0-9]{9,10}|\\+84[0-9]{9,10})$").matches(phone) -> FormValidationError.PhoneInvalid
            else -> null
        }

        if (titleError != null || descriptionError != null || locationError != null || phoneError != null) {
            _uiState.update {
                it.copy(
                    reportForm = it.reportForm.copy(
                        titleError = titleError,
                        descriptionError = descriptionError,
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

            reportItemUseCase(newListing).fold(
                onSuccess = { created ->
                    _uiState.update { state ->
                        state.copy(
                            listings = listOf(created) + state.listings,
                            selectedKind = created.kind,
                            selectedCategory = ItemCategory.ALL,
                            isReportDialogOpen = false,
                            reportForm = ReportFormState(),
                            userMessage = UserMessage.PostSuccess,
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            reportForm = it.reportForm.copy(isSubmitting = false),
                            userMessage = UserMessage.PostFailed,
                        )
                    }
                },
            )
        }
    }
}

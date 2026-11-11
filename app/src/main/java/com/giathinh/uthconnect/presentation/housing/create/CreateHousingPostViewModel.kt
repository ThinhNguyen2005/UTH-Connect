package com.giathinh.uthconnect.presentation.housing.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.HostVerificationMethod
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.CreateHousingListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateHousingPostEvent {
    data object PostCreated : CreateHousingPostEvent
    data class SubmitFailed(val code: ServerErrorCode) : CreateHousingPostEvent
    data object DraftSaved : CreateHousingPostEvent
}

@HiltViewModel
class CreateHousingPostViewModel @Inject constructor(
    private val createListing: CreateHousingListingUseCase,
    private val sessions: AccessStudentSessionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateHousingPostUiState())
    val uiState: StateFlow<CreateHousingPostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateHousingPostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onAction(action: CreateHousingPostAction) {
        when (action) {
            is CreateHousingPostAction.ChangeType,
            is CreateHousingPostAction.ChangeCampus,
            is CreateHousingPostAction.ChangeTitle,
            is CreateHousingPostAction.ChangeDescription,
            is CreateHousingPostAction.ChangeRent,
            is CreateHousingPostAction.ChangeDeposit,
            is CreateHousingPostAction.ChangeAddress,
            is CreateHousingPostAction.ChangeDistance,
            is CreateHousingPostAction.ChangeArea,
            is CreateHousingPostAction.ChangeScheduleNote,
            is CreateHousingPostAction.ChangeContactPhone,
            is CreateHousingPostAction.ChangeContactZalo,
            is CreateHousingPostAction.ChangeAllowZalo,
            -> handleFieldChange(action)

            is CreateHousingPostAction.ChangeUtilitiesIncluded,
            is CreateHousingPostAction.ChangeSlots,
            is CreateHousingPostAction.ChangeGender,
            is CreateHousingPostAction.ToggleAmenity,
            -> handlePreferenceChange(action)

            is CreateHousingPostAction.AddPhoto,
            is CreateHousingPostAction.RemovePhoto,
            -> handlePhotoChange(action)

            CreateHousingPostAction.SaveDraft -> saveDraft()

            CreateHousingPostAction.Submit -> submitPost()
        }
    }

    /** Gom nhóm ô nhập liệu để [onAction] không vượt ngưỡng phức tạp (docs/CODING_STANDARDS.md mục 4). */
    private fun handleFieldChange(action: CreateHousingPostAction) {
        when (action) {
            is CreateHousingPostAction.ChangeType -> _uiState.update { it.copy(type = action.type) }

            is CreateHousingPostAction.ChangeCampus -> _uiState.update { it.copy(campus = action.campus) }

            is CreateHousingPostAction.ChangeTitle ->
                _uiState.update { it.copy(title = action.title, validationError = null) }

            is CreateHousingPostAction.ChangeDescription ->
                _uiState.update { it.copy(description = action.desc) }

            is CreateHousingPostAction.ChangeRent ->
                _uiState.update { it.copy(rentInput = action.rent, validationError = null) }

            is CreateHousingPostAction.ChangeDeposit ->
                _uiState.update { it.copy(depositInput = action.deposit) }

            is CreateHousingPostAction.ChangeAddress ->
                _uiState.update { it.copy(address = action.address, validationError = null) }

            is CreateHousingPostAction.ChangeDistance ->
                _uiState.update { it.copy(distanceInput = action.distance) }

            is CreateHousingPostAction.ChangeArea ->
                _uiState.update { it.copy(areaInput = action.area) }

            is CreateHousingPostAction.ChangeScheduleNote ->
                _uiState.update { it.copy(scheduleNote = action.note) }

            is CreateHousingPostAction.ChangeContactPhone ->
                _uiState.update { it.copy(contactPhone = action.phone, validationError = null) }

            is CreateHousingPostAction.ChangeContactZalo ->
                _uiState.update { it.copy(contactZalo = action.zalo) }

            is CreateHousingPostAction.ChangeAllowZalo ->
                _uiState.update { it.copy(allowZalo = action.allowed) }

            else -> Unit
        }
    }

    private fun handlePreferenceChange(action: CreateHousingPostAction) {
        when (action) {
            is CreateHousingPostAction.ChangeUtilitiesIncluded ->
                _uiState.update { it.copy(utilitiesIncluded = action.included) }

            is CreateHousingPostAction.ChangeSlots ->
                _uiState.update { it.copy(slots = action.slots.coerceAtLeast(1)) }

            is CreateHousingPostAction.ChangeGender ->
                _uiState.update { it.copy(genderPreference = action.gender) }

            is CreateHousingPostAction.ToggleAmenity -> _uiState.update { state ->
                val updated = if (state.selectedAmenities.contains(action.amenity)) {
                    state.selectedAmenities - action.amenity
                } else {
                    state.selectedAmenities + action.amenity
                }
                state.copy(selectedAmenities = updated)
            }

            else -> Unit
        }
    }

    private fun handlePhotoChange(action: CreateHousingPostAction) {
        when (action) {
            is CreateHousingPostAction.AddPhoto -> _uiState.update { state ->
                // Chặn vượt trần ảnh để lưới ảnh và ô "Tối đa 6" không bị vỡ.
                if (state.photos.size >= MAX_HOUSING_PHOTOS) {
                    state
                } else {
                    state.copy(photos = state.photos + action.uri)
                }
            }

            is CreateHousingPostAction.RemovePhoto -> _uiState.update { state ->
                state.copy(photos = state.photos.filterIndexed { idx, _ -> idx != action.index })
            }

            else -> Unit
        }
    }

    /** Lưu nháp giữ nguyên trạng thái form trong phiên (chưa có backend nháp) và báo cho Route. */
    private fun saveDraft() {
        viewModelScope.launch { _events.send(CreateHousingPostEvent.DraftSaved) }
    }

    private fun submitPost() {
        val state = _uiState.value
        val profile = sessions.observe().value.profile
        val error = validationError(state, profile)
        if (error != null) {
            _uiState.update { it.copy(validationError = error) }
            return
        }
        val host = requireNotNull(profile) { "profile đã được validationError kiểm tra" }
        val rent = requireNotNull(parseAmount(state.rentInput)) { "giá thuê đã được validationError kiểm tra" }
        val deposit = parseAmount(state.depositInput) ?: 0
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = createListing(buildListing(state, host, rent, deposit))
            result.fold(
                onSuccess = { _events.send(CreateHousingPostEvent.PostCreated) },
                onFailure = { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CreateHousingPostEvent.SubmitFailed(ServerErrorCode.of(error)))
                },
            )
        }
    }

    /** Trả lỗi đầu tiên gặp phải; tách riêng để [submitPost] gọn và không vượt ngưỡng ReturnCount. */
    private fun validationError(
        state: CreateHousingPostUiState,
        profile: StudentProfile?,
    ): CreateHousingPostError? = when {
        profile == null -> CreateHousingPostError.SIGN_IN_REQUIRED
        state.title.isBlank() -> CreateHousingPostError.EMPTY_TITLE
        parseAmount(state.rentInput) == null -> CreateHousingPostError.INVALID_RENT
        state.address.isBlank() -> CreateHousingPostError.EMPTY_ADDRESS
        state.contactPhone.isBlank() -> CreateHousingPostError.EMPTY_PHONE
        else -> null
    }

    private fun buildListing(
        state: CreateHousingPostUiState,
        host: StudentProfile,
        rent: Int,
        deposit: Int,
    ): HousingListing = HousingListing(
        type = state.type,
        campus = state.campus,
        title = state.title.trim(),
        description = state.description.trim(),
        hostName = host.displayName,
        hostStudentId = host.studentId,
        contactPhone = state.contactPhone.trim(),
        contactZaloUrl = if (state.allowZalo) {
            state.contactZalo.trim().ifBlank { "https://zalo.me/${state.contactPhone.trim()}" }
        } else {
            ""
        },
        images = state.photos,
        monthlyRentVnd = rent,
        depositVnd = deposit,
        utilitiesIncluded = state.utilitiesIncluded,
        availableSlots = state.slots,
        genderPreference = state.genderPreference,
        scheduleNote = state.scheduleNote.trim(),
        amenities = state.selectedAmenities.toList(),
        addressLabel = state.address.trim(),
        distanceToCampusM = (state.distanceInput.replace(',', '.').toFloatOrNull()?.times(METERS_PER_KM))?.toInt() ?: 0,
        areaM2 = state.areaInput.filter { it.isDigit() }.toIntOrNull(),
        latitude = 0.0,
        longitude = 0.0,
        status = HousingListingStatus.OPEN,
        verified = host.uthVerified,
        verificationMethod = if (host.uthVerified) HostVerificationMethod.EMAIL_UTH else null,
    )

    /** Chỉ nhận chữ số, bỏ dấu phân cách người dùng gõ — `null` khi không phải số dương. */
    private fun parseAmount(input: String): Int? = input.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }

    private companion object {
        /** Ô khoảng cách nhập theo km, model lưu mét. */
        const val METERS_PER_KM = 1000
    }
}

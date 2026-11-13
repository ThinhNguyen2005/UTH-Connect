package com.giathinh.uthconnect.presentation.housing.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giathinh.uthconnect.domain.model.HostVerificationMethod
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.ZaloLink
import com.giathinh.uthconnect.domain.repository.HousingDraftStore
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.CreateHousingListingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

sealed interface CreateHousingPostEvent {
    data object PostCreated : CreateHousingPostEvent
    data class SubmitFailed(val code: ServerErrorCode, val detail: String? = null) : CreateHousingPostEvent
    data object DraftSaved : CreateHousingPostEvent
    data object DraftSaveFailed : CreateHousingPostEvent
    data object DraftRestored : CreateHousingPostEvent
}

/** Nháp gắn mã sinh viên sở hữu để tài khoản khác trên cùng máy không đọc được nháp của nhau. */
@Serializable
internal data class HousingDraftEnvelope(
    val ownerStudentId: String,
    val state: CreateHousingPostUiState,
)

/** Khớp check constraint housing_posts_title_check / area_m2 của backend. */
private const val MIN_TITLE_LENGTH = 5
private const val MAX_TITLE_LENGTH = 150

@HiltViewModel
class CreateHousingPostViewModel @Inject constructor(
    private val createListing: CreateHousingListingUseCase,
    private val sessions: AccessStudentSessionUseCase,
    private val draftStore: HousingDraftStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateHousingPostUiState())
    val uiState: StateFlow<CreateHousingPostUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateHousingPostEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Form đã bị chạm thì nháp đọc chậm từ DataStore không được đè lên thao tác thật. */
    private var formTouched = false

    /** Mọi sửa đổi form mở danh tính gửi mới — gửi lại sau lỗi chỉ trùng ID khi nội dung giữ nguyên. */
    private fun markFormEdited() {
        formTouched = true
        if (_uiState.value.submitId.isNotEmpty()) _uiState.update { it.copy(submitId = "") }
    }

    private val json = Json { ignoreUnknownKeys = true }

    private var draftRestore: Job? = null

    init {
        draftRestore = viewModelScope.launch {
            val draft = readDraft()
            if (draft != null && !formTouched) {
                _uiState.update { draft }
                _events.send(CreateHousingPostEvent.DraftRestored)
            }
        }
    }

    /** Chỉ khôi phục nháp đúng chủ tài khoản hiện tại; đọc lỗi thì vẫn mở form bình thường. */
    private suspend fun readDraft(): CreateHousingPostUiState? = try {
        val owner = sessions.observe().value.profile?.studentId ?: return null
        draftStore.read(owner)?.let { payload ->
            runCatching { json.decodeFromString<HousingDraftEnvelope>(payload) }.getOrNull()
        }?.takeIf { it.ownerStudentId == owner }?.state
    } catch (ignored: IOException) {
        null
    }

    fun onAction(action: CreateHousingPostAction) {
        when (action) {
            is CreateHousingPostAction.ChangeType,
            is CreateHousingPostAction.ChangeCampus,
            is CreateHousingPostAction.ChangeTitle,
            is CreateHousingPostAction.ChangeDescription,
            is CreateHousingPostAction.ChangeRent,
            is CreateHousingPostAction.ChangeDeposit,
            is CreateHousingPostAction.ChangeAddress,
            is CreateHousingPostAction.ChangeLocation,
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
        markFormEdited()
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
                _uiState.update { it.copy(depositInput = action.deposit, validationError = null) }

            is CreateHousingPostAction.ChangeAddress ->
                // Sửa địa chỉ tay thì pin cũ không còn khớp — xóa tọa độ để khỏi gửi vị trí sai.
                _uiState.update {
                    it.copy(address = action.address, latitude = 0.0, longitude = 0.0, validationError = null)
                }

            is CreateHousingPostAction.ChangeLocation -> _uiState.update {
                it.copy(
                    latitude = action.latitude,
                    longitude = action.longitude,
                    address = action.address.ifBlank { it.address },
                    validationError = null,
                )
            }

            is CreateHousingPostAction.ChangeArea ->
                _uiState.update { it.copy(areaInput = action.area) }

            is CreateHousingPostAction.ChangeScheduleNote ->
                _uiState.update { it.copy(scheduleNote = action.note) }

            is CreateHousingPostAction.ChangeContactPhone ->
                _uiState.update { it.copy(contactPhone = action.phone, validationError = null) }

            is CreateHousingPostAction.ChangeContactZalo ->
                _uiState.update { it.copy(contactZalo = action.zalo, validationError = null) }

            is CreateHousingPostAction.ChangeAllowZalo ->
                _uiState.update { it.copy(allowZalo = action.allowed, validationError = null) }

            else -> Unit
        }
    }

    private fun handlePreferenceChange(action: CreateHousingPostAction) {
        markFormEdited()
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
        markFormEdited()
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

    /** Lưu nháp xuống bộ nhớ máy để tắt app mở lại vẫn còn; đăng thành công thì xóa. */
    private fun saveDraft() {
        viewModelScope.launch {
            // Chờ khôi phục xong để lệnh lưu không ghi đè nháp cũ khi DataStore đọc chậm.
            draftRestore?.join()
            val owner = sessions.observe().value.profile?.studentId ?: return@launch
            try {
                draftStore.write(owner, json.encodeToString(HousingDraftEnvelope(owner, _uiState.value)))
                _events.send(CreateHousingPostEvent.DraftSaved)
            } catch (ignored: IOException) {
                _events.send(CreateHousingPostEvent.DraftSaveFailed)
            }
        }
    }

    /** Lỗi xóa nháp không được biến thao tác đăng tin thành công thành thất bại; chỉ xóa nháp đúng chủ. */
    private suspend fun clearDraftQuietly(owner: String) {
        try {
            draftStore.clear(owner)
        } catch (ignored: IOException) {
        }
    }

    private fun submitPost() {
        if (_uiState.value.isSubmitting) return
        val state = _uiState.value
        val profile = sessions.observe().value.profile
        val error = validationError(state, profile)
        if (error != null) {
            _uiState.update { it.copy(validationError = error, validationNonce = it.validationNonce + 1) }
            return
        }
        val host = requireNotNull(profile) { "profile đã được validationError kiểm tra" }
        val rent = requireNotNull(parseAmount(state.rentInput)) { "giá thuê đã được validationError kiểm tra" }
        val deposit = parseDeposit(state.depositInput) ?: 0
        // ID cố định cho lượt gửi này: gửi lại sau lỗi mất response không nhân đôi tin.
        val submitId = state.submitId.ifBlank { UUID.randomUUID().toString() }
        _uiState.update { it.copy(isSubmitting = true, submitId = submitId) }
        viewModelScope.launch {
            val result = createListing(buildListing(state, host, rent, deposit, submitId))
            result.fold(
                onSuccess = {
                    clearDraftQuietly(host.studentId)
                    // Giữ khóa submit tới khi Route điều hướng rời màn — nhả khóa ở đây
                    // sẽ mở cửa đăng tin thứ hai trong lúc snackbar thành công còn hiện.
                    _events.send(CreateHousingPostEvent.PostCreated)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CreateHousingPostEvent.SubmitFailed(ServerErrorCode.of(error), error.message))
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

        state.title.trim().length !in MIN_TITLE_LENGTH..MAX_TITLE_LENGTH ->
            CreateHousingPostError.INVALID_TITLE_LENGTH

        parseAmount(state.rentInput) == null -> CreateHousingPostError.INVALID_RENT

        state.depositInput.isNotBlank() && parseDeposit(state.depositInput) == null ->
            CreateHousingPostError.INVALID_DEPOSIT

        state.areaInput.isNotBlank() && parseArea(state.areaInput) == null -> CreateHousingPostError.INVALID_AREA

        state.address.isBlank() -> CreateHousingPostError.EMPTY_ADDRESS

        state.contactPhone.isBlank() -> CreateHousingPostError.EMPTY_PHONE

        state.allowZalo && !ZaloLink.isValid(zaloUrlOf(state)) -> CreateHousingPostError.INVALID_ZALO

        else -> null
    }

    private fun buildListing(
        state: CreateHousingPostUiState,
        host: StudentProfile,
        rent: Int,
        deposit: Int,
        submitId: String,
    ): HousingListing = HousingListing(
        id = submitId,
        type = state.type,
        campus = state.campus,
        title = state.title.trim(),
        description = state.description.trim(),
        hostName = host.displayName,
        hostStudentId = host.studentId,
        contactPhone = state.contactPhone.trim(),
        contactZaloUrl = if (state.allowZalo) zaloUrlOf(state) else "",
        contactZaloEnabled = state.allowZalo,
        images = state.photos,
        monthlyRentVnd = rent,
        depositVnd = deposit,
        utilitiesIncluded = state.utilitiesIncluded,
        availableSlots = state.slots,
        genderPreference = state.genderPreference,
        scheduleNote = state.scheduleNote.trim(),
        amenities = state.selectedAmenities.toList(),
        addressLabel = state.address.trim(),
        areaM2 = parseArea(state.areaInput),
        latitude = state.latitude,
        longitude = state.longitude,
        status = HousingListingStatus.OPEN,
        verified = host.uthVerified,
        verificationMethod = if (host.uthVerified) HostVerificationMethod.EMAIL_UTH else null,
    )

    /** Chỉ nhận chữ số, bỏ dấu phân cách người dùng gõ — `null` khi không phải số dương. */
    private fun parseAmount(input: String): Int? = input.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }

    /** Tiền cọc khác giá thuê: chấp nhận 0 (không cọc); `null` khi tràn số hoặc không có chữ số. */
    private fun parseDeposit(input: String): Int? = input.filter { it.isDigit() }.toIntOrNull()

    /**
     * Ô Zalo nhận link HTTPS zalo.me hoặc SĐT thuần (ô nhập ghi "Link Zalo hoặc
     * SĐT Zalo"); SĐT được ghép thành link đúng quy tắc fallback của module.
     */
    private fun zaloUrlOf(state: CreateHousingPostUiState): String {
        val input = state.contactZalo.trim()
        return when {
            input.isBlank() -> ZaloLink.forPhone(state.contactPhone)
            input.all(Char::isDigit) -> ZaloLink.forPhone(input)
            else -> input
        }
    }

    /** Cột area_m2 là số nguyên: từ chối "25.5"/"25,5" thay vì nối chữ số như [parseAmount]. */
    private fun parseArea(input: String): Int? {
        val trimmed = input.trim()
        return trimmed.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toIntOrNull()?.takeIf { it > 0 }
    }
}

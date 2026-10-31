package com.giathinh.uthconnect.presentation.announcement

import androidx.lifecycle.SavedStateHandle
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import com.giathinh.uthconnect.domain.usecase.GetAnnouncementDetailUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Test dọc theo khung MVVM: ViewModel + UseCase + Repository giả (`docs/CODING_STANDARDS.md`
 * mục 9). Không mạng, không thiết bị, không `sleep`.
 *
 * `announcementId` đi vào ViewModel qua [SavedStateHandle] đúng như Navigation ghi tham số của
 * route type-safe, nên test không cần dựng NavController.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnnouncementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val announcementId = "tin-dang-ky-hoc-phan"

    private val detail = AnnouncementDetail(
        id = announcementId,
        category = AnnouncementCategory.TRAINING,
        title = "Kế hoạch đăng ký học phần",
        publisher = "Phòng Đào tạo",
        publishedAt = LocalDateTime.of(2026, 10, 28, 9, 0),
        lead = "Cổng đăng ký mở từ 08:00 ngày 01/11.",
    )

    @Test
    fun `mở màn hình - nội dung đến từ repository, loading tắt đi`() {
        val viewModel = viewModel(FakeRepository(Result.success(detail)))

        val state = viewModel.uiState.value
        assertEquals(detail, state.detail)
        assertFalse("đã tải xong thì isLoading phải tắt", state.isLoading)
        assertNull(state.failure)
        assertFalse("có nội dung thì không phải missing", state.isMissing)
    }

    @Test
    fun `repository thất bại - UiState báo LoadFailed, không giả danh bài bị gỡ`() {
        val viewModel = viewModel(FakeRepository(Result.failure(IllegalStateException("mất mạng"))))

        val state = viewModel.uiState.value
        assertEquals(AnnouncementFailure.LoadFailed, state.failure)
        assertFalse(state.isLoading)
        assertFalse("lỗi đọc khác với không tìm thấy", state.isMissing)
    }

    @Test
    fun `repository trả null - bài không còn tồn tại, không phải lỗi`() {
        val viewModel = viewModel(FakeRepository(Result.success(null)))

        val state = viewModel.uiState.value
        assertTrue("null = không tìm thấy", state.isMissing)
        assertNull(state.failure)
        assertNull(state.detail)
    }

    @Test
    fun `action Retry - ViewModel nạp lại và xoá failure`() {
        val repository = FakeRepository(Result.failure(IllegalStateException("mất mạng")))
        val viewModel = viewModel(repository)
        assertEquals(AnnouncementFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.result = Result.success(detail)
        viewModel.onAction(AnnouncementAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(detail, state.detail)
        assertNull("Retry thành công thì failure phải bị xoá", state.failure)
    }

    /**
     * Trạng thái khởi đầu phải là loading chứ **không** phải missing — nếu đổi lại, UI sẽ nháy
     * "Thông báo không còn tồn tại" trước khi dữ liệu về.
     */
    @Test
    fun `UiState mặc định là loading, không phải missing`() {
        val initial = AnnouncementUiState()

        assertTrue(initial.isLoading)
        assertFalse("đang tải thì chưa được coi là bài bị gỡ", initial.isMissing)
    }

    /** Bài ngắn (chỉ tiêu đề) không được ghi "0 phút đọc". */
    @Test
    fun `thời lượng đọc luôn ít nhất 1 phút`() {
        val shortest = detail.copy(title = "Nghỉ lễ", lead = null)

        assertEquals(1, shortest.readMinutes)
        assertTrue("bài có nội dung luôn đọc được ít nhất 1 phút", detail.readMinutes >= 1)
    }

    private fun viewModel(repository: AnnouncementRepository) = AnnouncementViewModel(
        getAnnouncementDetail = GetAnnouncementDetailUseCase(repository),
        savedStateHandle = SavedStateHandle(mapOf("announcementId" to announcementId)),
    )

    private class FakeRepository(
        var result: Result<AnnouncementDetail?>,
    ) : AnnouncementRepository {

        override suspend fun homeFeed(): Result<HomeFeed> =
            Result.failure(UnsupportedOperationException("màn hình Chi tiết không dùng bảng tin"))

        override suspend fun detail(id: String): Result<AnnouncementDetail?> = result
    }
}

package com.giathinh.uthconnect.presentation.home

import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.HomeBanner
import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import com.giathinh.uthconnect.domain.usecase.GetHomeFeedUseCase
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
 * Test dọc theo khung MVVM: ViewModel + UseCase + Repository giả (fake, không MockK —
 * `docs/CODING_STANDARDS.md` mục 9). Không mạng, không thiết bị, không `sleep`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val announcements = listOf(
        announcement(id = "a1", category = AnnouncementCategory.TRAINING, unread = true),
        announcement(id = "a2", category = AnnouncementCategory.TUITION, unread = false),
    )

    private val feed = HomeFeed(
        banners = listOf(HomeBanner(announcement = announcements.first())),
        latest = announcements,
    )

    @Test
    fun `mở tab - bảng tin và banner đến từ repository, loading tắt đi`() {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(FakeRepository(feed)))

        val state = viewModel.uiState.value
        assertEquals(announcements, state.announcements)
        assertEquals(feed.banners, state.banners)
        assertEquals(1, state.unreadCount)
        assertFalse("đã tải xong thì isLoading phải tắt", state.isLoading)
        assertNull(state.failure)
    }

    @Test
    fun `repository thất bại - UiState báo LoadFailed, không giả danh bảng tin rỗng`() {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(FakeRepository(feed, failed = true)))

        val state = viewModel.uiState.value
        assertEquals(HomeFailure.LoadFailed, state.failure)
        assertFalse("đã lỗi thì isLoading phải tắt", state.isLoading)
        assertTrue(state.announcements.isEmpty())
        assertFalse("có lỗi thì không được coi là rỗng", state.isEmpty)
    }

    @Test
    fun `action Retry - ViewModel nạp lại và xoá failure`() {
        val repository = FakeRepository(feed, failed = true)
        val viewModel = HomeViewModel(GetHomeFeedUseCase(repository))
        assertEquals(HomeFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.failed = false
        viewModel.onAction(HomeAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(announcements, state.announcements)
        assertNull("Retry thành công thì failure phải bị xoá", state.failure)
    }

    /**
     * Trạng thái khởi đầu phải là loading chứ **không** phải empty — nếu đổi lại, UI sẽ nháy
     * "Chưa có thông báo nào" trước khi dữ liệu về.
     */
    @Test
    fun `UiState mặc định là loading, không phải empty`() {
        val initial = HomeUiState()

        assertTrue(initial.isLoading)
        assertFalse("đang tải thì chưa được coi là rỗng", initial.isEmpty)
    }

    @Test
    fun `action SelectCategory - chỉ còn thông báo của nhóm được chọn`() {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(FakeRepository(feed)))

        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.TUITION))

        val state = viewModel.uiState.value
        assertEquals(AnnouncementCategory.TUITION, state.selectedCategory)
        assertEquals(listOf("a2"), state.visibleAnnouncements.map { it.id })
        assertEquals("danh sách gốc vẫn giữ nguyên để bỏ lọc", announcements, state.announcements)
    }

    @Test
    fun `action SelectCategory null - bỏ lọc, trả lại toàn bộ bảng tin`() {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(FakeRepository(feed)))
        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.TRAINING))

        viewModel.onAction(HomeAction.SelectCategory(null))

        val state = viewModel.uiState.value
        assertNull(state.selectedCategory)
        assertEquals(announcements, state.visibleAnnouncements)
    }

    /** Nhóm đang lọc không có thông báo nào → empty, để UI hiện lối thoát khỏi bộ lọc. */
    @Test
    fun `nhóm được chọn không có thông báo - UiState báo empty`() {
        val viewModel = HomeViewModel(GetHomeFeedUseCase(FakeRepository(feed)))

        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.YOUTH_UNION))

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    private fun announcement(
        id: String,
        category: AnnouncementCategory,
        unread: Boolean,
    ) = Announcement(
        id = id,
        category = category,
        title = "Tiêu đề $id",
        publisher = "Phòng ban",
        publishedAt = LocalDateTime.of(2026, 10, 29, 8, 30),
        isUnread = unread,
    )

    private class FakeRepository(
        private val feed: HomeFeed,
        var failed: Boolean = false,
    ) : AnnouncementRepository {
        override suspend fun homeFeed(): Result<HomeFeed> =
            if (failed) Result.failure(IllegalStateException("mất mạng")) else Result.success(feed)

        // Tab Trang chủ không mở chi tiết — hành vi của màn Chi tiết có test riêng.
        override suspend fun detail(id: String): Result<AnnouncementDetail?> =
            Result.success(null)
    }
}

package com.giathinh.uthconnect.presentation.home

import com.giathinh.uthconnect.domain.model.Announcement
import com.giathinh.uthconnect.domain.model.AnnouncementCategory
import com.giathinh.uthconnect.domain.model.AnnouncementDetail
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventDraft
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HomeBanner
import com.giathinh.uthconnect.domain.model.HomeFeed
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.AnnouncementRepository
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import com.giathinh.uthconnect.domain.repository.EventRepository
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import com.giathinh.uthconnect.domain.usecase.GetHomeFeedUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

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

    private val sampleEvents = listOf(
        event(id = "e1", campus = "Cơ sở 1", status = EventStatus.PUBLISHED, offsetHours = 24),
        event(id = "e2", campus = "Cơ sở 2", status = EventStatus.HAPPENING, offsetHours = 2),
        event(id = "e3_draft", campus = "Cơ sở 1", status = EventStatus.DRAFT, offsetHours = 48),
        event(id = "e4_cancelled", campus = "Cơ sở 1", status = EventStatus.CANCELLED, offsetHours = 12),
    )

    @Test
    fun `mở tab - bảng tin và banner đến từ repository, loading tắt đi`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(),
        )

        val state = viewModel.uiState.value
        assertEquals(announcements, state.announcements)
        assertEquals(feed.banners, state.banners)
        assertEquals(1, state.unreadCount)
        assertFalse("đã tải xong thì isLoading phải tắt", state.isLoading)
        assertNull(state.failure)
    }

    @Test
    fun `repository thất bại - UiState báo LoadFailed, không giả danh bảng tin rỗng`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed, failed = true)),
            eventRepository = FakeEventRepository(),
        )

        val state = viewModel.uiState.value
        assertEquals(HomeFailure.LoadFailed, state.failure)
        assertFalse("đã lỗi thì isLoading phải tắt", state.isLoading)
        assertTrue(state.announcements.isEmpty())
        assertFalse("có lỗi thì không được coi là rỗng", state.isEmpty)
    }

    @Test
    fun `action Retry - ViewModel nạp lại và xoá failure`() {
        val repository = FakeRepository(feed, failed = true)
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(repository),
            eventRepository = FakeEventRepository(),
        )
        assertEquals(HomeFailure.LoadFailed, viewModel.uiState.value.failure)

        repository.failed = false
        viewModel.onAction(HomeAction.Retry)

        val state = viewModel.uiState.value
        assertEquals(announcements, state.announcements)
        assertNull("Retry thành công thì failure phải bị xoá", state.failure)
    }

    @Test
    fun `UiState mặc định là loading, không phải empty`() {
        val initial = HomeUiState()

        assertTrue(initial.isLoading)
        assertFalse("đang tải thì chưa được coi là rỗng", initial.isEmpty)
    }

    @Test
    fun `action SelectCategory - chỉ còn thông báo của nhóm được chọn`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(),
        )

        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.TUITION))

        val state = viewModel.uiState.value
        assertEquals(AnnouncementCategory.TUITION, state.selectedCategory)
        assertEquals(listOf("a2"), state.visibleAnnouncements.map { it.id })
        assertEquals("danh sách gốc vẫn giữ nguyên để bỏ lọc", announcements, state.announcements)
    }

    @Test
    fun `action SelectCategory null - bỏ lọc, trả lại toàn bộ bảng tin`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(),
        )
        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.TRAINING))

        viewModel.onAction(HomeAction.SelectCategory(null))

        val state = viewModel.uiState.value
        assertNull(state.selectedCategory)
        assertEquals(announcements, state.visibleAnnouncements)
    }

    @Test
    fun `nhóm được chọn không có thông báo - UiState báo empty khi không có sự kiện`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(initialEvents = emptyList()),
        )

        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.YOUTH_UNION))

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `sự kiện nạp từ repository và tuân thủ bộ lọc cơ sở`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(initialEvents = sampleEvents),
        )

        val state = viewModel.uiState.value
        assertEquals(sampleEvents, state.events)

        // Ban đầu (Tất cả cơ sở): chỉ thấy e1 (CS1) và e2 (CS2), loại trừ DRAFT (e3) và CANCELLED (e4)
        val initialVisible = state.visibleEvents
        assertEquals(listOf("e2", "e1"), initialVisible.map { it.id })

        // Lọc Cơ sở 1: chỉ còn e1
        viewModel.onAction(HomeAction.SelectCampus("Cơ sở 1"))
        val cs1Events = viewModel.uiState.value.visibleEvents
        assertEquals(listOf("e1"), cs1Events.map { it.id })

        // Bỏ lọc cơ sở (null): trả lại cả hai
        viewModel.onAction(HomeAction.SelectCampus(null))
        assertEquals(listOf("e2", "e1"), viewModel.uiState.value.visibleEvents.map { it.id })
    }

    @Test
    fun `lọc category khác YOUTH_UNION ẩn sự kiện để nhường cho bảng tin chuyên mục`() {
        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(initialEvents = sampleEvents),
        )

        // Khi chọn TUITION (học phí): visibleEvents rỗng
        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.TUITION))
        assertTrue(viewModel.uiState.value.visibleEvents.isEmpty())

        // Khi chọn YOUTH_UNION (Đoàn - Hội): visibleEvents xuất hiện
        viewModel.onAction(HomeAction.SelectCategory(AnnouncementCategory.YOUTH_UNION))
        assertFalse(viewModel.uiState.value.visibleEvents.isEmpty())
    }

    @Test
    fun `mở tab - nạp thành công các bài đăng tiện ích cộng đồng`() {
        val housing = sampleHousing(id = "h1", status = HousingListingStatus.OPEN)
        val book = sampleBook(id = "b1", status = BookListingStatus.AVAILABLE)
        val lost = sampleLostFound(id = "l1", isResolved = false)

        val viewModel = HomeViewModel(
            getHomeFeed = GetHomeFeedUseCase(FakeRepository(feed)),
            eventRepository = FakeEventRepository(),
            housingRepository = FakeHousingRepository(listOf(housing)),
            bookListingRepository = FakeBookListingRepository(listOf(book)),
            lostFoundRepository = FakeLostFoundRepository(listOf(lost)),
        )

        assertEquals(listOf("h1"), viewModel.uiState.value.visibleHousingListings.map { it.id })
        assertEquals(listOf("b1"), viewModel.uiState.value.visibleBookListings.map { it.id })
        assertEquals(listOf("l1"), viewModel.uiState.value.visibleLostFoundListings.map { it.id })
    }

    private fun sampleHousing(id: String, status: HousingListingStatus) = HousingListing(
        id = id,
        type = HousingListingType.ROOM_FOR_RENT,
        campus = UthCampus.CS1_QUANG_TRUNG,
        title = "Phòng trọ $id",
        description = "Mô tả",
        hostName = "Thịnh",
        monthlyRentVnd = 2000000,
        depositVnd = 2000000,
        utilitiesIncluded = true,
        availableSlots = 1,
        genderPreference = GenderPreference.ANY,
        scheduleNote = "",
        addressLabel = "123 Đường Số 1",
        latitude = 10.8,
        longitude = 106.7,
        status = status,
    )

    private fun sampleBook(id: String, status: BookListingStatus) = BookListing(
        id = id,
        title = "Sách $id",
        subject = "Toán",
        condition = BookCondition.GOOD,
        dealType = BookDealType.SALE,
        priceVnd = 50000L,
        description = "Mô tả sách",
        sellerName = "Thịnh",
        contact = "0900000000",
        status = status,
    )

    private fun sampleLostFound(id: String, isResolved: Boolean) = LostFoundListing(
        id = id,
        title = "Đồ thất lạc $id",
        description = "Mô tả đồ",
        kind = ListingKind.LOST,
        category = ItemCategory.STUDENT_CARD,
        location = "CS1",
        date = "23/11/2026",
        contactName = "Thịnh",
        contactPhone = "0900000000",
        isResolved = isResolved,
    )

    private class FakeHousingRepository(
        private val listings: List<HousingListing> = emptyList(),
    ) : HousingRepository {
        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(listings)
        override suspend fun getMyListings(): Result<List<HousingListing>> = Result.success(listings)
        override suspend fun getListingById(id: String): Result<HousingListing> = Result.success(listings.first())
        override suspend fun createListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)
        override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)
        override suspend fun markListingFulfilled(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeBookListingRepository(
        private val listings: List<BookListing> = emptyList(),
    ) : BookListingRepository {
        override fun currentUserId(): String? = "u1"
        override fun observeListings(): Flow<List<BookListing>> = flowOf(listings)
        override suspend fun saveListing(listing: BookListing): Result<Unit> = Result.success(Unit)
        override suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit> = Result.success(Unit)
        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)
        override fun refresh() = Unit
    }

    private class FakeLostFoundRepository(
        private val listings: List<LostFoundListing> = emptyList(),
    ) : LostFoundRepository {
        override suspend fun getListings(): Result<List<LostFoundListing>> = Result.success(listings)
        override suspend fun reportItem(
            item: LostFoundListing,
            imageBytes: ByteArray?,
            imageMimeType: String?,
        ): Result<LostFoundListing> = Result.success(item)
        override suspend fun updateItem(
            item: LostFoundListing,
            imageBytes: ByteArray?,
            imageMimeType: String?,
            removeExistingImage: Boolean,
        ): Result<LostFoundListing> = Result.success(item)
        override suspend fun deleteItem(
            id: String,
            imageUrl: String?,
        ): Result<Unit> = Result.success(Unit)
        override suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit> = Result.success(Unit)
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

    private fun event(
        id: String,
        campus: String,
        status: EventStatus,
        offsetHours: Long,
    ): Event {
        val now = System.currentTimeMillis()
        val startAt = now + offsetHours * 3600 * 1000
        val endAt = startAt + 7200 * 1000
        return Event(
            id = id,
            title = "Sự kiện $id",
            description = "Mô tả",
            orgId = "org1",
            orgName = "Ban Tổ Chức",
            location = "Hội trường",
            campus = campus,
            startAt = startAt,
            endAt = endAt,
            capacity = 100,
            registeredCount = 10,
            status = status,
            points = 5,
        )
    }

    private class FakeRepository(
        private val feed: HomeFeed,
        var failed: Boolean = false,
    ) : AnnouncementRepository {
        override suspend fun homeFeed(): Result<HomeFeed> = if (failed) Result.failure(IllegalStateException("mất mạng")) else Result.success(feed)

        // Tab Trang chủ không mở chi tiết — hành vi của màn Chi tiết có test riêng.
        override suspend fun detail(id: String): Result<AnnouncementDetail?> = Result.success(null)
    }

    private class FakeEventRepository(
        initialEvents: List<Event> = emptyList(),
        initialRegistrations: Set<String> = emptySet(),
    ) : EventRepository {
        val eventsFlow = MutableStateFlow(initialEvents)
        val registrationsFlow = MutableStateFlow(initialRegistrations)

        override fun getEvents(campus: String?): Flow<List<Event>> = eventsFlow
        override fun currentUserRegistrations(): Flow<Set<String>> = registrationsFlow
        override suspend fun getEventDetail(eventId: String): Result<Event?> = Result.success(null)
        override suspend fun registerEvent(
            eventId: String,
            uid: String,
            studentId: String,
            studentName: String,
        ): Result<Unit> = Result.success(Unit)
        override suspend fun cancelRegistration(eventId: String, uid: String): Result<Unit> = Result.success(Unit)
        override fun isRegistered(eventId: String, uid: String): Flow<Boolean> = flowOf(false)
        override suspend fun registerCurrentUser(eventId: String): Result<Unit> = Result.success(Unit)
        override suspend fun cancelCurrentRegistration(eventId: String): Result<Unit> = Result.success(Unit)
        override fun refresh() = Unit
        override fun currentUserId(): String? = "test_user"
        override suspend fun saveEvent(eventId: String?, draft: EventDraft): Result<Unit> = Result.success(Unit)
        override suspend fun setStatus(eventId: String, status: EventStatus): Result<Unit> = Result.success(Unit)
    }
}

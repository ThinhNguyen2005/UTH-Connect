package com.giathinh.uthconnect.presentation.lostfound

import com.giathinh.uthconnect.data.repository.InMemoryLostFoundRepository
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import com.giathinh.uthconnect.domain.usecase.GetLostFoundListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportLostFoundItemUseCase
import com.giathinh.uthconnect.domain.usecase.ToggleResolveListingUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LostFoundViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /**
     * Verifies `createViewModel` behavior and invariants.
     */
    private fun createViewModel(
        repository: LostFoundRepository = InMemoryLostFoundRepository(),
    ): LostFoundViewModel = LostFoundViewModel(
        getListingsUseCase = GetLostFoundListingsUseCase(repository),
        reportItemUseCase = ReportLostFoundItemUseCase(repository),
        toggleResolveListingUseCase = ToggleResolveListingUseCase(repository),
    )

    /**
     * Verifies `initialState_loadsListingsSuccessfully` behavior and invariants.
     */
    @Test
    fun `initialState_loadsListingsSuccessfully`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertFalse("isLoading phải là false sau khi tải xong", state.isLoading)
        assertFalse("isError phải là false khi tải thành công", state.isError)
        assertEquals("Phải có 8 bài đăng mặc định", 8, state.listings.size)
        assertEquals("Mặc định chọn tab LOST", ListingKind.LOST, state.selectedKind)
        assertEquals("Mặc định chọn category ALL", ItemCategory.ALL, state.selectedCategory)
        assertEquals("Số lượng bài LOST", 4, state.lostCount)
        assertEquals("Số lượng bài FOUND", 4, state.foundCount)
        assertEquals("Danh sách lọc mặc định chỉ gồm bài LOST", 4, state.filteredListings.size)
        assertTrue(state.filteredListings.all { it.kind == ListingKind.LOST })
    }

    /**
     * Verifies `selectKindAndCategory_filtersListingsCorrectly` behavior and invariants.
     */
    @Test
    fun `selectKindAndCategory_filtersListingsCorrectly`() {
        val viewModel = createViewModel()

        // Chuyển sang tab FOUND
        viewModel.onAction(LostFoundAction.SelectKind(ListingKind.FOUND))
        assertEquals(ListingKind.FOUND, viewModel.uiState.value.selectedKind)
        assertEquals(4, viewModel.uiState.value.filteredListings.size)
        assertTrue(viewModel.uiState.value.filteredListings.all { it.kind == ListingKind.FOUND })

        // Lọc theo danh mục Đồ điện tử
        viewModel.onAction(LostFoundAction.SelectCategory(ItemCategory.ELECTRONICS))
        assertEquals(ItemCategory.ELECTRONICS, viewModel.uiState.value.selectedCategory)
        assertEquals(1, viewModel.uiState.value.filteredListings.size)
        val firstTitle = viewModel.uiState.value.filteredListings.first().title
        assertEquals("Nhặt được tai nghe AirPods Pro trong hộp màu xanh", firstTitle)

        // Reset về tất cả danh mục
        viewModel.onAction(LostFoundAction.SelectCategory(ItemCategory.ALL))
        assertEquals(4, viewModel.uiState.value.filteredListings.size)
    }

    /**
     * Verifies `updateSearchQuery_filtersListingsCorrectly` behavior and invariants.
     */
    @Test
    fun `updateSearchQuery_filtersListingsCorrectly`() {
        val viewModel = createViewModel(InMemoryLostFoundRepository())

        // Mặc định tab LOST (có 4 bài)
        assertEquals(4, viewModel.uiState.value.filteredListings.size)

        // Tìm từ khóa "MacBook"
        viewModel.onAction(LostFoundAction.UpdateSearchQuery("MacBook"))
        assertEquals("MacBook", viewModel.uiState.value.searchQuery)
        assertEquals(1, viewModel.uiState.value.filteredListings.size)
        assertTrue(viewModel.uiState.value.filteredListings.first().title.contains("MacBook"))

        // Tìm theo địa điểm "Võ Oanh"
        viewModel.onAction(LostFoundAction.UpdateSearchQuery("Võ Oanh"))
        assertEquals(1, viewModel.uiState.value.filteredListings.size)
        assertTrue(viewModel.uiState.value.filteredListings.first().location.contains("Võ Oanh"))

        // Tìm từ khóa không tồn tại
        viewModel.onAction(LostFoundAction.UpdateSearchQuery("xe dap the thao 999"))
        assertEquals(0, viewModel.uiState.value.filteredListings.size)

        // Xóa tìm kiếm -> quay về đủ 4 bài
        viewModel.onAction(LostFoundAction.UpdateSearchQuery(""))
        assertEquals(4, viewModel.uiState.value.filteredListings.size)
    }

    /**
     * Verifies `toggleResolveListing_switchesResolvedStatus` behavior and invariants.
     */
    @Test
    fun `toggleResolveListing_switchesResolvedStatus`() {
        val viewModel = createViewModel(InMemoryLostFoundRepository())
        val firstItem = viewModel.uiState.value.listings.first()
        val firstId = firstItem.id
        assertFalse("Ban đầu chưa được resolve", firstItem.isResolved)

        // Bấm đánh dấu đã nhận lại
        viewModel.onAction(LostFoundAction.ToggleResolveListing(firstId))
        val resolvedItem = viewModel.uiState.value.listings.first { it.id == firstId }
        assertTrue("Sau khi toggle, isResolved phải là true", resolvedItem.isResolved)

        // Bấm toggle lại để hủy đánh dấu
        viewModel.onAction(LostFoundAction.ToggleResolveListing(firstId))
        val revertedItem = viewModel.uiState.value.listings.first { it.id == firstId }
        assertFalse("Toggle lần 2 quay lại false", revertedItem.isResolved)
    }

    /**
     * Verifies `toggleResolveListing_unauthorized_revertsOptimisticUpdateAndSetsAuthRequired` behavior.
     */
    @Test
    fun `toggleResolveListing_unauthorized_revertsOptimisticUpdateAndSetsAuthRequired`() {
        val delegate = InMemoryLostFoundRepository()
        val repository = object : LostFoundRepository {
            /**
             * Verifies `getListings` behavior.
             */
            override suspend fun getListings(): Result<List<LostFoundListing>> = delegate.getListings()

            /**
             * Verifies `reportItem` behavior.
             */
            override suspend fun reportItem(
                item: LostFoundListing,
                imageBytes: ByteArray?,
                imageMimeType: String?,
            ): Result<LostFoundListing> = delegate.reportItem(item, imageBytes, imageMimeType)

            /**
             * Verifies `toggleResolved` behavior.
             */
            override suspend fun toggleResolved(
                id: String,
                isResolved: Boolean,
            ): Result<Unit> = Result.failure(
                com.giathinh.uthconnect.data.repository.SupabaseHttpException(
                    java.net.HttpURLConnection.HTTP_UNAUTHORIZED,
                    "Unauthorized",
                ),
            )
        }
        val viewModel = createViewModel(repository)
        val firstId = viewModel.uiState.value.listings.first().id
        assertFalse(viewModel.uiState.value.listings.first().isResolved)

        viewModel.onAction(LostFoundAction.ToggleResolveListing(firstId))

        val currentItem = viewModel.uiState.value.listings.first { it.id == firstId }
        assertFalse("Optimistic update must be reverted on 401 error", currentItem.isResolved)
        assertEquals(UserMessage.AuthRequired, viewModel.uiState.value.userMessage)
    }

    /**
     * Verifies `toggleResolveListing_forbidden_revertsOptimisticUpdateAndSetsForbidden` behavior.
     */
    @Test
    fun `toggleResolveListing_forbidden_revertsOptimisticUpdateAndSetsForbidden`() {
        val delegate = InMemoryLostFoundRepository()
        val repository = object : LostFoundRepository {
            /**
             * Verifies `getListings` behavior.
             */
            override suspend fun getListings(): Result<List<LostFoundListing>> = delegate.getListings()

            /**
             * Verifies `reportItem` behavior.
             */
            override suspend fun reportItem(
                item: LostFoundListing,
                imageBytes: ByteArray?,
                imageMimeType: String?,
            ): Result<LostFoundListing> = delegate.reportItem(item, imageBytes, imageMimeType)

            /**
             * Verifies `toggleResolved` behavior.
             */
            override suspend fun toggleResolved(
                id: String,
                isResolved: Boolean,
            ): Result<Unit> = Result.failure(
                com.giathinh.uthconnect.data.repository.SupabaseHttpException(
                    java.net.HttpURLConnection.HTTP_FORBIDDEN,
                    "Forbidden",
                ),
            )
        }
        val viewModel = createViewModel(repository)
        val firstId = viewModel.uiState.value.listings.first().id
        assertFalse(viewModel.uiState.value.listings.first().isResolved)

        viewModel.onAction(LostFoundAction.ToggleResolveListing(firstId))

        val currentItem = viewModel.uiState.value.listings.first { it.id == firstId }
        assertFalse("Optimistic update must be reverted on 403 error", currentItem.isResolved)
        assertEquals(UserMessage.Forbidden, viewModel.uiState.value.userMessage)
    }

    /**
     * Verifies `toggleResolveListing_genericError_revertsOptimisticUpdateAndSetsUpdateStatusFailed` behavior.
     */
    @Test
    fun `toggleResolveListing_genericError_revertsOptimisticUpdateAndSetsUpdateStatusFailed`() {
        val delegate = InMemoryLostFoundRepository()
        val repository = object : LostFoundRepository {
            /**
             * Verifies `getListings` behavior.
             */
            override suspend fun getListings(): Result<List<LostFoundListing>> = delegate.getListings()

            /**
             * Verifies `reportItem` behavior.
             */
            override suspend fun reportItem(
                item: LostFoundListing,
                imageBytes: ByteArray?,
                imageMimeType: String?,
            ): Result<LostFoundListing> = delegate.reportItem(item, imageBytes, imageMimeType)

            /**
             * Verifies `toggleResolved` behavior.
             */
            override suspend fun toggleResolved(
                id: String,
                isResolved: Boolean,
            ): Result<Unit> = Result.failure(java.io.IOException("Network unavailable"))
        }
        val viewModel = createViewModel(repository)
        val firstId = viewModel.uiState.value.listings.first().id
        assertFalse(viewModel.uiState.value.listings.first().isResolved)

        viewModel.onAction(LostFoundAction.ToggleResolveListing(firstId))

        val currentItem = viewModel.uiState.value.listings.first { it.id == firstId }
        assertFalse("Optimistic update must be reverted on network failure", currentItem.isResolved)
        assertEquals(UserMessage.UpdateStatusFailed, viewModel.uiState.value.userMessage)
    }

    /**
     * Verifies `updateAndRemoveReportImage_modifiesFormStateCorrectly` behavior and invariants.
     */
    @Test
    fun `updateAndRemoveReportImage_modifiesFormStateCorrectly`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)

        val sampleBytes = byteArrayOf(1, 2, 3, 4)
        val sampleMime = "image/png"
        val sampleUri = "content://media/external/images/123"

        viewModel.onAction(LostFoundAction.UpdateReportImage(sampleBytes, sampleMime, sampleUri))
        val state = viewModel.uiState.value.reportForm
        assertEquals(sampleMime, state.imageMimeType)
        assertEquals(sampleUri, state.imagePreviewUri)
        assertNotNull(state.imageBytes)
        assertTrue(sampleBytes.contentEquals(state.imageBytes))

        // Xóa ảnh
        viewModel.onAction(LostFoundAction.RemoveReportImage)
        val clearedState = viewModel.uiState.value.reportForm
        assertNull(clearedState.imageBytes)
        assertNull(clearedState.imageMimeType)
        assertNull(clearedState.imagePreviewUri)
    }

    /**
     * Verifies `submitReport_withEmptyTitle_setsTitleErrorAndDoesNotSubmit` behavior and invariants.
     */
    @Test
    fun `submitReport_withEmptyTitle_setsTitleErrorAndDoesNotSubmit`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        assertTrue(viewModel.uiState.value.isReportDialogOpen)

        val longDescription = "Đây là mô tả dài hơn hai mươi ký tự để pass kiểm tra"
        viewModel.onAction(LostFoundAction.UpdateReportDescription(longDescription))
        viewModel.onAction(LostFoundAction.SubmitReport)

        val state = viewModel.uiState.value
        assertEquals(
            "titleError phải là FormValidationError.TitleRequired",
            FormValidationError.TitleRequired,
            state.reportForm.titleError,
        )
        assertNull("descriptionError không được có lỗi", state.reportForm.descriptionError)
        assertEquals("Số lượng tin không được thay đổi", 8, state.listings.size)
        assertTrue("Dialog vẫn phải mở", state.isReportDialogOpen)
    }

    /**
     * Verifies `submitReport_withEmptyDescription_succeedsBecauseDescriptionIsOptional` behavior and invariants.
     */
    @Test
    fun `submitReport_withEmptyDescription_succeedsBecauseDescriptionIsOptional`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)

        viewModel.onAction(LostFoundAction.UpdateReportTitle("Rơi ví tiền cá nhân"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription(""))
        viewModel.onAction(LostFoundAction.UpdateReportLocation("Căn tin CS1"))
        viewModel.onAction(LostFoundAction.UpdateReportContactPhone("0912345678"))
        viewModel.onAction(LostFoundAction.SubmitReport)

        val state = viewModel.uiState.value
        assertNull("titleError không được có lỗi", state.reportForm.titleError)
        assertNull("descriptionError không được có lỗi vì mô tả là tùy chọn", state.reportForm.descriptionError)
        assertEquals("Số lượng tin phải tăng lên 9", 9, state.listings.size)
        assertEquals("Rơi ví tiền cá nhân", state.listings.first().title)
        assertEquals("", state.listings.first().description)
        assertFalse("Dialog phải đóng sau khi submit thành công", state.isReportDialogOpen)
    }

    /**
     * Verifies `submitReport_withValidData_addsItemToTopResetsCategoryAndForm` behavior and invariants.
     */
    @Test
    fun `submitReport_withValidData_addsItemToTopResetsCategoryAndForm`() {
        val viewModel = createViewModel()

        // Giả sử trước đó người dùng đang lọc theo danh mục STUDENT_CARD
        viewModel.onAction(LostFoundAction.SelectCategory(ItemCategory.STUDENT_CARD))
        assertEquals(ItemCategory.STUDENT_CARD, viewModel.uiState.value.selectedCategory)

        viewModel.onAction(LostFoundAction.OpenReportDialog)

        val title = "Rơi tai nghe Sony WH-1000XM5"
        val desc = "Để quên ở bàn học tầng 3 thư viện cơ sở 1 lúc 14h chiều nay"
        val location = "Thư viện CS1"
        val contactName = "Nguyễn Minh Thắng"
        val contactPhone = "0909123456"

        viewModel.onAction(LostFoundAction.UpdateReportTitle(title))
        viewModel.onAction(LostFoundAction.UpdateReportDescription(desc))
        viewModel.onAction(LostFoundAction.UpdateReportKind(ListingKind.LOST))
        viewModel.onAction(LostFoundAction.UpdateReportCategory(ItemCategory.ELECTRONICS))
        viewModel.onAction(LostFoundAction.UpdateReportLocation(location))
        viewModel.onAction(LostFoundAction.UpdateReportContactName(contactName))
        viewModel.onAction(LostFoundAction.UpdateReportContactPhone(contactPhone))

        viewModel.onAction(LostFoundAction.SubmitReport)

        val state = viewModel.uiState.value
        assertFalse("Dialog phải đóng sau khi đăng thành công", state.isReportDialogOpen)
        assertEquals("Tổng số bài phải tăng lên 9", 9, state.listings.size)
        assertEquals("Bài mới phải nằm đầu danh sách", title, state.listings.first().title)
        assertEquals(desc, state.listings.first().description)
        assertEquals(location, state.listings.first().location)
        assertEquals(contactName, state.listings.first().contactName)
        assertEquals(contactPhone, state.listings.first().contactPhone)
        assertEquals(ListingKind.LOST, state.listings.first().kind)
        assertEquals(ItemCategory.ELECTRONICS, state.listings.first().category)

        // Kiểm tra tiêu chí: Bộ lọc danh mục phải được reset về ALL để tin mới hiện ngay!
        assertEquals("Bộ lọc danh mục phải được reset về ALL", ItemCategory.ALL, state.selectedCategory)
        assertTrue("Tin mới vừa đăng phải có trong filteredListings", state.filteredListings.any { it.title == title })

        assertEquals("Form phải được reset", "", state.reportForm.title)
        assertEquals(UserMessage.PostSuccess, state.userMessage)
    }

    /**
     * Verifies `dismissMessage_clearsUserMessage` behavior and invariants.
     */
    @Test
    fun `dismissMessage_clearsUserMessage`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        viewModel.onAction(LostFoundAction.UpdateReportTitle("Rơi thẻ sinh viên"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription("Rơi thẻ tại sảnh toà B trường cơ sở 1 sáng nay"))
        viewModel.onAction(LostFoundAction.UpdateReportLocation("Sảnh tòa B, CS1"))
        viewModel.onAction(LostFoundAction.UpdateReportContactPhone("0912345678"))
        viewModel.onAction(LostFoundAction.SubmitReport)

        assertNotNull(viewModel.uiState.value.userMessage)
        viewModel.onAction(LostFoundAction.DismissMessage)
        assertNull(viewModel.uiState.value.userMessage)
    }

    /**
     * Verifies `submitReport_requiresLocationAndPhone_andClearsEachErrorWhenEdited` behavior and invariants.
     */
    @Test
    fun `submitReport_requiresLocationAndPhone_andClearsEachErrorWhenEdited`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        viewModel.onAction(LostFoundAction.UpdateReportTitle("Nhặt được thẻ sinh viên"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription("Nhặt được thẻ trước cửa phòng B101 chiều nay"))
        viewModel.onAction(LostFoundAction.SubmitReport)

        assertEquals(FormValidationError.LocationRequired, viewModel.uiState.value.reportForm.locationError)
        assertEquals(FormValidationError.PhoneRequired, viewModel.uiState.value.reportForm.phoneError)
        assertEquals(8, viewModel.uiState.value.listings.size)
        assertTrue(viewModel.uiState.value.isReportDialogOpen)

        viewModel.onAction(LostFoundAction.UpdateReportLocation("Phòng B101"))
        viewModel.onAction(LostFoundAction.UpdateReportContactPhone("not a phone"))
        assertNull(viewModel.uiState.value.reportForm.locationError)
        assertNull(viewModel.uiState.value.reportForm.phoneError)
        viewModel.onAction(LostFoundAction.SubmitReport)
        assertEquals(FormValidationError.PhoneInvalid, viewModel.uiState.value.reportForm.phoneError)
        assertEquals(8, viewModel.uiState.value.listings.size)
        assertEquals("Nhặt được thẻ sinh viên", viewModel.uiState.value.reportForm.title)

        viewModel.onAction(LostFoundAction.UpdateReportContactPhone("+84 912 345 678"))
        viewModel.onAction(LostFoundAction.SubmitReport)
        assertEquals(9, viewModel.uiState.value.listings.size)
        assertEquals("+84912345678", viewModel.uiState.value.listings.first().contactPhone)
        assertEquals("", viewModel.uiState.value.listings.first().contactName)
    }

    /**
     * Verifies `submitReport_preventsDuplicatePostsWhilePending` behavior and invariants.
     */
    @Test
    fun `submitReport_preventsDuplicatePostsWhilePending`() {
        val release = CompletableDeferred<Unit>()
        var submissions = 0
        val repository = object : LostFoundRepository {
            private val delegate = InMemoryLostFoundRepository()

            /**
             * Verifies `getListings` behavior and invariants.
             */
            override suspend fun getListings(): Result<List<LostFoundListing>> = delegate.getListings()

            /**
             * Verifies `reportItem` behavior and invariants.
             */
            override suspend fun reportItem(
                item: LostFoundListing,
                imageBytes: ByteArray?,
                imageMimeType: String?,
            ): Result<LostFoundListing> {
                submissions++
                release.await()
                return delegate.reportItem(item, imageBytes, imageMimeType)
            }

            /**
             * Verifies `toggleResolved` behavior and invariants.
             */
            override suspend fun toggleResolved(
                id: String,
                isResolved: Boolean,
            ): Result<Unit> = delegate.toggleResolved(id, isResolved)
        }
        val viewModel = createViewModel(repository)
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        viewModel.onAction(LostFoundAction.UpdateReportTitle("Nhặt được ví"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription("Nhặt được ví tại phòng B101 chiều nay"))
        viewModel.onAction(LostFoundAction.UpdateReportLocation("B101"))
        viewModel.onAction(LostFoundAction.UpdateReportContactPhone("0912345678"))

        viewModel.onAction(LostFoundAction.SubmitReport)
        viewModel.onAction(LostFoundAction.SubmitReport)
        assertTrue(viewModel.uiState.value.reportForm.isSubmitting)
        assertEquals(1, submissions)

        release.complete(Unit)
        assertEquals(1, submissions)
        assertEquals(9, viewModel.uiState.value.listings.size)
        assertFalse(viewModel.uiState.value.isReportDialogOpen)
    }

    /**
     * Verifies `repositoryFailure_setsErrorState_andRetryReloadsListings` behavior and invariants.
     */
    @Test
    fun `repositoryFailure_setsErrorState_andRetryReloadsListings`() {
        var shouldFail = true
        val mockRepo = object : LostFoundRepository {
            private val delegate = InMemoryLostFoundRepository()

            /**
             * Verifies `getListings` behavior and invariants.
             */
            override suspend fun getListings(): Result<List<LostFoundListing>> = if (shouldFail) {
                Result.failure(RuntimeException("Lỗi kết nối cơ sở dữ liệu"))
            } else {
                delegate.getListings()
            }

            /**
             * Verifies `reportItem` behavior and invariants.
             */
            override suspend fun reportItem(
                item: LostFoundListing,
                imageBytes: ByteArray?,
                imageMimeType: String?,
            ): Result<LostFoundListing> = delegate.reportItem(item, imageBytes, imageMimeType)

            /**
             * Verifies `toggleResolved` behavior and invariants.
             */
            override suspend fun toggleResolved(
                id: String,
                isResolved: Boolean,
            ): Result<Unit> = delegate.toggleResolved(id, isResolved)
        }

        val viewModel = createViewModel(mockRepo)
        var state = viewModel.uiState.value

        assertFalse("isLoading phải là false", state.isLoading)
        assertTrue("isError phải là true khi xảy ra lỗi", state.isError)
        assertTrue("listings phải rỗng khi lỗi", state.listings.isEmpty())

        // Thử lại khi kết nối phục hồi
        shouldFail = false
        viewModel.onAction(LostFoundAction.Retry)

        state = viewModel.uiState.value
        assertFalse("isLoading phải là false sau khi retry", state.isLoading)
        assertFalse("isError phải là false sau khi retry thành công", state.isError)
        assertEquals("Phải nạp lại đầy đủ 8 tin", 8, state.listings.size)
    }

    /**
     * Verifies that triggering [LostFoundAction.ShowDialerUnavailable] properly sets
     * [UserMessage.DialerUnavailable] so the UI surfaces a user-facing explanation.
     */
    @Test
    fun `showDialerUnavailable_setsUserMessageDialerUnavailable`() {
        val viewModel = createViewModel()
        assertNull("userMessage ban đầu phải là null", viewModel.uiState.value.userMessage)

        viewModel.onAction(LostFoundAction.ShowDialerUnavailable)

        assertEquals(
            "userMessage phải là DialerUnavailable",
            UserMessage.DialerUnavailable,
            viewModel.uiState.value.userMessage,
        )

        viewModel.onAction(LostFoundAction.DismissMessage)
        assertNull("userMessage sau khi dismiss phải là null", viewModel.uiState.value.userMessage)
    }
}

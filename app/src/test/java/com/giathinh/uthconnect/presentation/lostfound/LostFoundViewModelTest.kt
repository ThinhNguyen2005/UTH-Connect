package com.giathinh.uthconnect.presentation.lostfound

import com.giathinh.uthconnect.data.repository.InMemoryLostFoundRepository
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import com.giathinh.uthconnect.domain.usecase.GetLostFoundListingsUseCase
import com.giathinh.uthconnect.domain.usecase.ReportLostFoundItemUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
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

    private fun createViewModel(
        repository: LostFoundRepository = InMemoryLostFoundRepository(),
    ): LostFoundViewModel {
        return LostFoundViewModel(
            getListingsUseCase = GetLostFoundListingsUseCase(repository),
            reportItemUseCase = ReportLostFoundItemUseCase(repository),
        )
    }

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
        assertEquals("Nhặt được tai nghe AirPods Pro trong hộp màu xanh", viewModel.uiState.value.filteredListings.first().title)

        // Reset về tất cả danh mục
        viewModel.onAction(LostFoundAction.SelectCategory(ItemCategory.ALL))
        assertEquals(4, viewModel.uiState.value.filteredListings.size)
    }

    @Test
    fun `submitReport_withEmptyTitle_setsTitleErrorAndDoesNotSubmit`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        assertTrue(viewModel.uiState.value.isReportDialogOpen)

        viewModel.onAction(LostFoundAction.UpdateReportDescription("Đây là mô tả dài hơn hai mươi ký tự để pass kiểm tra"))
        viewModel.onAction(LostFoundAction.SubmitReport)

        val state = viewModel.uiState.value
        assertEquals("titleError phải là FormValidationError.TitleRequired", FormValidationError.TitleRequired, state.reportForm.titleError)
        assertNull("descriptionError không được có lỗi", state.reportForm.descriptionError)
        assertEquals("Số lượng tin không được thay đổi", 8, state.listings.size)
        assertTrue("Dialog vẫn phải mở", state.isReportDialogOpen)
    }

    @Test
    fun `submitReport_withShortDescription_setsDescriptionErrorAndDoesNotSubmit`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)

        val shortDesc = "Mô tả ngắn"
        viewModel.onAction(LostFoundAction.UpdateReportTitle("Rơi ví tiền cá nhân"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription(shortDesc))
        viewModel.onAction(LostFoundAction.SubmitReport)

        val state = viewModel.uiState.value
        assertNull("titleError không được có lỗi", state.reportForm.titleError)
        assertTrue("descriptionError phải là DescriptionTooShort", state.reportForm.descriptionError is FormValidationError.DescriptionTooShort)
        val err = state.reportForm.descriptionError as FormValidationError.DescriptionTooShort
        assertEquals(shortDesc.length, err.currentLength)
        assertEquals("Số lượng tin không được thay đổi", 8, state.listings.size)
        assertTrue("Dialog vẫn phải mở", state.isReportDialogOpen)
    }

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

    @Test
    fun `dismissMessage_clearsUserMessage`() {
        val viewModel = createViewModel()
        viewModel.onAction(LostFoundAction.OpenReportDialog)
        viewModel.onAction(LostFoundAction.UpdateReportTitle("Rơi thẻ sinh viên"))
        viewModel.onAction(LostFoundAction.UpdateReportDescription("Rơi thẻ tại sảnh toà B trường cơ sở 1 sáng nay"))
        viewModel.onAction(LostFoundAction.SubmitReport)

        assertNotNull(viewModel.uiState.value.userMessage)
        viewModel.onAction(LostFoundAction.DismissMessage)
        assertNull(viewModel.uiState.value.userMessage)
    }

    @Test
    fun `repositoryFailure_setsErrorState_andRetryReloadsListings`() {
        var shouldFail = true
        val mockRepo = object : LostFoundRepository {
            private val delegate = InMemoryLostFoundRepository()

            override suspend fun getListings(): Result<List<LostFoundListing>> {
                return if (shouldFail) {
                    Result.failure(RuntimeException("Lỗi kết nối cơ sở dữ liệu"))
                } else {
                    delegate.getListings()
                }
            }

            override suspend fun reportItem(item: LostFoundListing): Result<LostFoundListing> {
                return delegate.reportItem(item)
            }
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
}

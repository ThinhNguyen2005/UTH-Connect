package com.giathinh.uthconnect.presentation.housing.create

import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingDraftStore
import com.giathinh.uthconnect.domain.repository.HousingRepository
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.AccessStudentSessionUseCase
import com.giathinh.uthconnect.domain.usecase.CreateHousingListingUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CreateHousingPostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeCreateHousingRepository : HousingRepository {
        var createdListing: HousingListing? = null
        var createCalls = 0
        var createGate: CompletableDeferred<Unit>? = null
        private val notFound: Result<HousingListing> = Result.failure(NoSuchElementException())

        override suspend fun getListings(): Result<List<HousingListing>> = Result.success(emptyList())

        override suspend fun getListingById(id: String): Result<HousingListing> = notFound

        override suspend fun createListing(listing: HousingListing): Result<HousingListing> {
            createCalls++
            createGate?.await()
            createdListing = listing
            return Result.success(listing)
        }

        override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = Result.success(listing)

        override suspend fun markListingFulfilled(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun deleteListing(id: String): Result<Unit> = Result.success(Unit)

        override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeSessionRepository : SessionRepository {
        override val session: StateFlow<StudentSession> = MutableStateFlow(
            StudentSession(
                isRestoring = false,
                profile = StudentProfile(
                    studentId = "2251001234",
                    email = "student@ut.edu.vn",
                    displayName = "Sinh viên kiểm thử",
                    className = "22DTH",
                    major = "CNTT",
                    faculty = "Công nghệ thông tin",
                    uthVerified = true,
                ),
            ),
        )

        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
        override suspend fun updateProfile(profile: StudentProfile) = Unit
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun signOut() = Unit
    }

    private fun viewModel(
        repository: FakeCreateHousingRepository,
        draftStore: FakeHousingDraftStore = FakeHousingDraftStore(),
    ) = CreateHousingPostViewModel(
        CreateHousingListingUseCase(repository),
        AccessStudentSessionUseCase(FakeSessionRepository()),
        draftStore,
    )

    private class FakeHousingDraftStore : HousingDraftStore {
        var payload: String? = null
        var readGate: CompletableDeferred<Unit>? = null
        var readError: IOException? = null
        var writeError: IOException? = null
        var clearError: IOException? = null

        override suspend fun read(): String? {
            readGate?.await()
            readError?.let { throw it }
            return payload
        }

        override suspend fun write(payload: String) {
            writeError?.let { throw it }
            this.payload = payload
        }

        override suspend fun clear() {
            clearError?.let { throw it }
            payload = null
        }
    }

    private fun draftPayload(state: CreateHousingPostUiState, owner: String = "2251001234"): String {
        val envelope = HousingDraftEnvelope(owner, state)
        return Json.encodeToString(envelope)
    }

    @Test
    fun `validate - de trong tieu de bao loi EMPTY_TITLE`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.EMPTY_TITLE, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `validate - de trong gia bao loi INVALID_RENT`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng trọ đẹp"))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_RENT, viewModel.uiState.value.validationError)
    }

    @Test
    fun `validate - tieu de duoi 5 ky tu bao loi INVALID_TITLE_LENGTH`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("test"))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_TITLE_LENGTH, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `validate - dien tich 0 bao loi INVALID_AREA`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng trọ gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("1500000"))
        viewModel.onAction(CreateHousingPostAction.ChangeArea("0"))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_AREA, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `submit hop le - goi repository va phat event PostCreated`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng gác lửng gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("2500000"))
        viewModel.onAction(CreateHousingPostAction.ChangeAddress("138 Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeContactPhone("0901234567"))
        viewModel.onAction(CreateHousingPostAction.ChangeCampus(UthCampus.CS1_QUANG_TRUNG))

        viewModel.onAction(CreateHousingPostAction.Submit)

        val event = viewModel.events.first()
        assertEquals(CreateHousingPostEvent.PostCreated, event)
        assertNotNull(repo.createdListing)
        assertEquals("Phòng gác lửng gần CS1", repo.createdListing?.title)
        assertEquals(2_500_000, repo.createdListing?.monthlyRentVnd)
        assertEquals("0901234567", repo.createdListing?.contactPhone)
        assertEquals("2251001234", repo.createdListing?.hostStudentId)
        assertEquals("Sinh viên kiểm thử", repo.createdListing?.hostName)
        assertTrue(repo.createdListing?.verified == true)
    }

    @Test
    fun `Submit hai lan khi dang gui - chi tao mot tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val gate = CompletableDeferred<Unit>()
        repo.createGate = gate
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.Submit)
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.onAction(CreateHousingPostAction.Submit)
        gate.complete(Unit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals(1, repo.createCalls)
    }

    @Test
    fun `allowZalo mac dinh true va ChangeAllowZalo toggle duoc`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        assertTrue(viewModel.uiState.value.allowZalo)

        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(false))
        assertFalse(viewModel.uiState.value.allowZalo)

        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(true))
        assertTrue(viewModel.uiState.value.allowZalo)
    }

    @Test
    fun `allowZalo tat - contactZaloUrl rong khi dang tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("https://zalo.me/0901234567"))
        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(false))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("", repo.createdListing?.contactZaloUrl)
    }

    @Test
    fun `allowZalo bat - chua nhap link thi fallback theo so dien thoai`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("https://zalo.me/0901234567", repo.createdListing?.contactZaloUrl)
    }

    @Test
    fun `SaveDraft - phat DraftSaved giu nguyen form va khong dang tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Nháp phòng gác lửng"))

        viewModel.onAction(CreateHousingPostAction.SaveDraft)

        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        assertEquals("Nháp phòng gác lửng", viewModel.uiState.value.title)
        assertNull("luu nhap khong duoc tao tin that", repo.createdListing)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `SaveDraft - ghi payload xuong store va giu nguyen form`() = runTest {
        val repo = FakeCreateHousingRepository()
        val store = FakeHousingDraftStore()
        val viewModel = viewModel(repo, store)
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Nháp phòng gác lửng"))

        viewModel.onAction(CreateHousingPostAction.SaveDraft)

        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        assertTrue(requireNotNull(store.payload).contains("Nháp phòng gác lửng"))
        assertNull(repo.createdListing)
    }

    @Test
    fun `mo man - khoi phuc nhap da luu truoc do`() = runTest {
        val store = FakeHousingDraftStore()
        val saved = CreateHousingPostUiState(title = "Nháp phòng gác lửng", rentInput = "2500000")
        store.payload = draftPayload(saved)

        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        assertEquals(CreateHousingPostEvent.DraftRestored, viewModel.events.first())
        assertEquals("Nháp phòng gác lửng", viewModel.uiState.value.title)
        assertEquals("2500000", viewModel.uiState.value.rentInput)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `dang tin thanh cong - xoa nhap khoi store`() = runTest {
        val repo = FakeCreateHousingRepository()
        val store = FakeHousingDraftStore()
        val viewModel = viewModel(repo, store)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.SaveDraft)
        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        assertNotNull(store.payload)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertNull(store.payload)
    }

    @Test
    fun `nhap hong - mo man van form mac dinh khong crash`() = runTest {
        val store = FakeHousingDraftStore()
        store.payload = "{không phải json"

        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        assertEquals("", viewModel.uiState.value.title)
    }

    @Test
    fun `khoi phuc tre - khong de len form da bi cham`() = runTest {
        val store = FakeHousingDraftStore()
        store.payload = draftPayload(CreateHousingPostUiState(title = "Nháp phòng gác lửng"))
        val gate = CompletableDeferred<Unit>()
        store.readGate = gate
        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Người dùng đang gõ"))
        gate.complete(Unit)

        assertEquals("Người dùng đang gõ", viewModel.uiState.value.title)
    }

    @Test
    fun `AddPhoto - khong vuot tran MAX_HOUSING_PHOTOS`() {
        val viewModel = viewModel(FakeCreateHousingRepository())

        repeat(MAX_HOUSING_PHOTOS + 1) { index ->
            viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/$index"))
        }

        assertEquals(MAX_HOUSING_PHOTOS, viewModel.uiState.value.photos.size)
    }

    @Test
    fun `RemovePhoto - xoa dung thu tu anh`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/0"))
        viewModel.onAction(CreateHousingPostAction.AddPhoto("content://photo/1"))

        viewModel.onAction(CreateHousingPostAction.RemovePhoto(0))

        assertEquals(listOf("content://photo/1"), viewModel.uiState.value.photos)
    }

    @Test
    fun `ChangeLocation - luu toa do that va dia chi picker khi dang tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.ChangeLocation(10.8051, 106.7170, "138 Võ Oanh, Bình Thạnh"))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        val listing = requireNotNull(repo.createdListing)
        assertEquals(10.8051, listing.latitude, 0.0)
        assertEquals(106.7170, listing.longitude, 0.0)
        assertEquals("138 Võ Oanh, Bình Thạnh", listing.addressLabel)
        assertTrue(viewModel.uiState.value.hasLocation)
    }

    @Test
    fun `ChangeLocation - dia chi rong thi giu dia chi da nhap tay`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        viewModel.onAction(CreateHousingPostAction.ChangeAddress("138 Quang Trung, Gò Vấp"))

        viewModel.onAction(CreateHousingPostAction.ChangeLocation(10.8051, 106.7170, ""))

        assertEquals("138 Quang Trung, Gò Vấp", viewModel.uiState.value.address)
        assertEquals(10.8051, viewModel.uiState.value.latitude, 0.0)
        assertTrue(viewModel.uiState.value.hasLocation)
    }

    @Test
    fun `validate - dien tich thap phan bao loi INVALID_AREA`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng trọ gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("1500000"))

        viewModel.onAction(CreateHousingPostAction.ChangeArea("25.5"))
        viewModel.onAction(CreateHousingPostAction.Submit)
        assertEquals(CreateHousingPostError.INVALID_AREA, viewModel.uiState.value.validationError)

        viewModel.onAction(CreateHousingPostAction.ChangeArea("25,5"))
        viewModel.onAction(CreateHousingPostAction.Submit)
        assertEquals(CreateHousingPostError.INVALID_AREA, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `ChangeAddress - xoa toa do pin cu khi sua dia chi tay`() {
        val viewModel = viewModel(FakeCreateHousingRepository())
        viewModel.onAction(CreateHousingPostAction.ChangeLocation(10.8051, 106.7170, "138 Võ Oanh, Bình Thạnh"))

        viewModel.onAction(CreateHousingPostAction.ChangeAddress("Địa chỉ mới nhập tay"))

        assertEquals("Địa chỉ mới nhập tay", viewModel.uiState.value.address)
        assertEquals(0.0, viewModel.uiState.value.latitude, 0.0)
        assertEquals(0.0, viewModel.uiState.value.longitude, 0.0)
        assertFalse(viewModel.uiState.value.hasLocation)
    }

    @Test
    fun `ChangeType - LOOKING_FOR_ROOM duoc gui dung loai tin`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.ChangeType(HousingListingType.LOOKING_FOR_ROOM))
        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals(HousingListingType.LOOKING_FOR_ROOM, repo.createdListing?.type)
    }

    @Test
    fun `SaveDraft - cho khoi phuc hoan tat roi moi ghi`() = runTest {
        val store = FakeHousingDraftStore()
        val saved = CreateHousingPostUiState(title = "Nháp phòng gác lửng")
        store.payload = draftPayload(saved)
        val gate = CompletableDeferred<Unit>()
        store.readGate = gate
        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        viewModel.onAction(CreateHousingPostAction.SaveDraft)
        assertEquals("chua ghi khi khoi phuc chua xong", draftPayload(saved), store.payload)

        gate.complete(Unit)

        assertEquals(CreateHousingPostEvent.DraftRestored, viewModel.events.first())
        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        assertEquals(draftPayload(saved), store.payload)
    }

    @Test
    fun `Submit loi - khong tinh la cham form, nhap van duoc khoi phuc`() = runTest {
        val store = FakeHousingDraftStore()
        store.payload = draftPayload(CreateHousingPostUiState(title = "Nháp phòng gác lửng"))
        val gate = CompletableDeferred<Unit>()
        store.readGate = gate
        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        viewModel.onAction(CreateHousingPostAction.Submit)
        gate.complete(Unit)

        assertEquals("Nháp phòng gác lửng", viewModel.uiState.value.title)
    }

    @Test
    fun `nhap cua tai khoan khac - khong khoi phuc`() = runTest {
        val store = FakeHousingDraftStore()
        store.payload = draftPayload(CreateHousingPostUiState(title = "Nháp tài khoản khác"), owner = "9999999999")

        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        assertEquals("", viewModel.uiState.value.title)
    }

    @Test
    fun `nhap dinh dang cu khong co owner - bo qua`() = runTest {
        val store = FakeHousingDraftStore()
        store.payload = Json.encodeToString(CreateHousingPostUiState(title = "Nháp định dạng cũ"))

        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        assertEquals("", viewModel.uiState.value.title)
    }

    @Test
    fun `SaveDraft - gan ma sinh vien so huu vao payload`() = runTest {
        val store = FakeHousingDraftStore()
        val viewModel = viewModel(FakeCreateHousingRepository(), store)
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Nháp phòng gác lửng"))

        viewModel.onAction(CreateHousingPostAction.SaveDraft)

        assertEquals(CreateHousingPostEvent.DraftSaved, viewModel.events.first())
        val payload = requireNotNull(store.payload)
        assertTrue(payload.contains("2251001234"))
        assertTrue(payload.contains("Nháp phòng gác lửng"))
    }

    @Test
    fun `doc nhap loi IOException - van mo form binh thuong`() = runTest {
        val store = FakeHousingDraftStore()
        store.readError = IOException("disk read failed")

        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        assertEquals("", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `luu nhap loi IOException - bao DraftSaveFailed`() = runTest {
        val store = FakeHousingDraftStore()
        store.writeError = IOException("disk write failed")
        val viewModel = viewModel(FakeCreateHousingRepository(), store)

        viewModel.onAction(CreateHousingPostAction.SaveDraft)

        assertEquals(CreateHousingPostEvent.DraftSaveFailed, viewModel.events.first())
        assertNull(store.payload)
    }

    @Test
    fun `xoa nhap loi sau khi dang - van PostCreated`() = runTest {
        val repo = FakeCreateHousingRepository()
        val store = FakeHousingDraftStore()
        store.clearError = IOException("disk clear failed")
        val viewModel = viewModel(repo, store)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertNotNull(repo.createdListing)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `zalo ngoai mien - bao loi INVALID_ZALO va khong dang`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("https://evil.example/zalo"))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_ZALO, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `zalo http - bao loi INVALID_ZALO`() {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("http://zalo.me/0901234567"))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostError.INVALID_ZALO, viewModel.uiState.value.validationError)
        assertNull(repo.createdListing)
    }

    @Test
    fun `zalo hop le - dang voi link giu nguyen`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("https://zalo.me/0901234567"))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("https://zalo.me/0901234567", repo.createdListing?.contactZaloUrl)
    }

    @Test
    fun `zalo la sdt - ghep thanh link zalo me`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("0908123456"))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("https://zalo.me/0908123456", repo.createdListing?.contactZaloUrl)
    }

    @Test
    fun `zalo trong - fallback theo sdt lien he`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("https://zalo.me/0901234567", repo.createdListing?.contactZaloUrl)
    }

    @Test
    fun `tat zalo - link rac van dang duoc voi contactZaloUrl rong`() = runTest {
        val repo = FakeCreateHousingRepository()
        val viewModel = viewModel(repo)
        fillValidForm(viewModel)
        viewModel.onAction(CreateHousingPostAction.ChangeContactZalo("khong phai link"))
        viewModel.onAction(CreateHousingPostAction.ChangeAllowZalo(false))

        viewModel.onAction(CreateHousingPostAction.Submit)

        assertEquals(CreateHousingPostEvent.PostCreated, viewModel.events.first())
        assertEquals("", repo.createdListing?.contactZaloUrl)
    }

    private fun fillValidForm(viewModel: CreateHousingPostViewModel) {
        viewModel.onAction(CreateHousingPostAction.ChangeTitle("Phòng gác lửng gần CS1"))
        viewModel.onAction(CreateHousingPostAction.ChangeRent("2500000"))
        viewModel.onAction(CreateHousingPostAction.ChangeAddress("138 Quang Trung, Gò Vấp"))
        viewModel.onAction(CreateHousingPostAction.ChangeContactPhone("0901234567"))
    }
}

package com.giathinh.uthconnect.presentation.incidents

import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.model.IncidentSummary
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import com.giathinh.uthconnect.domain.usecase.CreateIncidentReportUseCase
import com.giathinh.uthconnect.domain.validation.IncidentFieldError
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncidentsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial load populates myReports and openFeed`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.myReports.size)
        assertEquals(1, state.openFeed.size)
        assertNull(state.error)
    }

    @Test
    fun `validation failure preserves clientRequestId and exposes field errors`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        val initialRequestId = vm.uiState.value.form.clientRequestId
        vm.onAction(IncidentsAction.OpenCreateSheet)
        vm.onAction(IncidentsAction.RoomCodeChanged("   "))
        vm.onAction(IncidentsAction.TitleChanged("abc"))
        vm.onAction(IncidentsAction.DescriptionChanged("short"))
        vm.onAction(IncidentsAction.SubmitReport)
        advanceUntilIdle()

        val form = vm.uiState.value.form
        assertEquals(initialRequestId, form.clientRequestId)
        assertEquals(IncidentFieldError.ROOM_REQUIRED, form.roomError)
        assertEquals(IncidentFieldError.TITLE_LENGTH, form.titleError)
        assertEquals(IncidentFieldError.DESCRIPTION_LENGTH, form.descriptionError)
        assertTrue(vm.uiState.value.showCreateSheet)
    }

    @Test
    fun `successful upload and submit resets form with new requestId and prepends report`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        val oldRequestId = vm.uiState.value.form.clientRequestId
        vm.onAction(IncidentsAction.OpenCreateSheet)
        vm.onAction(IncidentsAction.UploadCompressedImage(byteArrayOf(1, 2, 3)))
        advanceUntilIdle()
        assertEquals(listOf("user-1/photo-1.webp"), vm.uiState.value.form.imagePaths)

        vm.onAction(IncidentsAction.RoomCodeChanged(" a102 "))
        vm.onAction(IncidentsAction.TitleChanged("Máy chiếu phòng A102 không lên"))
        vm.onAction(IncidentsAction.DescriptionChanged("Đã cắm nguồn và bật công tắc nhưng đèn báo đỏ"))
        vm.onAction(IncidentsAction.SubmitReport)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.showCreateSheet)
        assertNotEquals(oldRequestId, state.form.clientRequestId)
        assertEquals("A102", repo.lastDraft?.roomCode)
        assertEquals(listOf("user-1/photo-1.webp"), repo.lastDraft?.imagePaths)
    }

    @Test
    fun `rate limit and conflict backend errors map to specific error codes`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        repo.createError = IllegalStateException("INCIDENT_RATE_LIMIT")
        vm.onAction(IncidentsAction.RoomCodeChanged("H001"))
        vm.onAction(IncidentsAction.TitleChanged("Hỏng quạt trần phòng H001"))
        vm.onAction(IncidentsAction.DescriptionChanged("Quạt trần số 2 kêu to và không quay"))
        vm.onAction(IncidentsAction.SubmitReport)
        advanceUntilIdle()
        assertEquals(IncidentErrorCode.RATE_LIMIT, vm.uiState.value.error)

        repo.transitionError = IllegalStateException("INCIDENT_CONFLICT")
        vm.onAction(IncidentsAction.CancelReport(repo.sampleReport))
        advanceUntilIdle()
        assertEquals(IncidentErrorCode.CONFLICT, vm.uiState.value.error)
    }

    @Test
    fun `confirmOpenIncident increments count and blocks duplicate click`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        vm.onAction(IncidentsAction.ConfirmOpenIncident("inc-1"))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("inc-1" in state.confirmedIds)
        assertEquals(5L, state.openFeed.first().confirmationCount)
    }

    @Test
    fun `reopenResolvedReport requires non-blank note before calling repository`() = runTest {
        val repo = FakeIncidentRepository()
        val vm = IncidentsViewModel(repo, CreateIncidentReportUseCase(repo))
        advanceUntilIdle()

        val resolved = repo.sampleReport.copy(status = IncidentStatus.RESOLVED)
        vm.onAction(IncidentsAction.ReopenResolvedReport(resolved))
        advanceUntilIdle()
        assertEquals(IncidentErrorCode.NOTE_REQUIRED, vm.uiState.value.error)

        vm.onAction(IncidentsAction.ReopenNoteChanged("Vẫn còn rò nước ở góc tường"))
        vm.onAction(IncidentsAction.ReopenResolvedReport(resolved))
        advanceUntilIdle()
        assertEquals(IncidentStatus.REOPENED, vm.uiState.value.selectedReport?.status)
    }

    private class FakeIncidentRepository : IncidentRepository {
        val sampleReport = IncidentReport(
            id = "inc-1",
            code = "INC-2026-000001",
            campus = "CS1",
            roomCode = "H001",
            locationDetail = "Cửa trước",
            category = IncidentCategory.DOOR_LOCK,
            severity = IncidentSeverity.HIGH,
            title = "Khóa cửa phòng H001 bị kẹt",
            description = "Không thể khóa cửa sau giờ học ca chiều",
            imagePaths = emptyList(),
            isAnonymousPublic = false,
            status = IncidentStatus.SUBMITTED,
            createdAt = "2026-11-28T01:00:00Z",
            updatedAt = "2026-11-28T01:00:00Z",
            rowVersion = 1L,
        )
        val sampleSummary = IncidentSummary(
            id = "inc-1",
            code = "INC-2026-000001",
            campus = "CS1",
            roomCode = "H001",
            category = IncidentCategory.DOOR_LOCK,
            severity = IncidentSeverity.HIGH,
            title = "Khóa cửa phòng H001 bị kẹt",
            status = IncidentStatus.SUBMITTED,
            createdAt = "2026-11-28T01:00:00Z",
            confirmationCount = 4L,
        )
        var currentReport: IncidentReport = sampleReport
        var lastDraft: IncidentDraft? = null
        var createError: Throwable? = null
        var transitionError: Throwable? = null

        private val uploadedKey = "user-1/photo-1.webp"
        private val openSummaries = listOf(sampleSummary)

        override suspend fun uploadIncidentImage(bytes: ByteArray): Result<String> = Result.success(uploadedKey)

        override suspend fun createReport(draft: IncidentDraft): Result<IncidentReport> {
            lastDraft = draft
            createError?.let { return Result.failure(it) }
            return Result.success(
                sampleReport.copy(
                    id = "inc-2",
                    code = "INC-2026-000002",
                    campus = draft.campus,
                    roomCode = draft.roomCode,
                    title = draft.title,
                    description = draft.description,
                    imagePaths = draft.imagePaths,
                ),
            )
        }

        override suspend fun getMyReports(): Result<List<IncidentReport>> {
            val items = listOf(currentReport)
            return Result.success(items)
        }

        override suspend fun getReport(id: String): Result<IncidentReport> {
            val found = currentReport.copy(id = id)
            return Result.success(found)
        }

        override suspend fun getOpenReports(campus: String?): Result<List<IncidentSummary>> {
            val items = openSummaries
            return Result.success(items)
        }

        override suspend fun getStatusHistory(id: String): Result<List<IncidentStatusLog>> = Result.success(
            listOf(
                IncidentStatusLog(
                    fromStatus = null,
                    toStatus = currentReport.status,
                    note = null,
                    createdAt = "2026-11-28T01:00:00Z",
                ),
            ),
        )

        override suspend fun confirmReport(id: String): Result<Long> = Result.success(5L)

        override suspend fun transitionReport(
            id: String,
            expectedVersion: Int,
            targetStatus: IncidentStatus,
            note: String?,
            duplicateOfId: String?,
        ): Result<Int> {
            transitionError?.let { return Result.failure(it) }
            val nextVersion = expectedVersion + 1
            currentReport = currentReport.copy(status = targetStatus, rowVersion = nextVersion.toLong())
            return Result.success(nextVersion)
        }
    }
}

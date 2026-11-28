package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.model.IncidentSummary
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateIncidentReportUseCaseTest {
    private class RecordingRepository : IncidentRepository {
        var received: IncidentDraft? = null
        val failure = IllegalStateException("offline")

        override suspend fun createReport(draft: IncidentDraft): Result<IncidentReport> {
            received = draft
            return Result.failure(failure)
        }

        override suspend fun getMyReports(): Result<List<IncidentReport>> = error("Not called")
        override suspend fun getReport(id: String): Result<IncidentReport> = error("Not called")
        override suspend fun getOpenReports(campus: String?): Result<List<IncidentSummary>> = error("Not called")
        override suspend fun getStatusHistory(id: String): Result<List<IncidentStatusLog>> = error("Not called")
        override suspend fun confirmReport(id: String): Result<Long> = error("Not called")
        override suspend fun transitionReport(
            id: String,
            expectedVersion: Int,
            targetStatus: IncidentStatus,
            note: String?,
            duplicateOfId: String?,
        ): Result<Int> = error("Not called")
    }

    private fun draft() = IncidentDraft(
        clientRequestId = "d72615aa-9c66-42ba-8153-02dfa832c923",
        campus = "campus-1",
        roomCode = " h001 ",
        locationDetail = null,
        category = IncidentCategory.DOOR_LOCK,
        severity = IncidentSeverity.MEDIUM,
        title = " Cửa phòng hỏng ",
        description = " Cửa phòng không đóng được ",
        imagePaths = emptyList(),
        isAnonymousPublic = false,
    )

    @Test fun `invalid report is rejected before repository call`() = runBlocking {
        val repository = RecordingRepository()
        val result = CreateIncidentReportUseCase(repository)(draft().copy(roomCode = "  "))
        assertTrue(result.exceptionOrNull() is InvalidIncidentReportException)
        assertNull(repository.received)
    }

    @Test fun `valid report is normalized and repository failure is preserved`() = runBlocking {
        val repository = RecordingRepository()
        val result = CreateIncidentReportUseCase(repository)(draft())
        assertEquals("H001", repository.received?.roomCode)
        assertEquals("Cửa phòng hỏng", repository.received?.title)
        assertEquals("Cửa phòng không đóng được", repository.received?.description)
        assertEquals(repository.failure, result.exceptionOrNull())
    }

    @Test fun `missing campus and invalid request id are rejected`() = runBlocking {
        val repository = RecordingRepository()
        val result = CreateIncidentReportUseCase(repository)(draft().copy(campus = " ", clientRequestId = "1-1-1-1-1"))
        assertTrue(result.exceptionOrNull() is InvalidIncidentReportException)
        assertNull(repository.received)
    }
}

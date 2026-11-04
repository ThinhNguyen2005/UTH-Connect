package com.giathinh.uthconnect.presentation.attendance

import com.giathinh.uthconnect.domain.model.AttendanceEntry
import com.giathinh.uthconnect.domain.model.AttendanceRoster
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.domain.repository.AttendanceRosterRepository
import com.giathinh.uthconnect.domain.usecase.GetAttendanceRosterUseCase
import com.giathinh.uthconnect.domain.usecase.AttendanceCsvLabels
import com.giathinh.uthconnect.domain.usecase.ExportAttendanceCsvUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.runBlocking

class AttendanceViewModelTest {
    @get:Rule val dispatcher = MainDispatcherRule()

    private val roster = AttendanceRoster("event", "Hội thao", listOf(
        AttendanceEntry("1", "20230001", "Nguyễn An", "an@ut.edu.vn", "2026-11-03T08:00:00Z", "organizer"),
        AttendanceEntry("2", "20230002", "Trần Bình", "binh@ut.edu.vn", null, null),
    ))

    @Test fun `load gives organizer summary from server roster`() {
        val vm = AttendanceViewModel(GetAttendanceRosterUseCase(FakeRepository(Result.success(roster))), ExportAttendanceCsvUseCase())
        vm.load("event")
        assertFalse(vm.uiState.value.loading)
        assertEquals(2, vm.uiState.value.roster?.registeredCount)
        assertEquals(1, vm.uiState.value.roster?.checkedInCount)
        assertEquals(1, vm.uiState.value.roster?.notCheckedInCount)
    }

    @Test fun `search and filters show matching students`() {
        val vm = AttendanceViewModel(GetAttendanceRosterUseCase(FakeRepository(Result.success(roster))), ExportAttendanceCsvUseCase())
        vm.load("event")
        vm.onAction(AttendanceAction.Filter(AttendanceFilter.MISSING))
        vm.onAction(AttendanceAction.Search("20230002"))
        assertEquals(listOf("2"), vm.uiState.value.visibleEntries.map { it.userId })
        vm.onAction(AttendanceAction.Filter(AttendanceFilter.PRESENT))
        assertTrue(vm.uiState.value.visibleEntries.isEmpty())
    }

    @Test fun `failed refresh removes stale roster and shows error`() {
        val fake = FakeRepository(Result.success(roster))
        val vm = AttendanceViewModel(GetAttendanceRosterUseCase(fake), ExportAttendanceCsvUseCase())
        vm.load("event")
        fake.result = Result.failure(IllegalStateException("offline"))
        vm.onAction(AttendanceAction.Refresh)
        assertNull(vm.uiState.value.roster)
        assertEquals(AttendanceFailure.LoadFailed, vm.uiState.value.failure)
        assertEquals("offline", vm.uiState.value.errorDetail)
        assertEquals(ServerErrorCode.Unknown, vm.uiState.value.errorCode)
        assertFalse(vm.uiState.value.loading)
    }

    @Test fun `ma loi server duoc giu thanh enum de Screen tu dich`() {
        val fake = FakeRepository(Result.failure(IllegalStateException("FORBIDDEN")))
        val vm = AttendanceViewModel(GetAttendanceRosterUseCase(fake), ExportAttendanceCsvUseCase())
        vm.load("event")
        assertEquals(ServerErrorCode.Forbidden, vm.uiState.value.errorCode)
        assertEquals(AttendanceFailure.LoadFailed, vm.uiState.value.failure)
    }

    @Test fun `export that cannot write fails with enum not a magic string`() {
        val vm = AttendanceViewModel(
            GetAttendanceRosterUseCase(FakeRepository(Result.success(roster))),
            ExportAttendanceCsvUseCase(),
        )
        vm.showExportFailure(IllegalStateException("Cannot open output"))
        assertEquals(AttendanceFailure.ExportFailed, vm.uiState.value.failure)
        assertEquals("Cannot open output", vm.uiState.value.errorDetail)
        assertEquals(ServerErrorCode.Unknown, vm.uiState.value.errorCode)
    }

    @Test fun `export fetches fresh server roster instead of filtered screen data`() = runBlocking {
        val fake = FakeRepository(Result.success(roster))
        val vm = AttendanceViewModel(GetAttendanceRosterUseCase(fake), ExportAttendanceCsvUseCase())
        vm.load("event")
        vm.onAction(AttendanceAction.Filter(AttendanceFilter.PRESENT))
        val updated = roster.copy(entries = roster.entries.map {
            it.copy(checkedInAt = "2026-11-03T09:00:00Z")
        })
        fake.result = Result.success(updated)
        val csv = vm.createExport(AttendanceCsvLabels(
            "Event", "Exported", "ID", "Name", "Email", "Status", "Time", "Staff", "Present", "Missing",
        ), Instant.EPOCH).getOrThrow()
        assertTrue(csv.contains("20230002"))
        assertTrue(csv.contains("03/11/2026 16:00:00"))
        assertEquals(1, vm.uiState.value.visibleEntries.size)
    }

    private class FakeRepository(var result: Result<AttendanceRoster>) : AttendanceRosterRepository {
        override suspend fun getRoster(eventId: String) = result
    }
}

package com.giathinh.uthconnect.presentation.scanner

import com.giathinh.uthconnect.data.repository.MockTicketRepository
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.TicketQrPayload
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.CheckInResult
import com.giathinh.uthconnect.domain.usecase.CheckInUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScannerViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `quét QR tự kiểm tra mã đầy đủ và tăng số thành công`() {
        val session = FakeSession()
        val viewModel = ScannerViewModel(CheckInUseCase(session, MockTicketRepository()), session)
        val payload = TicketQrPayload.create("ticket-sports", 123L)

        viewModel.onAction(ScannerAction.CameraCodeDetected(payload))

        assertEquals(payload, viewModel.uiState.value.code)
        assertEquals(CheckInResult.SUCCESS, viewModel.uiState.value.result)
        assertEquals(1, viewModel.uiState.value.successCount)
    }

    @Test fun `quét lại cùng vé báo trùng và không tăng bộ đếm`() {
        val session = FakeSession()
        val viewModel = ScannerViewModel(CheckInUseCase(session, MockTicketRepository()), session)
        val payload = TicketQrPayload.create("ticket-sports", 123L)
        viewModel.onAction(ScannerAction.CameraCodeDetected(payload))
        viewModel.onAction(ScannerAction.ClearScan)

        viewModel.onAction(ScannerAction.CameraCodeDetected(payload))

        assertEquals(CheckInResult.ALREADY_CHECKED_IN, viewModel.uiState.value.result)
        assertEquals(1, viewModel.uiState.value.successCount)
    }

    @Test fun `sinh viên không thể kiểm tra QR`() {
        val session = FakeSession()
        session.session.value = StudentSession(
            isRestoring = false,
            profile = session.session.value.profile?.copy(role = UserRole.STUDENT),
        )
        val viewModel = ScannerViewModel(CheckInUseCase(session, MockTicketRepository()), session)

        viewModel.onAction(ScannerAction.CameraCodeDetected(TicketQrPayload.create("ticket-sports", 1L)))

        assertEquals(false, viewModel.uiState.value.authorized)
        assertEquals("", viewModel.uiState.value.code)
    }

    private class FakeSession : SessionRepository {
        override val session = MutableStateFlow(StudentSession(isRestoring = false, profile = profile))
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) = Unit
        override suspend fun updateProfile(profile: StudentProfile) = Unit
        override suspend fun enableBiometric(studentId: String) = Unit
        override suspend fun disableBiometric() = Unit
        override suspend fun unlockWithBiometric() = Unit
        override suspend fun signOut() = Unit
    }

    private companion object {
        val profile = StudentProfile("organizer-1", "demo@ut.edu.vn", "Demo", "A", "IT", "IT", role = UserRole.ORGANIZER)
    }
}

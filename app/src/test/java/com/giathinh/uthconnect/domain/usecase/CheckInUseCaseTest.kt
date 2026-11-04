package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.repository.MockTicketRepository
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.TicketQrPayload
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckInUseCaseTest {
    private val session = FakeSession()
    private val tickets = MockTicketRepository()
    private val checkIn = CheckInUseCase(session, tickets)

    @Test fun `QR thật chứa ID vé - check-in và cập nhật trạng thái vé`() = runBlocking {
        val payload = TicketQrPayload.create("ticket-sports", 1_234L)
        assertEquals(CheckInResult.SUCCESS, checkIn(payload))
        assertEquals(TicketStatus.CHECKED_IN, tickets.getTickets("2023040123").getOrThrow().first().status)
    }

    @Test fun `quét lại cùng QR - báo đã check-in`() = runBlocking {
        val payload = TicketQrPayload.create("ticket-sports", 1_234L)
        assertEquals(CheckInResult.SUCCESS, checkIn(payload))
        assertEquals(CheckInResult.ALREADY_CHECKED_IN, checkIn(payload))
    }

    @Test fun `vé đã check-in từ trước - báo trùng`() = runBlocking {
        assertEquals(CheckInResult.ALREADY_CHECKED_IN, checkIn(TicketQrPayload.create("ticket-club", 1L)))
    }

    @Test fun `mã thủ công có dấu cách và dài hơn sáu ký tự vẫn được chấp nhận`() = runBlocking {
        assertEquals(CheckInResult.SUCCESS, checkIn("482 917"))
    }

    @Test fun `ID vé không đăng ký - báo chưa đăng ký`() = runBlocking {
        assertEquals(CheckInResult.NOT_REGISTERED, checkIn(TicketQrPayload.create("other-ticket", 1L)))
    }

    @Test fun `QR sai cấu trúc và mã rỗng - báo không hợp lệ`() = runBlocking {
        assertEquals(CheckInResult.INVALID_CODE, checkIn("UTHCONNECT:TICKET:1:ticket-sports:bad"))
        assertEquals(CheckInResult.INVALID_CODE, checkIn("  "))
    }

    @Test fun `sinh viên không có quyền check-in`() = runBlocking {
        session.session.value = StudentSession(isRestoring = false, profile = profile.copy(role = UserRole.STUDENT))
        assertEquals(CheckInResult.FORBIDDEN, checkIn(TicketQrPayload.create("ticket-sports", 1L)))
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

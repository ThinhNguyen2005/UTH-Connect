package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterEventUseCaseTest {
    @Test
    fun `registration includes event and current student IDs`() = runTest {
        val startedAt = LocalDateTime.now()
        val registration = registerWith("2023040123")("career-day").getOrThrow()
        val finishedAt = LocalDateTime.now()

        assertEquals("career-day", registration.eventId)
        assertEquals("2023040123", registration.studentId)
        assertTrue(registration.id.isNotBlank())
        assertTrue(!registration.registeredAt.isBefore(startedAt))
        assertTrue(!registration.registeredAt.isAfter(finishedAt))
    }

    @Test
    fun `student ID is trimmed before creating registration`() = runTest {
        val registration = registerWith(" 2023040123 ")("career-day").getOrThrow()

        assertEquals("2023040123", registration.studentId)
    }

    @Test
    fun `closed event cannot be registered`() = runTest {
        val result = registerWith("2023040123")("green-campus")

        assertTrue(result.isFailure)
    }

    @Test
    fun `full event and unknown event cannot be registered`() = runTest {
        val useCase = registerWith("2023040123")

        assertTrue(useCase("android-workshop").isFailure)
        assertTrue(useCase("missing-event").isFailure)
    }

    @Test
    fun `student ID provider failure is propagated`() = runTest {
        val error = IllegalStateException("Authentication unavailable")
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.failure<String>(error)
        }
        val useCase = RegisterEventUseCase(GetEventsUseCase(StaticEventRepository()), studentIdProvider)

        assertSame(error, useCase("career-day").exceptionOrNull())
    }

    @Test
    fun `missing student ID prevents registration`() = runTest {
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.success("")
        }
        val useCase = RegisterEventUseCase(GetEventsUseCase(StaticEventRepository()), studentIdProvider)

        assertTrue(useCase("career-day").isFailure)
    }

    @Test
    fun `blank student ID prevents registration`() = runTest {
        val result = registerWith("   ")("career-day")

        assertTrue(result.isFailure)
    }

    private fun registerWith(studentId: String): RegisterEventUseCase {
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.success(studentId)
        }
        return RegisterEventUseCase(GetEventsUseCase(StaticEventRepository()), studentIdProvider)
    }
}

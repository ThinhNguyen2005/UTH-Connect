package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.repository.StaticEventRepository
import com.giathinh.uthconnect.domain.repository.StudentIdProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterEventUseCaseTest {
    @Test
    fun `registration includes event and current student IDs`() = runTest {
        val registration = registerWith("2023040123")("career-day").getOrThrow()

        assertEquals("career-day", registration.eventId)
        assertEquals("2023040123", registration.studentId)
        assertNotNull(registration.id)
    }

    @Test
    fun `closed event cannot be registered`() = runTest {
        val result = registerWith("2023040123")("green-campus")

        assertTrue(result.isFailure)
    }

    @Test
    fun `missing student ID prevents registration`() = runTest {
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.success("")
        }
        val useCase = RegisterEventUseCase(GetEventsUseCase(StaticEventRepository()), studentIdProvider)

        assertTrue(useCase("career-day").isFailure)
    }

    private fun registerWith(studentId: String): RegisterEventUseCase {
        val studentIdProvider = object : StudentIdProvider {
            override suspend fun studentId() = Result.success(studentId)
        }
        return RegisterEventUseCase(GetEventsUseCase(StaticEventRepository()), studentIdProvider)
    }
}

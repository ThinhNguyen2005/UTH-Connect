package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.data.repository.InMemoryTimetableRepository
import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.model.Timetable
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableUseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-31T17:30:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"))
    private val now = LocalDateTime.now(clock)
    private val repository = InMemoryTimetableRepository(clock)
    private val useCase = TimetableUseCase(repository)

    @Test fun mockContainsFiveClassesAndFourDeadlines() {
        assertEquals(5, repository.timetable.value.sessions.size)
        assertEquals(4, repository.timetable.value.deadlines.size)
    }
    @Test fun tomorrowFilterUsesVietnamDateAndSortsChronologically() {
        val value = repository.timetable.value
        val result = TimetableUseCase.tomorrow(value.copy(sessions = value.sessions.reversed()), now.toLocalDate())
        assertEquals(2, result.size)
        assertTrue(result.all { it.startsAt.toLocalDate() == now.toLocalDate().plusDays(1) })
        assertEquals(result.sortedBy { it.startsAt }, result)
    }
    @Test fun urgencyIncludesOverdueAndExactBoundaryButExcludesCompletedAndLater() {
        val value = Timetable(deadlines = listOf(
            Deadline("later", Course.ANDROID, "later", now.plusHours(48).plusSeconds(1)),
            Deadline("boundary", Course.ANDROID, "boundary", now.plusHours(48)),
            Deadline("done", Course.ANDROID, "done", now.minusHours(3), true),
            Deadline("overdue", Course.ANDROID, "overdue", now.minusHours(2)),
        ))
        assertEquals(listOf("overdue", "boundary"), TimetableUseCase.urgent(value, now).map { it.id })
    }
    @Test fun completionIsIdempotentAndRemovesUrgentDeadline() {
        useCase.complete("deadline-1")
        useCase.complete("deadline-1")
        assertEquals(4, repository.timetable.value.deadlines.size)
        assertTrue(repository.timetable.value.deadlines.first().completed)
        assertFalse(TimetableUseCase.urgent(repository.timetable.value, now).any { it.id == "deadline-1" })
    }
    @Test fun validFormAddsTrimmedDeadlineAndPublishesState() {
        assertTrue(useCase.add(Course.DATABASE, "  Nộp bài  ", "2026-11-02", "15:30"))
        val added = repository.timetable.value.deadlines.last()
        assertEquals("Nộp bài", added.title)
        assertEquals(LocalDateTime.of(2026, 11, 2, 15, 30), added.dueAt)
        assertFalse(added.completed)
    }
    @Test fun invalidFormDoesNotMutateData() {
        assertFalse(useCase.add(Course.ANDROID, " ", "2026-11-02", "15:30"))
        assertFalse(useCase.add(Course.ANDROID, "Bài tập", "2026-02-30", "15:30"))
        assertFalse(useCase.add(Course.ANDROID, "Bài tập", "2026-11-02", "25:00"))
        assertEquals(4, repository.timetable.value.deadlines.size)
    }
    @Test fun unknownCompletionAndEmptyFiltersAreSafe() {
        useCase.complete("unknown")
        assertFalse(repository.timetable.value.deadlines.any { it.completed })
        assertTrue(TimetableUseCase.urgent(Timetable(), now).isEmpty())
        assertTrue(TimetableUseCase.tomorrow(Timetable(), now.toLocalDate()).isEmpty())
    }
    @Test fun newRepositoryDoesNotPersistAddedDeadlines() {
        useCase.add(Course.ENGLISH, "Essay", "2026-11-03", "12:00")
        assertEquals(4, InMemoryTimetableRepository(clock).timetable.value.deadlines.size)
    }
}

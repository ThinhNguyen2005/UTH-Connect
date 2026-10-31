package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.EventTopic
import com.giathinh.uthconnect.domain.repository.EventRepository
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Dữ liệu demo cố định, không phải lịch công bố chính thức của trường. */
@Singleton
class StaticEventRepository @Inject constructor() : EventRepository {
    override suspend fun events(): Result<List<Event>> = Result.success(
        listOf(
            Event(
                id = "career-day", topic = EventTopic.CAREER_DAY, campus = Campus.BINH_THANH,
                startAt = LocalDateTime.of(2026, 11, 7, 8, 0),
                endAt = LocalDateTime.of(2026, 11, 7, 11, 30),
                capacity = 200, registeredCount = 145, status = EventStatus.OPEN,
            ),
            Event(
                id = "android-workshop", topic = EventTopic.ANDROID_WORKSHOP, campus = Campus.THU_DUC,
                startAt = LocalDateTime.of(2026, 11, 8, 8, 30),
                endAt = LocalDateTime.of(2026, 11, 8, 11, 0),
                capacity = 40, registeredCount = 40, status = EventStatus.OPEN,
            ),
            Event(
                id = "english-club", topic = EventTopic.ENGLISH_CLUB, campus = Campus.QUAN_12,
                startAt = LocalDateTime.of(2026, 11, 10, 17, 30),
                endAt = LocalDateTime.of(2026, 11, 10, 19, 0),
                capacity = 60, registeredCount = 24, status = EventStatus.OPEN,
            ),
            Event(
                id = "green-campus", topic = EventTopic.GREEN_CAMPUS, campus = Campus.THU_DUC,
                startAt = LocalDateTime.of(2026, 11, 1, 7, 0),
                endAt = LocalDateTime.of(2026, 11, 1, 10, 0),
                capacity = 80, registeredCount = 65, status = EventStatus.CLOSED,
            ),
            Event(
                id = "logistics-talk", topic = EventTopic.LOGISTICS_TALK, campus = Campus.BINH_THANH,
                startAt = LocalDateTime.of(2026, 11, 5, 14, 0),
                endAt = LocalDateTime.of(2026, 11, 5, 16, 0),
                capacity = 100, registeredCount = 32, status = EventStatus.CANCELLED,
            ),
            Event(
                id = "welcome-freshmen", topic = EventTopic.WELCOME_FRESHMEN, campus = Campus.QUAN_12,
                startAt = LocalDateTime.of(2026, 10, 25, 8, 0),
                endAt = LocalDateTime.of(2026, 10, 25, 11, 0),
                capacity = 150, registeredCount = 138, status = EventStatus.ENDED,
            ),
        ),
    )
}

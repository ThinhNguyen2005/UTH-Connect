package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.UserRole
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseMappersTest {
    @Test fun eventStatusAndTimestampMatchAndroidDomain() {
        for (status in EventStatus.entries) {
            val event = objectFromJson("""
                {
                  "id":"33333333-3333-4333-8333-333333333333",
                  "title":"Ngày hội UTH",
                  "organizer_id":"22222222-2222-4222-8222-222222222222",
                  "start_at":"2026-11-03T12:30:00+07:00",
                  "end_at":"2026-11-03T15:30:00+07:00",
                  "max_participants":100,
                  "current_registered":19,
                  "status":"${status.name}"
                }
            """).toDomainEvent()
            assertEquals(status, event.status)
            assertEquals(Instant.parse("2026-11-03T05:30:00Z").toEpochMilli(), event.startAt)
            assertEquals(Instant.parse("2026-11-03T08:30:00Z").toEpochMilli(), event.endAt)
            assertEquals(100, event.capacity)
            assertEquals(19, event.registeredCount)
            assertEquals("33333333-3333-4333-8333-333333333333", event.id)
        }
    }

    @Test fun verifiedProfileMapsRolesAndStudentData() {
        for ((remote, expected) in listOf(
            "student" to UserRole.STUDENT,
            "organizer" to UserRole.ORGANIZER,
            "admin" to UserRole.ADMIN,
        )) {
            val profile = objectFromJson("""
                {
                  "student_id":"23510001",
                  "student_id_verified":true,
                  "email":"23510001@ut.edu.vn",
                  "display_name":"Sinh viên UTH",
                  "class_name":"CN23A",
                  "role":"$remote"
                }
            """).toDomainProfile()
            assertEquals(expected, profile.role)
            assertEquals("23510001", profile.studentId)
            assertEquals("CN23A", profile.className)
            assertTrue(profile.uthVerified)
        }
    }

    @Test fun unverifiedOrMissingStudentIdCannotBecomeAuthenticatedProfile() {
        for (input in listOf(
            """{"student_id":"23510001","student_id_verified":false,"role":"admin"}""",
            """{"student_id":null,"student_id_verified":true,"role":"student"}""",
            """{"student_id":"","student_id_verified":true,"role":"student"}""",
        )) {
            assertThrows(IllegalArgumentException::class.java) {
                objectFromJson(input).toDomainProfile()
            }
        }
    }

    private fun objectFromJson(source: String): JsonObject = Json.parseToJsonElement(source) as JsonObject
}

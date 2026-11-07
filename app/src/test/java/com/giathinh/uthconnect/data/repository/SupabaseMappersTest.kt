package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.ItemCategory
import com.giathinh.uthconnect.domain.model.ListingKind
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.model.TicketStatus
import com.giathinh.uthconnect.domain.model.UserRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SupabaseMappersTest {
    @Test fun eventStatusAndTimestampMatchAndroidDomain() {
        for (status in EventStatus.entries) {
            val event = objectFromJson(
                """
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
            """,
            ).toDomainEvent()
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
            val profile = objectFromJson(
                """
                {
                  "student_id":"23510001",
                  "student_id_verified":true,
                  "email":"23510001@ut.edu.vn",
                  "display_name":"Sinh viên UTH",
                  "class_name":"CN23A",
                  "role":"$remote"
                }
            """,
            ).toDomainProfile()
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

    @Test fun toDomainTicketMapsEmbeddedEventCorrectly() {
        val raw = """
            {
              "event_id": "33333333-3333-4333-8333-333333333333",
              "status": "REGISTERED",
              "events": {
                "id": "33333333-3333-4333-8333-333333333333",
                "title": "Hội thảo AI 2026",
                "location": "Hội trường A",
                "start_at": "2026-11-03T12:30:00+07:00",
                "organizer_name": "Đoàn Thanh niên"
              }
            }
        """
        val ticket = objectFromJson(raw).toDomainTicket(
            studentId = "23510001",
            userId = "11111111-1111-4111-8111-111111111111",
            attendedEventIds = emptySet(),
        )
        assertNotNull(ticket)
        assertEquals("33333333-3333-4333-8333-333333333333:11111111-1111-4111-8111-111111111111", ticket?.id)
        assertEquals("23510001", ticket?.studentId)
        assertEquals("Hội thảo AI 2026", ticket?.eventTitle)
        assertEquals("Hội trường A", ticket?.eventLocation)
        assertEquals("Đoàn Thanh niên", ticket?.organizerName)
        assertEquals(TicketStatus.VALID, ticket?.status)
        assertEquals("33333333", ticket?.fallbackCode)
    }

    @Test fun toDomainTicketDistinguishesStatuses() {
        val base = """
            {
              "event_id": "33333333-3333-4333-8333-333333333333",
              "events": { "title": "Sự kiện" }
            }
        """
        val eventId = "33333333-3333-4333-8333-333333333333"

        // Cancelled
        val cancelled = objectFromJson("""{"event_id":"$eventId","status":"CANCELLED","events":{"title":"T"}}""")
            .toDomainTicket("23510001", "uid-1", emptySet())
        assertEquals(TicketStatus.CANCELLED, cancelled?.status)

        // Checked in
        val checkedIn = objectFromJson("""{"event_id":"$eventId","status":"REGISTERED","events":{"title":"T"}}""")
            .toDomainTicket("23510001", "uid-1", setOf(eventId))
        assertEquals(TicketStatus.CHECKED_IN, checkedIn?.status)

        // Missing embedded event returns null
        val missingEvent = objectFromJson("""{"event_id":"$eventId","status":"REGISTERED"}""")
            .toDomainTicket("23510001", "uid-1", emptySet())
        assertNull(missingEvent)
    }

    @Test fun toDomainLostFoundListingMapsAllFieldsCorrectly() {
        val raw = """
            {
              "id": "33333333-3333-4333-8333-333333333333",
              "title": "Rơi chìa khóa xe máy",
              "description": "Chìa khóa smartkey Honda gắn móc gấu nâu tại bãi xe CS1",
              "kind": "LOST",
              "category": "PERSONAL_ITEMS",
              "location": "Bãi xe CS1",
              "date": "07/11/2026",
              "contact_name": "Nguyễn Minh Thắng",
              "contact_phone": "0901234567",
              "image_url": "https://example.supabase.co/storage/v1/object/public/lost-found/items/key.jpg",
              "is_resolved": true
            }
        """
        val item = objectFromJson(raw).toDomainLostFoundListing()
        assertEquals("33333333-3333-4333-8333-333333333333", item.id)
        assertEquals("Rơi chìa khóa xe máy", item.title)
        assertEquals("Chìa khóa smartkey Honda gắn móc gấu nâu tại bãi xe CS1", item.description)
        assertEquals(ListingKind.LOST, item.kind)
        assertEquals(ItemCategory.PERSONAL_ITEMS, item.category)
        assertEquals("Bãi xe CS1", item.location)
        assertEquals("07/11/2026", item.date)
        assertEquals("Nguyễn Minh Thắng", item.contactName)
        assertEquals("0901234567", item.contactPhone)
        assertEquals("https://example.supabase.co/storage/v1/object/public/lost-found/items/key.jpg", item.imageUrl)
        assertTrue(item.isResolved)
    }

    @Test fun lostFoundListingToJsonObjectSerializesCorrectly() {
        val domain = LostFoundListing(
            id = "33333333-3333-4333-8333-333333333333",
            title = "Nhặt được tai nghe",
            description = "Tai nghe Galaxy Buds trắng tại sảnh cơ sở 1",
            kind = ListingKind.FOUND,
            category = ItemCategory.ELECTRONICS,
            location = "Sảnh toà B, CS1",
            date = "07/11/2026",
            contactName = "Lê Văn B",
            contactPhone = "0987654321",
            imageUrl = "https://example.com/bud.jpg",
            isResolved = false,
        )
        val json = domain.toJsonObject(userId = "11111111-1111-4111-8111-111111111111")
        assertEquals("33333333-3333-4333-8333-333333333333", json.string("id"))
        assertEquals("Nhặt được tai nghe", json.string("title"))
        assertEquals("FOUND", json.string("kind"))
        assertEquals("ELECTRONICS", json.string("category"))
        assertEquals("https://example.com/bud.jpg", json.string("image_url"))
        assertEquals("11111111-1111-4111-8111-111111111111", json.string("user_id"))
        assertEquals(false, json.bool("is_resolved"))
    }

    @Test fun uuidAndUrlEncodingBehaveSafely() {
        val validId = "33333333-3333-4333-8333-333333333333"
        assertEquals(validId, uuid(validId))
        assertThrows(IllegalArgumentException::class.java) {
            uuid("invalid-not-a-uuid; DROP TABLE users;")
        }

        assertEquals("a%2Bb%26c%3Dd", urlEncode("a+b&c=d"))
        assertEquals("33333333-3333-4333-8333-333333333333", urlEncode(validId))
    }

    /**
     * Parses a JSON string literal into a [JsonObject] instance.
     */
    private fun objectFromJson(source: String): JsonObject = Json.parseToJsonElement(source) as JsonObject
}

package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.core.util.ImageCompressor
import com.giathinh.uthconnect.domain.model.IncidentStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentMapperTest {
    private val record = """{
        "id":"11111111-1111-4111-8111-111111111111",
        "code":"INC-2026-000001","campus":"CS1","room_code":"H001",
        "location_detail":null,"category":"DOOR_LOCK","severity":"HIGH",
        "title":"Broken door","description":"Door cannot close",
        "image_paths":["owner/photo.webp"],"is_anonymous_public":true,
        "status":"SUBMITTED","created_at":"2026-11-28T00:00:00Z",
        "updated_at":"2026-11-28T00:00:00Z","row_version":1
    }
    """.trimIndent()

    @Test fun `maps incident fields and storage paths`() {
        val report = Json.parseToJsonElement(record).jsonObject.toIncidentReport()
        assertEquals(IncidentStatus.SUBMITTED, report.status)
        assertEquals(listOf("owner/photo.webp"), report.imagePaths)
        assertEquals(true, report.isAnonymousPublic)
        assertEquals(1L, report.rowVersion)
    }

    @Test fun `rejects unknown status rather than inventing one`() {
        val unknown = record.replace("SUBMITTED", "FUTURE_STATUS")
        assertThrows(IllegalArgumentException::class.java) {
            Json.parseToJsonElement(unknown).jsonObject.toIncidentReport()
        }
    }

    @Test fun `maps open feed without requiring reporter identity`() {
        val summary = Json.parseToJsonElement(
            """{"id":"11111111-1111-4111-8111-111111111111","code":"INC-1",
               "campus":"CS1","room_code":"H001","category":"DOOR_LOCK",
               "severity":"HIGH","title":"Broken door","status":"SUBMITTED",
               "created_at":"2026-11-28T00:00:00Z","confirmation_count":2}""",
        ).jsonObject.toIncidentSummary()
        assertEquals(2L, summary.confirmationCount)
        assertEquals("H001", summary.roomCode)
    }

    @Test fun `validates webp header and size bounds for incident upload`() {
        val validWebp = "RIFF\u0000\u0000\u0000\u0000WEBPvp8 ".encodeToByteArray()
        assertTrue(isValidWebpImage(validWebp))
        assertFalse(isValidWebpImage(ByteArray(11)))
        assertFalse(isValidWebpImage("PNG_NOT_WEBP_HEADER".encodeToByteArray()))
        val oversized = ByteArray(ImageCompressor.MAX_BYTES + 1).also {
            validWebp.copyInto(it)
        }
        assertFalse(isValidWebpImage(oversized))
    }
}

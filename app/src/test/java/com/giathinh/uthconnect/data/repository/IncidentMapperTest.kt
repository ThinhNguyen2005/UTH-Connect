package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.IncidentStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    }""".trimIndent()

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
}

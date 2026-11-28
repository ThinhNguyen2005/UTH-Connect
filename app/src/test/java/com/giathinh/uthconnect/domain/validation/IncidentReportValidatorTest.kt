package com.giathinh.uthconnect.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncidentReportValidatorTest {
    private val validator = IncidentReportValidator()
    private val requestId = "d72615aa-9c66-42ba-8153-02dfa832c923"

    private fun validate(
        room: String,
        title: String,
        description: String,
        images: Int,
    ): IncidentValidation = validator.validate("CS1", requestId, room, title, description, images)

    @Test fun `normalizes room and accepts boundary lengths and four images`() {
        val result = validate(" h001 ", "a".repeat(5), "b".repeat(10), 4)
        assertEquals("H001", result.normalizedRoomCode)
        assertTrue(result.valid)
        assertTrue(validate("H001", "a".repeat(120), "b".repeat(1000), 0).valid)
    }

    @Test fun `rejects blank room and out of range content and images`() {
        val result = validate("  ", "1234", "short", 5)
        assertFalse(result.valid)
        assertEquals(IncidentFieldError.ROOM_REQUIRED, result.roomError)
        assertEquals(IncidentFieldError.TITLE_LENGTH, result.titleError)
        assertEquals(IncidentFieldError.DESCRIPTION_LENGTH, result.descriptionError)
        assertEquals(IncidentFieldError.INVALID_IMAGE_COUNT, result.imageError)
    }

    @Test fun `rejects upper limits and negative image count`() {
        val result = validate("H001", "a".repeat(121), "b".repeat(1001), -1)
        assertEquals(IncidentFieldError.TITLE_LENGTH, result.titleError)
        assertEquals(IncidentFieldError.DESCRIPTION_LENGTH, result.descriptionError)
        assertEquals(IncidentFieldError.INVALID_IMAGE_COUNT, result.imageError)
    }

    @Test fun `counts supplementary characters consistently with database character length`() {
        assertTrue(validate("H001", "🚪".repeat(5), "🔧".repeat(10), 0).valid)
        assertEquals(
            IncidentFieldError.TITLE_LENGTH,
            validate("H001", "🚪".repeat(4), "🔧".repeat(10), 0).titleError,
        )
    }
}

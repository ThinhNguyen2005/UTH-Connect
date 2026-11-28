package com.giathinh.uthconnect.domain.validation

import java.util.Locale
import java.util.UUID

enum class IncidentFieldError {
    CAMPUS_REQUIRED,
    INVALID_REQUEST_ID,
    ROOM_REQUIRED,
    TITLE_LENGTH,
    DESCRIPTION_LENGTH,
    INVALID_IMAGE_COUNT,
}

data class IncidentValidation(
    val normalizedRoomCode: String,
    val campusError: IncidentFieldError? = null,
    val requestIdError: IncidentFieldError? = null,
    val roomError: IncidentFieldError? = null,
    val titleError: IncidentFieldError? = null,
    val descriptionError: IncidentFieldError? = null,
    val imageError: IncidentFieldError? = null,
) {
    val valid: Boolean
        get() = campusError == null && requestIdError == null && roomError == null &&
            titleError == null && descriptionError == null && imageError == null
}

class IncidentReportValidator {
    fun validate(
        campus: String,
        clientRequestId: String,
        roomCode: String,
        title: String,
        description: String,
        imageCount: Int,
    ): IncidentValidation {
        val room = roomCode.trim().uppercase(Locale.ROOT)
        val normalizedTitle = title.trim()
        val normalizedDescription = description.trim()
        return IncidentValidation(
            normalizedRoomCode = room,
            campusError = IncidentFieldError.CAMPUS_REQUIRED.takeIf { campus.isBlank() },
            requestIdError = IncidentFieldError.INVALID_REQUEST_ID.takeIf {
                runCatching { UUID.fromString(clientRequestId).toString().equals(clientRequestId, ignoreCase = true) }
                    .getOrDefault(false).not()
            },
            roomError = IncidentFieldError.ROOM_REQUIRED.takeIf { room.isEmpty() },
            titleError = IncidentFieldError.TITLE_LENGTH.takeIf { normalizedTitle.codePointCount(0, normalizedTitle.length) !in 5..120 },
            descriptionError = IncidentFieldError.DESCRIPTION_LENGTH.takeIf {
                normalizedDescription.codePointCount(0, normalizedDescription.length) !in 10..1000
            },
            imageError = IncidentFieldError.INVALID_IMAGE_COUNT.takeIf { imageCount !in 0..4 },
        )
    }
}

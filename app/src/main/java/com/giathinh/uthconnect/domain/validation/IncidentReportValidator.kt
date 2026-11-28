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

private const val MIN_TITLE_CODE_POINTS = 5
private const val MAX_TITLE_CODE_POINTS = 120
private const val MIN_DESCRIPTION_CODE_POINTS = 10
private const val MAX_DESCRIPTION_CODE_POINTS = 1000
private const val MAX_INCIDENT_IMAGE_COUNT = 4

class IncidentReportValidator {
    @Suppress("LongParameterList")
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
        val titlePoints = normalizedTitle.codePointCount(0, normalizedTitle.length)
        val descriptionPoints = normalizedDescription.codePointCount(0, normalizedDescription.length)
        return IncidentValidation(
            normalizedRoomCode = room,
            campusError = IncidentFieldError.CAMPUS_REQUIRED.takeIf { campus.isBlank() },
            requestIdError = IncidentFieldError.INVALID_REQUEST_ID.takeIf {
                runCatching {
                    UUID.fromString(clientRequestId).toString().equals(clientRequestId, ignoreCase = true)
                }.getOrDefault(false).not()
            },
            roomError = IncidentFieldError.ROOM_REQUIRED.takeIf { room.isEmpty() },
            titleError = IncidentFieldError.TITLE_LENGTH.takeIf {
                titlePoints !in MIN_TITLE_CODE_POINTS..MAX_TITLE_CODE_POINTS
            },
            descriptionError = IncidentFieldError.DESCRIPTION_LENGTH.takeIf {
                descriptionPoints !in MIN_DESCRIPTION_CODE_POINTS..MAX_DESCRIPTION_CODE_POINTS
            },
            imageError = IncidentFieldError.INVALID_IMAGE_COUNT.takeIf {
                imageCount !in 0..MAX_INCIDENT_IMAGE_COUNT
            },
        )
    }
}

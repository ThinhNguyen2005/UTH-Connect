package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.core.util.ImageCompressor
import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.model.IncidentSummary
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val INCIDENT_PAGE_SIZE = 100
private const val MEDIA_UPLOADS_BUCKET = "media_uploads"
private const val WEBP_HEADER_MIN_BYTES = 12
private const val RIFF_MAGIC_END = 4
private const val WEBP_MAGIC_OFFSET = 8
private val RIFF_BYTES = "RIFF".encodeToByteArray()
private val WEBP_BYTES = "WEBP".encodeToByteArray()

@Singleton
class SupabaseIncidentRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : IncidentRepository {
    override suspend fun uploadIncidentImage(bytes: ByteArray): Result<String> = suspendResult {
        require(isValidWebpImage(bytes)) { "INCIDENT_INVALID_IMAGE" }
        val ownerId = uuid(client.userId)
        val objectKey = "$ownerId/${UUID.randomUUID()}.webp"
        client.uploadStorageObject(
            bucket = MEDIA_UPLOADS_BUCKET,
            path = objectKey,
            bytes = bytes,
            contentType = "image/webp",
        )
        check(client.userIdOrNull?.let { runCatching { uuid(it) }.getOrNull() } == ownerId) {
            "Session changed while uploading incident image"
        }
        objectKey
    }

    override suspend fun createReport(draft: IncidentDraft): Result<IncidentReport> = suspendResult {
        val ownerId = uuid(client.userId)
        val id = client.rpc(
            "create_incident_report",
            buildJsonObject {
                put("p_client_request_id", draft.clientRequestId)
                put("p_campus", draft.campus)
                put("p_room_code", draft.roomCode)
                put("p_location_detail", draft.locationDetail)
                put("p_category", draft.category.name)
                put("p_severity", draft.severity.name)
                put("p_title", draft.title)
                put("p_description", draft.description)
                put("p_image_paths", JsonArray(draft.imagePaths.map(::JsonPrimitive)))
                put("p_is_anonymous_public", draft.isAnonymousPublic)
            },
            expectedUserId = ownerId,
        ).jsonPrimitive.content
        loadReport(id, ownerId)
    }

    override suspend fun getMyReports(): Result<List<IncidentReport>> = suspendResult {
        val ownerId = uuid(client.userId)
        val ownerFilter = urlEncode(ownerId)
        val results = mutableListOf<IncidentReport>()
        var createdAt: String? = null
        var id: String? = null
        while (true) {
            val cursor = if (createdAt != null && id != null) {
                val encodedTime = urlEncode(createdAt)
                val encodedId = urlEncode(id)
                "&or=(created_at.lt.$encodedTime,and(created_at.eq.$encodedTime,id.lt.$encodedId))"
            } else {
                ""
            }
            val rows = client.table(
                "incident_reports?select=*&reporter_id=eq.$ownerFilter$cursor" +
                    "&order=created_at.desc,id.desc&limit=$INCIDENT_PAGE_SIZE",
            ).jsonArray.map { it.jsonObject }
            results += rows.map { it.toIncidentReport() }
            if (rows.size < INCIDENT_PAGE_SIZE) break
            createdAt = requireNotNull(rows.last().string("created_at"))
            id = requireNotNull(rows.last().string("id"))
        }
        check(client.userIdOrNull?.let { runCatching { uuid(it) }.getOrNull() } == ownerId) {
            "Session changed while loading incidents"
        }
        results
    }

    override suspend fun getReport(id: String): Result<IncidentReport> = suspendResult {
        loadReport(id, uuid(client.userId))
    }

    override suspend fun getOpenReports(campus: String?): Result<List<IncidentSummary>> = suspendResult {
        val ownerId = uuid(client.userId)
        val rows = client.rpc(
            "list_open_incidents",
            buildJsonObject { put("p_campus", campus) },
            expectedUserId = ownerId,
        ).jsonArray
        check(client.userIdOrNull?.let { runCatching { uuid(it) }.getOrNull() } == ownerId) {
            "Session changed while loading incidents"
        }
        rows.map { it.jsonObject.toIncidentSummary() }
    }

    override suspend fun getStatusHistory(id: String): Result<List<IncidentStatusLog>> = suspendResult {
        val ownerId = uuid(client.userId)
        val safeId = urlEncode(uuid(id))
        val rows = client.table(
            "incident_status_logs?select=from_status,to_status,note,created_at" +
                "&incident_id=eq.$safeId&order=created_at.asc,id.asc",
        ).jsonArray
        check(client.userIdOrNull?.let { runCatching { uuid(it) }.getOrNull() } == ownerId) {
            "Session changed while loading incident history"
        }
        rows.map { row ->
            val item = row.jsonObject
            IncidentStatusLog(
                fromStatus = item.string("from_status")?.let(IncidentStatus::valueOf),
                toStatus = IncidentStatus.valueOf(requireNotNull(item.string("to_status"))),
                note = item.string("note"),
                createdAt = requireNotNull(item.string("created_at")),
            )
        }
    }

    override suspend fun confirmReport(id: String): Result<Long> = suspendResult {
        val ownerId = uuid(client.userId)
        client.rpc(
            "confirm_incident_report",
            buildJsonObject { put("p_incident_id", uuid(id)) },
            expectedUserId = ownerId,
        ).jsonPrimitive.long
    }

    override suspend fun transitionReport(
        id: String,
        expectedVersion: Int,
        targetStatus: IncidentStatus,
        note: String?,
        duplicateOfId: String?,
    ): Result<Int> = suspendResult {
        val ownerId = uuid(client.userId)
        client.rpc(
            "transition_incident_report",
            buildJsonObject {
                put("p_incident_id", uuid(id))
                put("p_expected_version", expectedVersion)
                put("p_target_status", targetStatus.name)
                put("p_note", note)
                put("p_duplicate_of_id", duplicateOfId?.let(::uuid))
            },
            expectedUserId = ownerId,
        ).jsonPrimitive.int
    }

    private suspend fun loadReport(id: String, expectedUserId: String): IncidentReport {
        val safeId = urlEncode(uuid(id))
        val row = client.table("incident_reports?select=*&id=eq.$safeId").jsonArray.singleOrNull()?.jsonObject
            ?: error("Incident report not found or not accessible")
        check(client.userIdOrNull?.let { runCatching { uuid(it) }.getOrNull() } == expectedUserId) {
            "Session changed while loading incident"
        }
        return row.toIncidentReport()
    }
}

internal fun JsonObject.toIncidentReport(): IncidentReport = IncidentReport(
    id = requireNotNull(string("id")),
    code = requireNotNull(string("code")),
    campus = requireNotNull(string("campus")),
    roomCode = requireNotNull(string("room_code")),
    locationDetail = string("location_detail"),
    category = IncidentCategory.valueOf(requireNotNull(string("category"))),
    severity = IncidentSeverity.valueOf(requireNotNull(string("severity"))),
    title = requireNotNull(string("title")),
    description = requireNotNull(string("description")),
    imagePaths = getValue("image_paths").jsonArray.map { it.jsonPrimitive.content },
    isAnonymousPublic = bool("is_anonymous_public"),
    status = IncidentStatus.valueOf(requireNotNull(string("status"))),
    createdAt = requireNotNull(string("created_at")),
    updatedAt = requireNotNull(string("updated_at")),
    rowVersion = getValue("row_version").jsonPrimitive.long,
)

internal fun JsonObject.toIncidentSummary(): IncidentSummary = IncidentSummary(
    id = requireNotNull(string("id")),
    code = requireNotNull(string("code")),
    campus = requireNotNull(string("campus")),
    roomCode = requireNotNull(string("room_code")),
    category = IncidentCategory.valueOf(requireNotNull(string("category"))),
    severity = IncidentSeverity.valueOf(requireNotNull(string("severity"))),
    title = requireNotNull(string("title")),
    status = IncidentStatus.valueOf(requireNotNull(string("status"))),
    createdAt = requireNotNull(string("created_at")),
    confirmationCount = getValue("confirmation_count").jsonPrimitive.long,
)

internal fun isValidWebpImage(bytes: ByteArray): Boolean {
    if (bytes.size !in WEBP_HEADER_MIN_BYTES..ImageCompressor.MAX_BYTES) return false
    val isRiff = bytes.copyOfRange(0, RIFF_MAGIC_END).contentEquals(RIFF_BYTES)
    val isWebp = bytes.copyOfRange(WEBP_MAGIC_OFFSET, WEBP_HEADER_MIN_BYTES).contentEquals(WEBP_BYTES)
    return isRiff && isWebp
}

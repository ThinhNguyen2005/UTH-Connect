package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentDraft
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.repository.IncidentRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val INCIDENT_PAGE_SIZE = 100

@Singleton
class SupabaseIncidentRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : IncidentRepository {
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
                "&or=(created_at.lt.${urlEncode(createdAt)},and(created_at.eq.${urlEncode(createdAt)},id.lt.${urlEncode(id)}))"
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

package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseLostFoundRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : LostFoundRepository {

    override suspend fun getListings(): Result<List<LostFoundListing>> = runCatching {
        val rows = client.table("lost_found_items?select=*&order=created_at.desc").jsonArray
        rows.map { it.jsonObject.toDomainLostFoundListing() }
    }

    override suspend fun reportItem(
        item: LostFoundListing,
        imageBytes: ByteArray?,
        imageMimeType: String?,
    ): Result<LostFoundListing> = runCatching {
        var uploadedImageUrl: String? = item.imageUrl
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val extension = if (imageMimeType?.contains("png", ignoreCase = true) == true) "png" else "jpg"
            val filename = "${UUID.randomUUID()}.$extension"
            val bucketPath = "items/$filename"
            uploadedImageUrl = client.uploadStorage(
                bucket = "lost-found",
                path = bucketPath,
                bytes = imageBytes,
                mimeType = imageMimeType ?: "image/jpeg",
            )
        }

        val itemToInsert = item.copy(imageUrl = uploadedImageUrl)
        val payload = itemToInsert.toJsonObject(userId = client.userIdOrNull)
        val response = client.insertTable("lost_found_items", payload)
        val createdRow = when (response) {
            is JsonArray -> response.firstOrNull()?.jsonObject
            is JsonObject -> response
            else -> null
        } ?: error("Failed to insert lost & found item: empty response")
        createdRow.toDomainLostFoundListing()
    }

    override suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit> = runCatching {
        val rowId = runCatching { uuid(id) }.getOrNull() ?: id
        client.patchTable(
            "lost_found_items?id=eq.${urlEncode(rowId)}",
            buildJsonObject { put("is_resolved", isResolved) },
        )
    }.map { }
}

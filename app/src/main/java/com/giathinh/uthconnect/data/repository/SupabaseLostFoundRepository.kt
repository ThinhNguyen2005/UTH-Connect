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

/**
 * Supabase-backed implementation of [LostFoundRepository].
 * Interacts with PostgREST `lost_found_items` table and Supabase Storage `lost-found` bucket.
 */
@Singleton
class SupabaseLostFoundRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : LostFoundRepository {

    /**
     * Retrieves all lost and found listings ordered by most recently created first.
     * Allows anonymous reading so all students can browse listings without logging in.
     */
    override suspend fun getListings(): Result<List<LostFoundListing>> = runCatching {
        val rows = client.table("lost_found_items?select=*&order=created_at.desc", allowAnonymous = true).jsonArray
        rows.map { it.jsonObject.toDomainLostFoundListing() }
    }

    /**
     * Reports a new lost or found listing with optional proof photo upload.
     * Requires the user to be authenticated in accordance with RLS policies.
     *
     * @param item The listing metadata to create.
     * @param imageBytes Optional raw byte array of the attached photo.
     * @param imageMimeType Optional MIME type of the attached photo.
     * @return Result containing the created [LostFoundListing].
     */
    override suspend fun reportItem(
        item: LostFoundListing,
        imageBytes: ByteArray?,
        imageMimeType: String?,
    ): Result<LostFoundListing> = runCatching {
        var uploadedImageUrl: String? = item.imageUrl
        if (imageBytes != null && imageBytes.isNotEmpty()) {
            val (extension, resolvedMime) = when {
                imageMimeType?.contains("png", ignoreCase = true) == true -> "png" to "image/png"
                imageMimeType?.contains("webp", ignoreCase = true) == true -> "webp" to "image/webp"
                else -> "jpg" to "image/jpeg"
            }
            val filename = "${UUID.randomUUID()}.$extension"
            val bucketPath = "items/$filename"
            uploadedImageUrl = client.uploadStorage(
                bucket = "lost-found",
                path = bucketPath,
                bytes = imageBytes,
                mimeType = resolvedMime,
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

    /**
     * Toggles the resolved status of a listing.
     * Requires the user to be authenticated and the author/owner of the listing.
     *
     * @param id Unique identifier of the listing.
     * @param isResolved New resolved boolean status.
     */
    override suspend fun toggleResolved(id: String, isResolved: Boolean): Result<Unit> = runCatching {
        val rowId = runCatching { uuid(id) }.getOrNull() ?: id
        val response = client.patchTable(
            "lost_found_items?id=eq.${urlEncode(rowId)}",
            buildJsonObject { put("is_resolved", isResolved) },
        )
        val updatedRows = when (response) {
            is JsonArray -> response.size
            is JsonObject -> 1
            else -> 0
        }
        if (updatedRows == 0) {
            throw SupabaseHttpException(
                status = java.net.HttpURLConnection.HTTP_FORBIDDEN,
                message = "No permission to update this listing",
            )
        }
    }
}

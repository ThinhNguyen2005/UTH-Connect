package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.LostFoundListing
import com.giathinh.uthconnect.domain.repository.LostFoundRepository
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val LOST_FOUND_BUCKET = "lost-found"

/**
 * Supabase-backed implementation of [LostFoundRepository].
 * Interacts with PostgREST `lost_found_items` table and Supabase Storage `lost-found` bucket.
 */
@Singleton
class SupabaseLostFoundRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : LostFoundRepository {

    override fun currentUserId(): String? = client.userIdOrNull

    /**
     * Retrieves all lost and found listings ordered by most recently created first.
     * Allows anonymous reading so all students can browse listings without logging in.
     */
    override suspend fun getListings(): Result<List<LostFoundListing>> = runCatching {
        val rows = client.table("lost_found_items?select=*&order=created_at.desc", allowAnonymous = true).jsonArray
        rows.map { it.jsonObject.toDomainLostFoundListing() }
    }.onFailure { if (it is CancellationException) throw it }

    /**
     * Reports a new lost or found listing with optional proof photo upload.
     * Requires the user to be authenticated in accordance with RLS policies.
     */
    override suspend fun reportItem(
        item: LostFoundListing,
        imageBytes: ByteArray?,
        imageMimeType: String?,
    ): Result<LostFoundListing> {
        var uploadedBucketPath: String? = null
        return runCatching {
            val uploaded = uploadPhotoIfNeeded(imageBytes, imageMimeType)
            uploadedBucketPath = uploaded?.first
            val itemToInsert = item.copy(imageUrl = uploaded?.second ?: item.imageUrl)
            val payload = itemToInsert.toJsonObject(userId = client.userIdOrNull)
            val response = client.insertTable("lost_found_items", payload)
            val createdRow = when (response) {
                is JsonArray -> response.firstOrNull()?.jsonObject
                is JsonObject -> response
                else -> null
            } ?: error("Failed to insert lost & found item: empty response")
            createdRow.toDomainLostFoundListing()
        }.onFailure { error ->
            uploadedBucketPath?.let { path ->
                withContext(NonCancellable) {
                    runCatching { client.deleteStorage(LOST_FOUND_BUCKET, path) }
                }
            }
            if (error is CancellationException) throw error
        }
    }

    /**
     * Updates an existing lost or found listing owned by the current user.
     */
    override suspend fun updateItem(
        item: LostFoundListing,
        imageBytes: ByteArray?,
        imageMimeType: String?,
        removeExistingImage: Boolean,
    ): Result<LostFoundListing> {
        var uploadedBucketPath: String? = null
        return runCatching {
            val previousImageUrl = item.imageUrl
            val uploaded = uploadPhotoIfNeeded(imageBytes, imageMimeType)
            uploadedBucketPath = uploaded?.first
            val resolvedImageUrl = when {
                uploaded != null -> uploaded.second
                removeExistingImage -> null
                else -> previousImageUrl
            }

            val updatedRow = patchUpdatedRow(item = item, resolvedImageUrl = resolvedImageUrl)

            if (uploadedBucketPath != null || removeExistingImage) {
                val oldPath = extractStoragePath(previousImageUrl)
                if (oldPath != null && oldPath != uploadedBucketPath) {
                    runCatching { client.deleteStorage(LOST_FOUND_BUCKET, oldPath) }
                }
            }

            updatedRow.toDomainLostFoundListing()
        }.onFailure { error ->
            uploadedBucketPath?.let { path ->
                withContext(NonCancellable) {
                    runCatching { client.deleteStorage(LOST_FOUND_BUCKET, path) }
                }
            }
            if (error is CancellationException) throw error
        }
    }

    private suspend fun patchUpdatedRow(
        item: LostFoundListing,
        resolvedImageUrl: String?,
    ): JsonObject {
        val rowId = runCatching { uuid(item.id) }.getOrNull() ?: item.id
        val payload = buildJsonObject {
            put("title", item.title)
            put("description", item.description)
            put("kind", item.kind.name)
            put("category", item.category.name)
            put("location", item.location)
            put("contact_name", item.contactName)
            put("contact_phone", item.contactPhone)
            if (resolvedImageUrl != null) {
                put("image_url", resolvedImageUrl)
            } else {
                put("image_url", JsonNull)
            }
        }
        val response = client.patchTable("lost_found_items?id=eq.${urlEncode(rowId)}", payload)
        return when (response) {
            is JsonArray -> response.firstOrNull()?.jsonObject
            is JsonObject -> response
            else -> null
        } ?: throw SupabaseHttpException(
            status = HttpURLConnection.HTTP_FORBIDDEN,
            message = "No permission to update this listing",
        )
    }

    /**
     * Deletes a lost or found listing owned by the current user and best-effort cleans its storage photo.
     */
    override suspend fun deleteItem(id: String, imageUrl: String?): Result<Unit> = runCatching {
        client.userId
        val rowId = runCatching { uuid(id) }.getOrNull() ?: id
        client.deleteTable("lost_found_items?id=eq.${urlEncode(rowId)}")
        extractStoragePath(imageUrl)?.let { path ->
            runCatching { client.deleteStorage(LOST_FOUND_BUCKET, path) }
        }
        Unit
    }.onFailure { if (it is CancellationException) throw it }

    /**
     * Toggles the resolved status of a listing.
     * Requires the user to be authenticated and the author/owner of the listing.
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
                status = HttpURLConnection.HTTP_FORBIDDEN,
                message = "No permission to update this listing",
            )
        }
    }.onFailure { if (it is CancellationException) throw it }

    private suspend fun uploadPhotoIfNeeded(
        imageBytes: ByteArray?,
        imageMimeType: String?,
    ): Pair<String, String>? {
        if (imageBytes == null || imageBytes.isEmpty()) return null
        val (extension, resolvedMime) = when {
            imageMimeType?.contains("png", ignoreCase = true) == true -> "png" to "image/png"
            imageMimeType?.contains("webp", ignoreCase = true) == true -> "webp" to "image/webp"
            else -> "jpg" to "image/jpeg"
        }
        val userId = client.userId
        val bucketPath = "items/$userId/${UUID.randomUUID()}.$extension"
        val publicUrl = client.uploadStorage(
            bucket = LOST_FOUND_BUCKET,
            path = bucketPath,
            bytes = imageBytes,
            mimeType = resolvedMime,
        )
        return bucketPath to publicUrl
    }

    private fun extractStoragePath(imageUrl: String?): String? {
        if (imageUrl.isNullOrBlank()) return null
        val prefix = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$LOST_FOUND_BUCKET/"
        return imageUrl.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.takeIf { it.isNotBlank() }
    }
}

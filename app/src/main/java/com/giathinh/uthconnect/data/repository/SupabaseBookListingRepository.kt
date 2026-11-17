package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.domain.model.BookListingStatus
import com.giathinh.uthconnect.domain.repository.BookListingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseBookListingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: SupabaseRestClient,
) : BookListingRepository {

    private val revision = MutableStateFlow(0L)

    override fun currentUserId(): String? = runCatching { client.userId }.getOrNull()

    override fun observeListings(): Flow<List<BookListing>> = revision.map {
        fetchListings()
    }

    override fun refresh() {
        revision.value++
    }

    private suspend fun fetchListings(): List<BookListing> {
        val rows = client.table("book_listings?select=*&order=created_at.desc").jsonArray
        return rows.map { it.jsonObject.toBookListing() }
    }

    override suspend fun saveListing(listing: BookListing): Result<Unit> = withContext(Dispatchers.IO) {
        suspendResult {
            val uid = client.userId
            val listingId = uuid(listing.id)

            // Verify existing ownership if already exists
            val existing = client.table("book_listings?select=owner_user_id,image_path&id=eq.${urlEncode(listingId)}")
                .jsonArray.singleOrNull()?.jsonObject

            if (existing != null) {
                val ownerId = existing.string("owner_user_id")
                check(ownerId == uid) { "Only the listing owner can edit it" }
            }

            val oldImagePath = existing?.string("image_path")
            var newlyUploadedPath: String? = null
            var writeStarted = false

            try {
                if (listing.photoUri != null) {
                    val (imageBytes, mimeType) = readAndValidateImage(context, listing.photoUri)
                    val objectPath = "$uid/$listingId/${UUID.randomUUID()}.${imageExtension(mimeType)}"
                    client.uploadStorage(BUCKET_NAME, objectPath, mimeType, imageBytes)
                    newlyUploadedPath = objectPath
                }

                val finalImagePath = newlyUploadedPath ?: listing.imagePath ?: oldImagePath

                val payload = buildJsonObject {
                    put("id", listingId)
                    put("owner_user_id", uuid(uid))
                    put("title", listing.title.trim())
                    put("subject", listing.subject.trim())
                    put("condition", listing.condition.name)
                    put("deal_type", listing.dealType.name)
                    if (listing.dealType == BookDealType.SALE && listing.priceVnd != null) {
                        put("price_vnd", listing.priceVnd)
                    } else {
                        put("price_vnd", JsonNull)
                    }
                    put("description", listing.description.trim())
                    put("seller_name", listing.sellerName.trim())
                    put("contact", listing.contact.trim())
                    if (finalImagePath != null) {
                        put("image_path", finalImagePath)
                    } else {
                        put("image_path", JsonNull)
                    }
                    put("status", listing.status.name)
                    put("updated_at", Instant.now().toString())
                }

                // Once the request starts, a timeout or mapping error cannot tell us whether
                // the server committed the row. Keep the image in that ambiguous case.
                writeStarted = true
                if (existing != null) {
                    client.patchTable("book_listings?id=eq.${urlEncode(listingId)}", payload)
                } else {
                    client.insertTable("book_listings", payload)
                }

                // Cleanup old image if replaced
                if (newlyUploadedPath != null && !oldImagePath.isNullOrBlank() && oldImagePath != newlyUploadedPath) {
                    runCatching { client.deleteStorage(BUCKET_NAME, oldImagePath) }
                }

                refresh()
            } catch (error: Exception) {
                // Only clean up when no database write could have referenced the image.
                if (!writeStarted && newlyUploadedPath != null) {
                    try {
                        client.deleteStorage(BUCKET_NAME, newlyUploadedPath)
                    } catch (cleanupFailure: Exception) {
                        Log.w(
                            "SupabaseBookListing",
                            "Compensating storage cleanup failed for $newlyUploadedPath",
                            cleanupFailure,
                        )
                    }
                }
                throw error
            }
        }
    }

    override suspend fun updateStatus(
        id: String,
        status: BookListingStatus,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        suspendResult {
            val listingId = uuid(id)
            val payload = buildJsonObject {
                put("status", status.name)
                put("updated_at", Instant.now().toString())
            }
            client.patchTable("book_listings?id=eq.${urlEncode(listingId)}", payload)
            refresh()
        }
    }

    override suspend fun deleteListing(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        suspendResult {
            val uid = client.userId
            val listingId = uuid(id)
            val existing = client.table("book_listings?select=owner_user_id,image_path&id=eq.${urlEncode(listingId)}")
                .jsonArray.singleOrNull()?.jsonObject

            if (existing != null) {
                val ownerId = existing.string("owner_user_id")
                check(ownerId == uid) { "Only the listing owner can delete it" }
                val imagePath = existing.string("image_path")

                client.deleteTable("book_listings?id=eq.${urlEncode(listingId)}")

                if (!imagePath.isNullOrBlank()) {
                    runCatching { client.deleteStorage(BUCKET_NAME, imagePath) }
                }
            }
            refresh()
        }
    }

    private fun JsonObject.toBookListing(): BookListing {
        val imagePath = string("image_path")
        val photoUrl = imagePath?.takeIf { it.isNotBlank() }?.let {
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET_NAME/$it"
        }
        val createdAtMillis = runCatching {
            Instant.parse(string("created_at")).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

        return BookListing(
            id = requireNotNull(string("id")),
            title = string("title").orEmpty(),
            subject = string("subject").orEmpty(),
            condition = runCatching { BookCondition.valueOf(string("condition").orEmpty()) }
                .getOrDefault(BookCondition.GOOD),
            dealType = runCatching { BookDealType.valueOf(string("deal_type").orEmpty()) }
                .getOrDefault(BookDealType.SALE),
            priceVnd = (get("price_vnd") as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull,
            description = string("description").orEmpty(),
            sellerName = string("seller_name").orEmpty(),
            contact = string("contact").orEmpty(),
            ownerUid = string("owner_user_id").orEmpty(),
            photoUrl = photoUrl,
            status = runCatching { BookListingStatus.valueOf(string("status").orEmpty()) }
                .getOrDefault(BookListingStatus.AVAILABLE),
            createdAt = createdAtMillis,
            imagePath = imagePath,
        )
    }

    companion object {
        const val BUCKET_NAME = "book_listings"
        private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024 // 5 MB
        private const val MIN_IMAGE_HEADER_BYTES = 12
        private const val WEBP_FORMAT_OFFSET = 8
        private const val IMAGE_READ_BUFFER_BYTES = 8192
        private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        private val RIFF_SIGNATURE = "RIFF".toByteArray(Charsets.US_ASCII)
        private val WEBP_SIGNATURE = "WEBP".toByteArray(Charsets.US_ASCII)

        internal fun imageExtension(mimeType: String): String = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("Unsupported image type")
        }

        internal fun validateImageBytes(bytes: ByteArray): Pair<ByteArray, String> {
            require(bytes.size <= MAX_PHOTO_BYTES) { "Image exceeds maximum allowed size (5 MB)" }
            require(bytes.size >= MIN_IMAGE_HEADER_BYTES) { "File is too small to be a valid image" }

            val mimeType = when {
                // JPEG magic bytes: FF D8 FF
                bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"

                // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
                bytes.copyOfRange(0, PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE) -> "image/png"

                // WebP magic bytes: RIFF....WEBP
                bytes.copyOfRange(0, RIFF_SIGNATURE.size).contentEquals(RIFF_SIGNATURE) &&
                    bytes.copyOfRange(WEBP_FORMAT_OFFSET, WEBP_FORMAT_OFFSET + WEBP_SIGNATURE.size)
                        .contentEquals(WEBP_SIGNATURE) -> "image/webp"

                else -> error("Invalid image format. Only JPEG, PNG, and WebP are allowed.")
            }
            return bytes to mimeType
        }

        internal fun readAndValidateStream(inputStream: InputStream): Pair<ByteArray, String> {
            val buffer = ByteArray(IMAGE_READ_BUFFER_BYTES)
            val output = ByteArrayOutputStream()
            var totalRead = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                require(totalRead <= MAX_PHOTO_BYTES) { "Image exceeds maximum allowed size (5 MB)" }
                output.write(buffer, 0, bytesRead)
            }
            val bytes = output.toByteArray()
            return validateImageBytes(bytes)
        }

        internal suspend fun readAndValidateImage(
            context: Context,
            uriString: String,
        ): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open image file: $uriString")
            stream.use { readAndValidateStream(it) }
        }
    }
}

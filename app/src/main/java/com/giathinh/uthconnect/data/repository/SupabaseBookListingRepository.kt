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
import kotlinx.coroutines.CancellationException
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
            val existing = checkExistingOwnership(listingId, uid)
            val oldImagePath = existing?.string("image_path")
            var newlyUploadedPath: String? = null

            try {
                if (listing.photoUri != null && !listing.photoUri.startsWith("http")) {
                    newlyUploadedPath = uploadImage(context, uid, listingId, listing.photoUri)
                }

                val finalImagePath = newlyUploadedPath ?: listing.imagePath ?: oldImagePath
                val payload = buildListingPayload(listing, listingId, uid, finalImagePath)

                if (existing != null) {
                    client.patchTable("book_listings?id=eq.${urlEncode(listingId)}", payload)
                } else {
                    client.insertTable("book_listings", payload)
                }

                cleanupReplacedImage(newlyUploadedPath, oldImagePath)
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                compensateUploadFailure(newlyUploadedPath)
                throw error
            }
        }
    }

    private suspend fun checkExistingOwnership(listingId: String, currentUid: String): JsonObject? {
        val query = "book_listings?select=owner_user_id,image_path&id=eq.${urlEncode(listingId)}"
        val existing = client.table(query).jsonArray.singleOrNull()?.jsonObject
        if (existing != null) {
            val ownerId = existing.string("owner_user_id")
            check(ownerId == currentUid) { "Only the listing owner can edit it" }
        }
        return existing
    }

    private suspend fun uploadImage(ctx: Context, uid: String, listingId: String, uriString: String): String {
        val (imageBytes, mimeType) = readAndValidateImage(ctx, uriString)
        val objectPath = "$uid/$listingId/${UUID.randomUUID()}.jpg"
        client.uploadStorage(BUCKET_NAME, objectPath, mimeType, imageBytes)
        return objectPath
    }

    private fun buildListingPayload(
        listing: BookListing,
        listingId: String,
        uid: String,
        finalImagePath: String?,
    ): JsonObject = buildJsonObject {
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

    private suspend fun cleanupReplacedImage(newPath: String?, oldPath: String?) {
        if (newPath != null && !oldPath.isNullOrBlank() && oldPath != newPath) {
            runCatching { client.deleteStorage(BUCKET_NAME, oldPath) }
        }
    }

    private suspend fun compensateUploadFailure(newlyUploadedPath: String?) {
        if (newlyUploadedPath != null) {
            try {
                client.deleteStorage(BUCKET_NAME, newlyUploadedPath)
            } catch (cleanupFailure: Exception) {
                Log.w(
                    TAG,
                    "Compensating storage cleanup failed for $newlyUploadedPath",
                    cleanupFailure,
                )
            }
        }
    }

    override suspend fun updateStatus(id: String, status: BookListingStatus): Result<Unit> = withContext(Dispatchers.IO) {
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
            val query = "book_listings?select=owner_user_id,image_path&id=eq.${urlEncode(listingId)}"
            val existing = client.table(query).jsonArray.singleOrNull()?.jsonObject

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

        val parsedCondition = runCatching {
            BookCondition.valueOf(string("condition").orEmpty())
        }.getOrDefault(BookCondition.GOOD)

        val parsedDealType = runCatching {
            BookDealType.valueOf(string("deal_type").orEmpty())
        }.getOrDefault(BookDealType.SALE)

        val parsedStatus = runCatching {
            BookListingStatus.valueOf(string("status").orEmpty())
        }.getOrDefault(BookListingStatus.AVAILABLE)

        return BookListing(
            id = requireNotNull(string("id")),
            title = string("title").orEmpty(),
            subject = string("subject").orEmpty(),
            condition = parsedCondition,
            dealType = parsedDealType,
            priceVnd = (get("price_vnd") as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull,
            description = string("description").orEmpty(),
            sellerName = string("seller_name").orEmpty(),
            contact = string("contact").orEmpty(),
            ownerUid = string("owner_user_id").orEmpty(),
            photoUrl = photoUrl,
            status = parsedStatus,
            createdAt = createdAtMillis,
            imagePath = imagePath,
        )
    }

    companion object {
        private const val TAG = "SupabaseBookListing"
        const val BUCKET_NAME = "book_listings"
        private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024 // 5 MB
        private const val MIN_IMAGE_BYTES = 12
        private const val BUFFER_SIZE = 8192

        private const val JPEG_BYTE_0 = 0xFF.toByte()
        private const val JPEG_BYTE_1 = 0xD8.toByte()
        private const val JPEG_BYTE_2 = 0xFF.toByte()

        private const val PNG_BYTE_0 = 0x89.toByte()
        private const val PNG_BYTE_1 = 0x50.toByte()
        private const val PNG_BYTE_2 = 0x4E.toByte()
        private const val PNG_BYTE_3 = 0x47.toByte()

        private fun isJpeg(b: ByteArray): Boolean = b[0] == JPEG_BYTE_0 && b[1] == JPEG_BYTE_1 && b[2] == JPEG_BYTE_2

        private fun isPng(b: ByteArray): Boolean = b[0] == PNG_BYTE_0 && b[1] == PNG_BYTE_1 && b[2] == PNG_BYTE_2 && b[3] == PNG_BYTE_3

        private fun isWebp(b: ByteArray): Boolean = b[0] == 'R'.code.toByte() && b[1] == 'I'.code.toByte() &&
            b[2] == 'F'.code.toByte() && b[3] == 'F'.code.toByte() &&
            b[8] == 'W'.code.toByte() && b[9] == 'E'.code.toByte() &&
            b[10] == 'B'.code.toByte() && b[11] == 'P'.code.toByte()

        internal fun validateImageBytes(bytes: ByteArray): Pair<ByteArray, String> {
            require(bytes.size <= MAX_PHOTO_BYTES) { "Image exceeds maximum allowed size (5 MB)" }
            require(bytes.size >= MIN_IMAGE_BYTES) { "File is too small to be a valid image" }

            val mimeType = when {
                isJpeg(bytes) -> "image/jpeg"
                isPng(bytes) -> "image/png"
                isWebp(bytes) -> "image/webp"
                else -> error("Invalid image format. Only JPEG, PNG, and WebP are allowed.")
            }
            return bytes to mimeType
        }

        internal fun readAndValidateStream(inputStream: InputStream): Pair<ByteArray, String> {
            val buffer = ByteArray(BUFFER_SIZE)
            val output = ByteArrayOutputStream()
            var totalRead = 0L
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                totalRead += bytesRead
                require(totalRead <= MAX_PHOTO_BYTES) { "Image exceeds maximum allowed size (5 MB)" }
                output.write(buffer, 0, bytesRead)
            }
            return validateImageBytes(output.toByteArray())
        }

        internal suspend fun readAndValidateImage(context: Context, uriString: String): Pair<ByteArray, String> = withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val stream = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open image file: $uriString")
            stream.use { readAndValidateStream(it) }
        }
    }
}

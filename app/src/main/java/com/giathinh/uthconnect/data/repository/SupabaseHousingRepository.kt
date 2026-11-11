package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.giathinh.uthconnect.BuildConfig
import com.giathinh.uthconnect.domain.model.CampusReference
import com.giathinh.uthconnect.domain.model.GenderPreference
import com.giathinh.uthconnect.domain.model.HostVerificationMethod
import com.giathinh.uthconnect.domain.model.HousingListing
import com.giathinh.uthconnect.domain.model.HousingListingStatus
import com.giathinh.uthconnect.domain.model.HousingListingType
import com.giathinh.uthconnect.domain.model.RoomAmenity
import com.giathinh.uthconnect.domain.model.UthCampus
import com.giathinh.uthconnect.domain.repository.HousingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nguồn dữ liệu thật của Ghép trọ trên `housing_posts` (RLS giữ quyền sở hữu,
 * giới hạn 2 tin mở và ngưỡng báo cáo ở backend). Ảnh cục bộ (`content://`)
 * được upload lên bucket [IMAGE_BUCKET] trước khi insert; insert thất bại thì
 * ảnh vừa upload bị dọn để không mồ côi trong Storage.
 *
 * `distanceToCampusM` luôn tính lại từ tọa độ thật qua [CampusReference]; tin
 * chưa gắn vị trí (kể cả tin đời cũ lưu điểm chuẩn cũ) trả khoảng cách 0 và
 * tọa độ (0,0) để UI không bịa số km.
 */
@Singleton
class SupabaseHousingRepository @Inject constructor(
    private val client: SupabaseRestClient,
    @ApplicationContext private val context: Context,
) : HousingRepository {

    override suspend fun getListings(): Result<List<HousingListing>> = suspendResult {
        client.table("housing_posts?select=*&status=eq.OPEN&available_slots=gt.0&order=created_at.desc&limit=100")
            .jsonArray.map { it.jsonObject.toHousingListing() }
    }

    override suspend fun getListingById(id: String): Result<HousingListing> = suspendResult {
        client.table("housing_posts?select=*&id=eq.${urlEncode(id)}")
            .jsonArray.singleOrNull()?.jsonObject?.toHousingListing()
            ?: throw NoSuchElementException("Không tìm thấy tin: $id")
    }

    override suspend fun createListing(listing: HousingListing): Result<HousingListing> = suspendResult {
        val uploadedPaths = mutableListOf<String>()
        try {
            val images = resolveImages(listing.images, uploadedPaths)
            val payload = listing.toPayload(images, includeAuthor = true)
            client.insertTable("housing_posts?select=*", payload).jsonArray.single().jsonObject.toHousingListing()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            deleteObjects(uploadedPaths)
            throw error
        }
    }

    override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = suspendResult {
        val authorFilter = "id=eq.${urlEncode(listing.id)}&author_id=eq.${urlEncode(client.userId)}"
        val previousImages = client.table("housing_posts?select=images&$authorFilter")
            .jsonArray.singleOrNull()?.jsonObject
            ?.get("images")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        val uploadedPaths = mutableListOf<String>()
        try {
            val images = resolveImages(listing.images, uploadedPaths)
            // status/report_count không gửi kèm: trigger backend chỉ cho đổi qua RPC.
            val payload = listing.toPayload(images, includeAuthor = false)
            val updated = client.patchTable("housing_posts?$authorFilter&select=*", payload)
                .jsonArray.singleOrNull()?.jsonObject?.toHousingListing()
                ?: throw NoSuchElementException("Không tìm thấy tin để cập nhật: ${listing.id}")
            deleteObjects(previousImages.filter { it !in images }.mapNotNull { storagePathOf(it) })
            updated
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            deleteObjects(uploadedPaths)
            throw error
        }
    }

    override suspend fun markListingFulfilled(id: String): Result<Unit> = suspendResult {
        client.rpc("mark_housing_listing_fulfilled", buildJsonObject { put("p_listing_id", uuid(id)) })
        Unit
    }

    override suspend fun deleteListing(id: String): Result<Unit> = suspendResult {
        val authorFilter = "id=eq.${urlEncode(id)}&author_id=eq.${urlEncode(client.userId)}"
        val images = client.table("housing_posts?select=images&$authorFilter")
            .jsonArray.singleOrNull()?.jsonObject
            ?.get("images")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        client.deleteTable("housing_posts?$authorFilter")
        deleteObjects(images.mapNotNull { storagePathOf(it) })
        Unit
    }

    override suspend fun reportListing(listingId: String, reason: String): Result<Unit> = suspendResult {
        client.rpc(
            "report_housing_listing",
            buildJsonObject {
                put("p_listing_id", uuid(listingId))
                put("p_reason", reason)
            },
        )
        Unit
    }

    /**
     * Giữ URL có sẵn; URI cục bộ thì đọc bytes, upload và trả public URL.
     * Chạy song song tối đa [MAX_PARALLEL_UPLOADS] ảnh trên IO: đọc file + giải mã
     * bitmap là việc chặn luồng (để trên Main sẽ đơ UI), còn upload tuần tự bắt
     * người đăng tin chờ cộng dồn thời gian của từng ảnh.
     */
    private suspend fun resolveImages(
        images: List<String>,
        uploadedPaths: MutableList<String>,
    ): List<String> = coroutineScope {
        val gate = Semaphore(MAX_PARALLEL_UPLOADS)
        val uploads = images.map { image ->
            async(Dispatchers.IO) {
                if (image.startsWith("http://") || image.startsWith("https://")) {
                    image
                } else {
                    gate.withPermit { uploadLocalImage(image, uploadedPaths) }
                }
            }
        }
        uploads.awaitAll()
    }

    private suspend fun uploadLocalImage(uriString: String, uploadedPaths: MutableList<String>): String {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val original = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Không đọc được ảnh: $uriString")
        require(original.size <= MAX_IMAGE_BYTES) { "Ảnh vượt quá dung lượng cho phép" }
        // Ảnh điện thoại gốc vài MB kéo chậm cả upload lẫn tải feed; thu nhỏ còn
        // cạnh dài tối đa 1280px và nén JPEG là đủ nét cho card phòng.
        val bytes = downscaleJpeg(original)
        val path = "${client.userId}/${UUID.randomUUID()}.jpg"
        client.uploadStorageObject(IMAGE_BUCKET, path, bytes, "image/jpeg")
        synchronized(uploadedPaths) { uploadedPaths += path }
        return "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$IMAGE_BUCKET/$path"
    }

    private fun downscaleJpeg(original: ByteArray): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_DIMENSION_PX) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(original, 0, original.size, options)
            ?: throw IllegalArgumentException("Ảnh không giải mã được")
        val output = ByteArrayOutputStream()
        try {
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) { "Nén ảnh thất bại" }
            return output.toByteArray()
        } finally {
            bitmap.recycle()
        }
    }

    /** Chỉ đụng ảnh thuộc bucket của module; URL ngoài (nếu có) không bị xóa nhầm. */
    private fun storagePathOf(url: String): String? {
        val prefix = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$IMAGE_BUCKET/"
        return url.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
    }

    private suspend fun deleteObjects(paths: List<String>) {
        paths.forEach { path ->
            runCatching { client.deleteStorageObject(IMAGE_BUCKET, path) }
        }
    }

    private fun HousingListing.toPayload(images: List<String>, includeAuthor: Boolean): JsonObject = buildJsonObject {
        if (includeAuthor) put("author_id", uuid(client.userId))
        put("type", type.name)
        put("campus", campus.name)
        put("title", title)
        put("description", description)
        put("host_name", hostName)
        put("host_student_id", hostStudentId)
        put("contact_phone", contactPhone)
        put("contact_zalo_url", contactZaloUrl)
        put("images", JsonArray(images.map { JsonPrimitive(it) }))
        put("monthly_rent_vnd", monthlyRentVnd)
        put("deposit_vnd", depositVnd)
        put("utilities_included", utilitiesIncluded)
        put("available_slots", availableSlots)
        put("gender_preference", genderPreference.name)
        put("schedule_note", scheduleNote)
        put("amenities", JsonArray(amenities.map { JsonPrimitive(it.name) }))
        put("address_label", addressLabel)
        val (lat, lng) = if (latitude == 0.0 && longitude == 0.0) CampusReference.of(campus) else latitude to longitude
        put("latitude", lat)
        put("longitude", lng)
        put("area_m2", areaM2?.let { JsonPrimitive(it) } ?: JsonNull)
        put("verified", verified)
        put("verification_method", verificationMethod?.name?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObject.toHousingListing(): HousingListing {
        val campus = UthCampus.valueOf(requireNotNull(string("campus")))
        val latitude = (get("latitude") as? JsonPrimitive)?.doubleOrNull ?: 0.0
        val longitude = (get("longitude") as? JsonPrimitive)?.doubleOrNull ?: 0.0
        // Điểm chuẩn do repository tự điền (kể cả điểm cũ trước 11/11) không phải
        // vị trí thật của phòng — đưa về (0,0) để UI báo "chưa gắn vị trí" và
        // deep-link bản đồ tìm theo địa chỉ thay vì dẫn tới điểm placeholder.
        val hasRealLocation = (latitude != 0.0 || longitude != 0.0) &&
            !CampusReference.isReferencePoint(latitude, longitude)
        return HousingListing(
            id = requireNotNull(string("id")),
            type = HousingListingType.valueOf(requireNotNull(string("type"))),
            campus = campus,
            title = string("title").orEmpty(),
            description = string("description").orEmpty(),
            hostName = string("host_name").orEmpty(),
            hostStudentId = string("host_student_id").orEmpty(),
            contactPhone = string("contact_phone").orEmpty(),
            contactZaloUrl = string("contact_zalo_url").orEmpty(),
            images = get("images")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
            monthlyRentVnd = int("monthly_rent_vnd"),
            depositVnd = int("deposit_vnd"),
            utilitiesIncluded = bool("utilities_included"),
            availableSlots = int("available_slots"),
            genderPreference = GenderPreference.valueOf(string("gender_preference") ?: "ANY"),
            scheduleNote = string("schedule_note").orEmpty(),
            amenities = get("amenities")?.jsonArray?.map { RoomAmenity.valueOf(it.jsonPrimitive.content) }.orEmpty(),
            addressLabel = string("address_label").orEmpty(),
            latitude = if (hasRealLocation) latitude else 0.0,
            longitude = if (hasRealLocation) longitude else 0.0,
            distanceToCampusM = if (hasRealLocation) CampusReference.distanceMeters(campus, latitude, longitude) else 0,
            status = HousingListingStatus.valueOf(string("status") ?: "OPEN"),
            verified = bool("verified"),
            verificationMethod = string("verification_method")?.let { HostVerificationMethod.valueOf(it) },
            areaM2 = (get("area_m2") as? JsonPrimitive)?.intOrNull,
        )
    }

    private companion object {
        const val IMAGE_BUCKET = "housing-images"

        /** Trần dung lượng mỗi ảnh đọc vào — chặn OOM trước khi giải mã. */
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

        /** Cạnh dài tối đa sau thu nhỏ (px) — đủ nét cho carousel, nhẹ cho feed. */
        const val MAX_IMAGE_DIMENSION_PX = 1280

        const val JPEG_QUALITY = 80

        /** Số ảnh upload cùng lúc — giới hạn bộ nhớ đỉnh khi giải mã nhiều ảnh. */
        const val MAX_PARALLEL_UPLOADS = 3
    }
}

package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
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
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Nguồn dữ liệu thật của Ghép trọ trên `housing_posts` (RLS giữ quyền sở hữu,
 * giới hạn 2 tin mở và ngưỡng báo cáo ở backend). Ảnh cục bộ (`content://`)
 * được upload lên bucket [IMAGE_BUCKET] trước khi insert; insert thất bại thì
 * ảnh vừa upload bị dọn để không mồ côi trong Storage.
 *
 * `distanceToCampusM` ưu tiên khoảng cách ĐƯỜNG ĐI thật qua OSRM (cùng dữ liệu
 * OpenStreetMap với picker, không key/billing) để khớp số km người dùng tra trên
 * Google Maps; OSRM lỗi/mất mạng thì giữ khoảng cách thẳng haversine qua
 * [CampusReference]. Tin chưa gắn vị trí (kể cả tin đời cũ lưu điểm chuẩn cũ)
 * trả khoảng cách 0 và tọa độ (0,0) để UI không bịa số km.
 */
@Singleton
class SupabaseHousingRepository @Inject constructor(
    private val client: SupabaseRestClient,
    @ApplicationContext private val context: Context,
) : HousingRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val roadDistanceCache = ConcurrentHashMap<String, Int>()

    override suspend fun getListings(): Result<List<HousingListing>> = suspendResult {
        val listings = client.table(
            "housing_posts?select=*&status=eq.OPEN&available_slots=gt.0&order=created_at.desc&limit=100",
        ).jsonArray.map { it.jsonObject.toHousingListing() }
        withRoadDistances(listings)
    }

    override suspend fun getListingById(id: String): Result<HousingListing> = suspendResult {
        val listing = client.table("housing_posts?select=*&id=eq.${urlEncode(id)}")
            .jsonArray.singleOrNull()?.jsonObject?.toHousingListing()
            ?: throw NoSuchElementException("Không tìm thấy tin: $id")
        withRoadDistances(listOf(listing)).single()
    }

    override suspend fun createListing(listing: HousingListing): Result<HousingListing> = suspendResult {
        val uploadedPaths = mutableListOf<String>()
        val images = try {
            resolveImages(listing.images, uploadedPaths)
        } catch (error: Exception) {
            // Lỗi trước khi ghi DB (ảnh quá cỡ, decode hỏng…) chắc chắn chưa commit → dọn ảnh đã upload.
            deleteObjects(uploadedPaths)
            throw error
        }
        try {
            val payload = listing.toPayload(images, includeAuthor = true)
            client.insertTable("housing_posts?select=*", payload).jsonArray.single().jsonObject.toHousingListing()
        } catch (rejected: SupabaseHttpException) {
            // Chỉ dọn ảnh khi server chắc chắn từ chối; timeout/lỗi mapping có thể
            // xảy ra sau khi DB đã commit — xóa ảnh sẽ làm tin tham chiếu ảnh đã mất.
            deleteObjects(uploadedPaths)
            throw rejected
        }
    }

    override suspend fun updateListing(listing: HousingListing): Result<HousingListing> = suspendResult {
        val authorFilter = "id=eq.${urlEncode(listing.id)}&author_id=eq.${urlEncode(client.userId)}"
        val previousImages = client.table("housing_posts?select=images&$authorFilter")
            .jsonArray.singleOrNull()?.jsonObject
            ?.get("images")?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        val uploadedPaths = mutableListOf<String>()
        val images = try {
            resolveImages(listing.images, uploadedPaths)
        } catch (error: Exception) {
            deleteObjects(uploadedPaths)
            throw error
        }
        try {
            // status/report_count không gửi kèm: trigger backend chỉ cho đổi qua RPC.
            val payload = listing.toPayload(images, includeAuthor = false)
            val updated = client.patchTable("housing_posts?$authorFilter&select=*", payload)
                .jsonArray.singleOrNull()?.jsonObject?.toHousingListing()
                ?: throw NoSuchElementException("Không tìm thấy tin để cập nhật: ${listing.id}")
            deleteObjects(previousImages.filter { it !in images }.mapNotNull { storagePathOf(it) })
            updated
        } catch (rejected: SupabaseHttpException) {
            deleteObjects(uploadedPaths)
            throw rejected
        } catch (missing: NoSuchElementException) {
            // Không dòng nào khớp (tin đã bị xóa) = chắc chắn chưa commit → dọn ảnh mới upload.
            deleteObjects(uploadedPaths)
            throw missing
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
     * Đường đi thật cho tin đã gắn vị trí; song song có trần để feed không
     * dội request lên OSRM, cache theo tọa độ làm tròn ~11m để lần nạp sau
     * không gọi lại. Tin chưa vị trí giữ 0.
     *
     * Trần thời gian cả feed: worker nằm TRONG scope timeout để hết ngân sách
     * là bị hủy thay vì giữ feed chờ; request đang chặn tự kết thúc theo
     * timeout 8s của kết nối. Hết ngân sách giữ khoảng cách fallback.
     */
    private suspend fun withRoadDistances(listings: List<HousingListing>): List<HousingListing> = withTimeoutOrNull(ROAD_BUDGET_MS) { parallelRoadDistances(listings) } ?: listings

    private suspend fun parallelRoadDistances(listings: List<HousingListing>): List<HousingListing> = coroutineScope {
        val gate = Semaphore(MAX_PARALLEL_ROUTES)
        listings.map { listing ->
            async { gate.withPermit { listing.copy(distanceToCampusM = roadDistanceMeters(listing)) } }
        }.awaitAll()
    }

    private suspend fun roadDistanceMeters(listing: HousingListing): Int {
        val fallback = listing.distanceToCampusM
        if (fallback == 0) return 0
        val key = listing.campus.name + ":" +
            String.format(Locale.US, "%.4f:%.4f", listing.latitude, listing.longitude)
        val meters = roadDistanceCache[key] ?: withContext(Dispatchers.IO) {
            try {
                fetchOsrmDistanceM(listing.latitude, listing.longitude, listing.campus)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(LOG_TAG, "OSRM lỗi, giữ khoảng cách đường thẳng", error)
                null
            }
        }?.also { roadDistanceCache[key] = it }
        return meters ?: fallback
    }

    private fun fetchOsrmDistanceM(latitude: Double, longitude: Double, campus: UthCampus): Int? {
        val (campusLat, campusLng) = CampusReference.of(campus)
        val connection = URL(
            "$OSRM_BASE_URL/route/v1/driving/$longitude,$latitude;$campusLng,$campusLat?overview=false",
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = OSRM_TIMEOUT_MS
            connection.readTimeout = OSRM_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", OSRM_USER_AGENT)
            if (connection.responseCode !in HTTP_OK_MIN..HTTP_OK_MAX) {
                Log.w(LOG_TAG, "OSRM HTTP ${connection.responseCode}")
                return null
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val route = json.parseToJsonElement(body).jsonObject["routes"]?.jsonArray?.firstOrNull()?.jsonObject
            val meters = (route?.get("distance") as? JsonPrimitive)?.doubleOrNull?.roundToInt()
            Log.i(LOG_TAG, "OSRM khoảng cách đường đi m=$meters")
            return meters
        } finally {
            connection.disconnect()
        }
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

    /** Đọc tối đa MAX_IMAGE_BYTES + 1 byte để stream khổng lồ không kịp ngốn heap trước khi bị chặn. */
    private fun readBounded(stream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        while (buffer.size() <= MAX_IMAGE_BYTES) {
            val read = stream.read(chunk)
            if (read < 0) break
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private suspend fun uploadLocalImage(uriString: String, uploadedPaths: MutableList<String>): String {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val original = resolver.openInputStream(uri)?.use { readBounded(it) }
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
            createdAt = string("created_at").orEmpty(),
        )
    }

    private companion object {
        const val IMAGE_BUCKET = "housing-images"

        const val LOG_TAG = "HousingRoad"

        /** OSRM demo công cộng, không key/billing — chỉ đường đi để số km khớp bản đồ người dùng tra. */
        const val OSRM_BASE_URL = "https://router.project-osrm.org"

        /** nginx của OSRM demo trả 403 cho User-Agent mặc định của Android (Dalvik) — phải tự khai báo. */
        const val OSRM_USER_AGENT = "UTH-Connect/1.0 (student housing app)"

        const val OSRM_TIMEOUT_MS = 8_000

        /** Ngân sách OSRM cho cả feed — quá hạn thì trả feed với khoảng cách fallback. */
        const val ROAD_BUDGET_MS = 10_000L

        const val READ_CHUNK_BYTES = 8192

        const val MAX_PARALLEL_ROUTES = 4

        const val HTTP_OK_MIN = 200
        const val HTTP_OK_MAX = 299

        /** Trần dung lượng mỗi ảnh đọc vào — chặn OOM trước khi giải mã. */
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

        /** Cạnh dài tối đa sau thu nhỏ (px) — đủ nét cho carousel, nhẹ cho feed. */
        const val MAX_IMAGE_DIMENSION_PX = 1280

        const val JPEG_QUALITY = 80

        /** Số ảnh upload cùng lúc — giới hạn bộ nhớ đỉnh khi giải mã nhiều ảnh. */
        const val MAX_PARALLEL_UPLOADS = 3
    }
}

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
import com.giathinh.uthconnect.domain.repository.HousingImageCleanupStore
import com.giathinh.uthconnect.domain.repository.HousingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * Nguồn dữ liệu thật của Ghép trọ trên `housing_posts` (RLS giữ quyền sở hữu,
 * giới hạn 2 tin mở và ngưỡng báo cáo ở backend). Ảnh cục bộ (`content://`)
 * được upload lên bucket [IMAGE_BUCKET] trước khi insert; server từ chối rõ
 * ràng (4xx) thì ảnh vừa upload bị dọn, lỗi chưa xác định (5xx/timeout) thì
 * đối chiếu DB trước để không xóa ảnh của tin đã commit. Xóa/sửa tin ghi path
 * ảnh cần dọn vào hàng đợi [HousingImageCleanupStore] rồi trả kết quả ngay;
 * worker nền dọn ảnh (không chặn thao tác lẫn feed, chỉ dọn ảnh đúng chủ).
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
    private val cleanupStore: HousingImageCleanupStore,
    @ApplicationContext private val context: Context,
) : HousingRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val roadDistanceCache = ConcurrentHashMap<String, Int>()

    /** Dọn ảnh mồ côi là việc nền của cả vòng đời app, không theo scope của một màn hình. */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cleanupRunning = AtomicBoolean(false)

    override suspend fun getListings(): Result<List<HousingListing>> = suspendResult {
        launchPendingImageCleanup()
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
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { deleteObjects(uploadedPaths) }
            throw cancelled
        } catch (error: Exception) {
            // Lỗi trước khi ghi DB (ảnh quá cỡ, decode hỏng…) chắc chắn chưa commit → dọn ảnh đã upload.
            withContext(NonCancellable) { deleteObjects(uploadedPaths) }
            throw error
        }
        try {
            val payload = listing.toPayload(images, includeAuthor = true)
            client.insertTable("housing_posts?select=*", payload).jsonArray.single().jsonObject.toHousingListing()
        } catch (rejected: SupabaseHttpException) {
            cleanupUploads(uploadedPaths, rejected.status, listingId = null)
            throw rejected
        } catch (error: Exception) {
            // Timeout/lỗi mạng không biết DB đã commit chưa — đối chiếu trước khi dọn.
            cleanupUploads(uploadedPaths, status = null, listingId = null)
            throw error
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
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) { deleteObjects(uploadedPaths) }
            throw cancelled
        } catch (error: Exception) {
            withContext(NonCancellable) { deleteObjects(uploadedPaths) }
            throw error
        }
        try {
            // status/report_count không gửi kèm: trigger backend chỉ cho đổi qua RPC.
            val payload = listing.toPayload(images, includeAuthor = false)
            val updated = client.patchTable("housing_posts?$authorFilter&select=*", payload)
                .jsonArray.singleOrNull()?.jsonObject?.toHousingListing()
                ?: throw NoSuchElementException("Không tìm thấy tin để cập nhật: ${listing.id}")
            val removedImages = previousImages.filter { it !in images }.mapNotNull { storagePathOf(it) }
            // Ảnh gỡ bỏ vào hàng đợi rồi trả kết quả sửa tin ngay; worker nền dọn,
            // hàng đợi lỗi đĩa thì dọn đồng bộ để không mồ côi.
            withContext(NonCancellable) {
                if (!enqueueImageCleanup(removedImages)) deleteObjects(removedImages)
            }
            launchPendingImageCleanup()
            updated
        } catch (rejected: SupabaseHttpException) {
            cleanupUploads(uploadedPaths, rejected.status, listing.id)
            throw rejected
        } catch (error: Exception) {
            // Kể cả NoSuchElementException (tin đã bị xóa): đối chiếu lại cho chắc,
            // vì lỗi mapping sau khi patch commit cũng rơi vào nhánh này.
            cleanupUploads(uploadedPaths, status = null, listingId = listing.id)
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
        // Bản ghi đã xóa: ghi path vào hàng đợi bền vững rồi trả kết quả ngay, worker nền
        // dọn ảnh — người dùng không phải chờ từng lệnh xóa Storage. Hàng đợi lỗi đĩa
        // thì dọn đồng bộ ngay, thà chậm còn hơn để ảnh mồ côi.
        withContext(NonCancellable) {
            val paths = images.mapNotNull { storagePathOf(it) }
            if (!enqueueImageCleanup(paths)) deleteObjects(paths)
        }
        launchPendingImageCleanup()
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
     * là bị hủy; hủy coroutine đóng socket ngay nên request đang chặn không
     * giữ feed chờ hết timeout kết nối. Hết ngân sách giữ khoảng cách fallback.
     */
    private suspend fun withRoadDistances(listings: List<HousingListing>): List<HousingListing> {
        val routed = withTimeoutOrNull(ROAD_BUDGET_MS) { parallelRoadDistances(listings) }
        return routed ?: listings
    }

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
        val meters = roadDistanceCache[key] ?: fetchRoadDistanceOrNull(listing)?.also { roadDistanceCache[key] = it }
        return meters ?: fallback
    }

    private suspend fun fetchRoadDistanceOrNull(listing: HousingListing): Int? = try {
        fetchOsrmDistanceM(listing.latitude, listing.longitude, listing.campus)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w(LOG_TAG, "OSRM lỗi, giữ khoảng cách đường thẳng", error)
        null
    }

    private suspend fun fetchOsrmDistanceM(latitude: Double, longitude: Double, campus: UthCampus): Int? {
        return withContext(Dispatchers.IO) {
            val (campusLat, campusLng) = CampusReference.of(campus)
            val connection = URL(
                "$OSRM_BASE_URL/route/v1/driving/$longitude,$latitude;$campusLng,$campusLat?overview=false",
            ).openConnection() as HttpURLConnection
            // Hủy coroutine là đóng socket ngay để lần read/connect đang chặn thoát tức thì.
            val onFinished = coroutineContext[Job]?.invokeOnCompletion { connection.disconnect() }
            try {
                connection.connectTimeout = OSRM_TIMEOUT_MS
                connection.readTimeout = OSRM_TIMEOUT_MS
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", OSRM_USER_AGENT)
                if (connection.responseCode !in HTTP_OK_MIN..HTTP_OK_MAX) {
                    Log.w(LOG_TAG, "OSRM HTTP ${connection.responseCode}")
                    return@withContext null
                }
                val body = connection.inputStream.bufferedReader().use {
                    readBodyWithinBudget(it, OSRM_TIMEOUT_MS.toLong()) { System.nanoTime() / NANOS_PER_MS }
                }
                val route = json.parseToJsonElement(body).jsonObject["routes"]?.jsonArray?.firstOrNull()?.jsonObject
                val meters = (route?.get("distance") as? JsonPrimitive)?.doubleOrNull?.roundToInt()
                Log.i(LOG_TAG, "OSRM khoảng cách đường đi m=$meters")
                meters
            } finally {
                onFinished?.dispose()
                connection.disconnect()
            }
        }
    }

    /**
     * Giữ URL có sẵn thuộc đúng bucket của module (kể cả dữ liệu cũ); URL ngoài
     * bucket bị từ chối để tin không tham chiếu ảnh lạ. URI cục bộ thì đọc bytes,
     * upload và trả public URL. Chạy song song tối đa [MAX_PARALLEL_UPLOADS] ảnh
     * trên IO: đọc file + giải mã bitmap là việc chặn luồng (để trên Main sẽ đơ UI),
     * còn upload tuần tự bắt người đăng tin chờ cộng dồn thời gian của từng ảnh.
     */
    private suspend fun resolveImages(
        images: List<String>,
        uploadedPaths: MutableList<String>,
    ): List<String> = coroutineScope {
        val gate = Semaphore(MAX_PARALLEL_UPLOADS)
        val uploads = images.map { image ->
            async(Dispatchers.IO) {
                if (image.startsWith("http://") || image.startsWith("https://")) {
                    require(storagePathOf(image) != null) { "Ảnh ngoài bucket $IMAGE_BUCKET: $image" }
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
        // Ghi path trước khi upload: Storage đã lưu mà coroutine bị hủy giữa chừng
        // thì danh sách dọn vẫn còn path; xóa object không tồn tại chỉ là no-op.
        synchronized(uploadedPaths) { uploadedPaths += path }
        client.uploadStorageObject(IMAGE_BUCKET, path, bytes, "image/jpeg")
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

    /** Xóa ảnh trong bucket; trả về path thất bại để hàng đợi dọn lại thử lần sau. */
    private suspend fun deleteObjects(paths: List<String>): List<String> {
        val failed = mutableListOf<String>()
        for (path in paths) {
            try {
                client.deleteStorageObject(IMAGE_BUCKET, path)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(LOG_TAG, "Xóa ảnh Storage thất bại: $path", error)
                failed += path
            }
        }
        return failed
    }

    /**
     * Ghi hàng đợi bền vững là việc phụ: lỗi đĩa không được làm hỏng thao tác chính.
     * Trả false khi không ghi được để caller dọn đồng bộ ngay, không mất path.
     */
    private suspend fun enqueueImageCleanup(paths: List<String>): Boolean {
        if (paths.isEmpty()) return true
        return try {
            cleanupStore.enqueue(paths)
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Log.w(LOG_TAG, "Không lưu được hàng đợi dọn ảnh", error)
            false
        }
    }

    /**
     * Dọn ảnh mồ côi chạy nền để feed không phải chờ từng lệnh xóa Storage;
     * mỗi lượt chỉ thử một nắm path, phần còn lại giữ trong hàng đợi cho lượt sau.
     * Hai lần nạp feed liên tiếp không chạy hai lượt dọn chồng nhau.
     */
    private fun launchPendingImageCleanup() {
        if (!cleanupRunning.compareAndSet(false, true)) return
        cleanupScope.launch {
            try {
                retryPendingImageCleanup()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(LOG_TAG, "Dọn ảnh mồ côi nền thất bại", error)
            } finally {
                cleanupRunning.set(false)
            }
        }
    }

    /** Thử lại ảnh mồ côi của lần xóa trước; chỉ bỏ khỏi hàng đợi những path đã xóa được. */
    private suspend fun retryPendingImageCleanup() {
        val pending = try {
            cleanupStore.pending()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Log.w(LOG_TAG, "Không đọc được hàng đợi dọn ảnh", error)
            return
        }
        // Path luôn mở đầu bằng uid chủ ảnh — chỉ dọn ảnh của tài khoản hiện tại;
        // ảnh tài khoản khác để nguyên cho chính chủ dọn, không phí request bị RLS chặn.
        val ownPaths = pending.filter { it.startsWith("${client.userId}/") }.take(MAX_CLEANUP_PATHS_PER_PASS)
        if (ownPaths.isEmpty()) return
        val failed = deleteObjects(ownPaths).toSet()
        try {
            cleanupStore.dequeue(ownPaths.filter { it !in failed })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            Log.w(LOG_TAG, "Không cập nhật được hàng đợi dọn ảnh", error)
        }
    }

    /**
     * 4xx = server chắc chắn từ chối, chưa commit → dọn ảnh mới upload.
     * 5xx/timeout chưa biết commit hay chưa → đối chiếu tin của chủ sở hữu, chỉ
     * xóa ảnh không còn được tham chiếu; không đối chiếu được thì giữ ảnh (thà
     * mồ côi trong Storage còn hơn làm tin thật mất ảnh). NonCancellable để lệnh
     * hủy giữa chừng không bỏ dở việc dọn.
     */
    private suspend fun cleanupUploads(uploadedPaths: List<String>, status: Int?, listingId: String?) {
        if (uploadedPaths.isEmpty()) return
        withContext(NonCancellable) {
            val confirmedRejection = status != null && status in HTTP_CLIENT_ERROR_MIN..HTTP_CLIENT_ERROR_MAX
            val failed = if (confirmedRejection) {
                deleteObjects(uploadedPaths)
            } else {
                deleteObjects(unreferencedUploads(uploadedPaths, listingId))
            }
            enqueueImageCleanup(failed)
        }
    }

    private suspend fun unreferencedUploads(uploadedPaths: List<String>, listingId: String?): List<String> {
        val idFilter = listingId?.let { "&id=eq.${urlEncode(it)}" }.orEmpty()
        val referenced = try {
            client.table("housing_posts?select=images&author_id=eq.${urlEncode(client.userId)}$idFilter")
                .jsonArray
                .flatMap { it.jsonObject["images"]?.jsonArray.orEmpty() }
                .mapNotNull { storagePathOf(it.jsonPrimitive.content) }
                .toSet()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(LOG_TAG, "Không đối chiếu được tin sau lỗi chưa xác định — giữ ảnh", error)
            return emptyList()
        }
        return uploadedPaths.filter { it !in referenced }
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
        put("contact_zalo_enabled", contactZaloEnabled)
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
            // Tin đời cũ chưa có cột này (migration chưa chạy) giữ fallback SĐT như cũ.
            contactZaloEnabled = (get("contact_zalo_enabled") as? JsonPrimitive)?.booleanOrNull ?: true,
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

        /** 4xx = server đã từ chối rõ ràng (chưa commit); ngoài dải này là chưa xác định. */
        const val HTTP_CLIENT_ERROR_MIN = 400
        const val HTTP_CLIENT_ERROR_MAX = 499

        /** Trần dung lượng mỗi ảnh đọc vào — chặn OOM trước khi giải mã. */
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024

        /** Cạnh dài tối đa sau thu nhỏ (px) — đủ nét cho carousel, nhẹ cho feed. */
        const val MAX_IMAGE_DIMENSION_PX = 1280

        const val JPEG_QUALITY = 80

        /** Số ảnh upload cùng lúc — giới hạn bộ nhớ đỉnh khi giải mã nhiều ảnh. */
        const val MAX_PARALLEL_UPLOADS = 3

        /** Số path dọn mỗi lượt nền — phần còn lại chờ lượt sau, không dội request lên Storage. */
        const val MAX_CLEANUP_PATHS_PER_PASS = 20
    }
}

private const val NANOS_PER_MS = 1_000_000L
private const val OSRM_BODY_CHUNK_CHARS = 4096

/**
 * Đọc trọn response nhưng giới hạn TỔNG thời gian đọc chứ không chỉ từng lần
 * read (server nhỏ giọt vẫn bị chặn); quá ngân sách ném IOException để caller
 * giữ khoảng cách fallback. Clock truyền vào được để test giả lập thời gian.
 */
internal fun readBodyWithinBudget(reader: Reader, budgetMs: Long, elapsedMs: () -> Long): String {
    val deadline = elapsedMs() + budgetMs
    val body = StringBuilder()
    val chunk = CharArray(OSRM_BODY_CHUNK_CHARS)
    while (true) {
        if (elapsedMs() >= deadline) throw IOException("OSRM response vượt tổng thời gian đọc")
        val read = reader.read(chunk)
        if (read == -1) return body.toString()
        body.append(chunk, 0, read)
    }
}

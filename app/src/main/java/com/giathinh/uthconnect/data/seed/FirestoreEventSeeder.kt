package com.giathinh.uthconnect.data.seed

import com.giathinh.uthconnect.data.repository.suspendResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Tiện ích hỗ trợ nạp dữ liệu mẫu ban đầu vào Firestore (phục vụ test và demo).
 */
object FirestoreEventSeeder {
    private const val SPORTS_DAY_OFFSET = 3
    private const val SPORTS_START_HOUR = 7
    private const val SPORTS_END_HOUR = 17
    private const val SPORTS_CAPACITY = 200
    private const val SPORTS_REGISTERED = 42
    private const val CLUB_DAY_OFFSET = 5
    private const val CLUB_START_HOUR = 18
    private const val CLUB_START_MINUTE = 30
    private const val CLUB_END_HOUR = 21
    private const val CLUB_CAPACITY = 80
    private const val CLUB_REGISTERED = 72
    private const val WORKSHOP_DAY_OFFSET = 7
    private const val WORKSHOP_START_HOUR = 9
    private const val WORKSHOP_END_HOUR = 11
    private const val WORKSHOP_END_MINUTE = 30
    private const val WORKSHOP_CAPACITY = 60
    private const val WORKSHOP_REGISTERED = 58

    suspend fun seedSampleEvents(firestore: FirebaseFirestore): Result<Int> = suspendResult {
        val calendar = Calendar.getInstance()

        val sampleEvents = listOf(
            mapOf(
                "id" to "event_hoi_thao_2026",
                "title" to "Hội thao Sinh viên UTH 2026",
                "description" to "Hội thao truyền thống sinh viên toàn trường. " +
                    "Thi đấu bóng đá, cầu lông, kéo co và cờ vua.",
                "orgId" to "clb_the_thao",
                "orgName" to "CLB Thể Thao UTH",
                "location" to "Sân vận động Cơ sở 2",
                "campus" to "Quang Trung",
                "startAt" to calendar.apply {
                    add(Calendar.DAY_OF_YEAR, SPORTS_DAY_OFFSET)
                    set(Calendar.HOUR_OF_DAY, SPORTS_START_HOUR)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis,
                "endAt" to calendar.apply {
                    set(Calendar.HOUR_OF_DAY, SPORTS_END_HOUR)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis,
                "capacity" to SPORTS_CAPACITY,
                "registeredCount" to SPORTS_REGISTERED,
                "status" to "PUBLISHED",
                "coverUrl" to "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800",
            ),
            mapOf(
                "id" to "event_clb_anh_sang",
                "title" to "Giao lưu & Tuyển thành viên CLB Ánh Sáng",
                "description" to "Buổi giao lưu chia sẻ về các hoạt động thiện nguyện, " +
                    "chiến dịch Mùa hè xanh và rèn luyện kỹ năng mềm.",
                "orgId" to "clb_anh_sang",
                "orgName" to "Đội CTXH Ánh Sáng",
                "location" to "Hội trường A.102",
                "campus" to "Nguyễn Tất Thành",
                "startAt" to calendar.apply {
                    add(Calendar.DAY_OF_YEAR, CLUB_DAY_OFFSET)
                    set(Calendar.HOUR_OF_DAY, CLUB_START_HOUR)
                    set(Calendar.MINUTE, CLUB_START_MINUTE)
                }.timeInMillis,
                "endAt" to calendar.apply {
                    set(Calendar.HOUR_OF_DAY, CLUB_END_HOUR)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis,
                "capacity" to CLUB_CAPACITY,
                "registeredCount" to CLUB_REGISTERED,
                "status" to "PUBLISHED",
                "coverUrl" to "https://images.unsplash.com/photo-1511578314322-379afb476865?w=800",
            ),
            mapOf(
                "id" to "event_workshop_ai_2026",
                "title" to "Workshop: Ứng dụng AI & Cloud cho Sinh viên CNTT",
                "description" to "Học cách xây dựng ứng dụng với Gemini API " +
                    "và Firebase Cloud Functions cùng diễn giả GDG.",
                "orgId" to "clb_tin_hoc",
                "orgName" to "CLB Tin học UTH",
                "location" to "Phòng Lab 3, Khu F",
                "campus" to "Thủ Đức",
                "startAt" to calendar.apply {
                    add(Calendar.DAY_OF_YEAR, WORKSHOP_DAY_OFFSET)
                    set(Calendar.HOUR_OF_DAY, WORKSHOP_START_HOUR)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis,
                "endAt" to calendar.apply {
                    set(Calendar.HOUR_OF_DAY, WORKSHOP_END_HOUR)
                    set(Calendar.MINUTE, WORKSHOP_END_MINUTE)
                }.timeInMillis,
                "capacity" to WORKSHOP_CAPACITY,
                "registeredCount" to WORKSHOP_REGISTERED,
                "status" to "PUBLISHED",
                "coverUrl" to "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800",
            ),
        )

        for (event in sampleEvents) {
            val id = event["id"] as String
            firestore.collection("events").document(id).set(event).await()
        }

        sampleEvents.size
    }
}

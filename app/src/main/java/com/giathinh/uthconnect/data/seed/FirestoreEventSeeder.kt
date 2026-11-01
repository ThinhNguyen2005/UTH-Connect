package com.giathinh.uthconnect.data.seed

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Tiện ích hỗ trợ nạp dữ liệu mẫu ban đầu vào Firestore (phục vụ test và demo).
 */
object FirestoreEventSeeder {

    suspend fun seedSampleEvents(firestore: FirebaseFirestore): Result<Int> = runCatching {
        val calendar = Calendar.getInstance()

        val sampleEvents = listOf(
            mapOf(
                "id" to "event_hoi_thao_2026",
                "title" to "Hội thao Sinh viên UTH 2026",
                "description" to "Hội thao truyền thống sinh viên toàn trường. Thi đấu bóng đá, cầu lông, kéo co và cờ vua.",
                "orgId" to "clb_the_thao",
                "orgName" to "CLB Thể Thao UTH",
                "location" to "Sân vận động Cơ sở 2",
                "campus" to "Quang Trung",
                "startAt" to calendar.apply { add(Calendar.DAY_OF_YEAR, 3); set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 0) }.timeInMillis,
                "endAt" to calendar.apply { set(Calendar.HOUR_OF_DAY, 17); set(Calendar.MINUTE, 0) }.timeInMillis,
                "capacity" to 200,
                "registeredCount" to 42,
                "status" to "PUBLISHED",
                "coverUrl" to "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800",
            ),
            mapOf(
                "id" to "event_clb_anh_sang",
                "title" to "Giao lưu & Tuyển thành viên CLB Ánh Sáng",
                "description" to "Buổi giao lưu chia sẻ về các hoạt động thiện nguyện, chiến dịch Mùa hè xanh và rèn luyện kỹ năng mềm.",
                "orgId" to "clb_anh_sang",
                "orgName" to "Đội CTXH Ánh Sáng",
                "location" to "Hội trường A.102",
                "campus" to "Nguyễn Tất Thành",
                "startAt" to calendar.apply { add(Calendar.DAY_OF_YEAR, 5); set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 30) }.timeInMillis,
                "endAt" to calendar.apply { set(Calendar.HOUR_OF_DAY, 21); set(Calendar.MINUTE, 0) }.timeInMillis,
                "capacity" to 80,
                "registeredCount" to 72,
                "status" to "PUBLISHED",
                "coverUrl" to "https://images.unsplash.com/photo-1511578314322-379afb476865?w=800",
            ),
            mapOf(
                "id" to "event_workshop_ai_2026",
                "title" to "Workshop: Ứng dụng AI & Cloud cho Sinh viên CNTT",
                "description" to "Học cách xây dựng ứng dụng với Gemini API và Firebase Cloud Functions cùng diễn giả GDG.",
                "orgId" to "clb_tin_hoc",
                "orgName" to "CLB Tin học UTH",
                "location" to "Phòng Lab 3, Khu F",
                "campus" to "Thủ Đức",
                "startAt" to calendar.apply { add(Calendar.DAY_OF_YEAR, 7); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }.timeInMillis,
                "endAt" to calendar.apply { set(Calendar.HOUR_OF_DAY, 11); set(Calendar.MINUTE, 30) }.timeInMillis,
                "capacity" to 60,
                "registeredCount" to 58,
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

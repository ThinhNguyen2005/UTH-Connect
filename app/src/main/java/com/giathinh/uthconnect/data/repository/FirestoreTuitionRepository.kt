package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TuitionCourse
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirestoreTuitionRepository @Inject constructor() : TuitionRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun getTuition(): Result<TuitionInfo> = runCatching {
        val studentId = "2023040123"
        val docRef = firestore.collection("students")
            .document(studentId)
            .collection("tuition")
            .document("hk1")

        val snapshot = docRef.get().awaitTask()

        if (!snapshot.exists()) {
            val initialCourses = listOf(
                mapOf("code" to "IT001", "name" to "Lập trình thiết bị di động", "credits" to 3, "fee" to "2.100.000 VNĐ"),
                mapOf("code" to "IT002", "name" to "Kiến trúc & Thiết kế phần mềm", "credits" to 3, "fee" to "2.100.000 VNĐ"),
                mapOf("code" to "IT003", "name" to "Kiểm thử & Đảm bảo chất lượng", "credits" to 3, "fee" to "2.100.000 VNĐ"),
                mapOf("code" to "IT004", "name" to "Công nghệ phần mềm chuyên sâu", "credits" to 3, "fee" to "2.100.000 VNĐ"),
                mapOf("code" to "IT005", "name" to "Đồ án chuyên ngành KTPM", "credits" to 3, "fee" to "2.100.000 VNĐ"),
                mapOf("code" to "GE006", "name" to "Kỹ năng nghề nghiệp & Giao tiếp", "credits" to 3, "fee" to "2.150.000 VNĐ"),
            )
            val seedData = hashMapOf(
                "semester" to "Học kỳ 1 (Năm học 2025 - 2026)",
                "isPaid" to true,
                "totalAmount" to "12.650.000 VNĐ",
                "paidAmount" to "12.650.000 VNĐ",
                "remainingAmount" to "0 VNĐ",
                "creditsCount" to 18,
                "dueDate" to "15/11/2025",
                "courses" to initialCourses,
            )
            docRef.set(seedData).awaitTask()
            return@runCatching mapSnapshot(docRef.get().awaitTask())
        }

        mapSnapshot(snapshot)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapSnapshot(snapshot: DocumentSnapshot): TuitionInfo {
        val coursesRaw = snapshot.get("courses") as? List<Map<String, Any>> ?: emptyList()
        val courses = coursesRaw.map {
            TuitionCourse(
                code = it["code"] as? String ?: "",
                name = it["name"] as? String ?: "",
                credits = (it["credits"] as? Number)?.toInt() ?: 0,
                fee = it["fee"] as? String ?: "",
            )
        }

        return TuitionInfo(
            semester = snapshot.getString("semester") ?: "Học kỳ 1 (Năm học 2025 - 2026)",
            isPaid = snapshot.getBoolean("isPaid") ?: true,
            totalAmount = snapshot.getString("totalAmount") ?: "12.650.000 VNĐ",
            paidAmount = snapshot.getString("paidAmount") ?: "12.650.000 VNĐ",
            remainingAmount = snapshot.getString("remainingAmount") ?: "0 VNĐ",
            creditsCount = snapshot.getLong("creditsCount")?.toInt() ?: 18,
            dueDate = snapshot.getString("dueDate") ?: "15/11/2025",
            courses = courses,
        )
    }
}

internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TuitionCourse
import com.giathinh.uthconnect.domain.model.TuitionInfo
import com.giathinh.uthconnect.domain.repository.TuitionRepository
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
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
    private val auth by lazy { FirebaseAuth.getInstance() }

    private suspend fun ensureAuthUid(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        val result = auth.signInWithEmailAndPassword("2023040123@ut.edu.vn", "123456").awaitTask()
        return result.user?.uid ?: "9umVXbv2kchxkNguQ9kUUMmutBv2"
    }

    override suspend fun getTuition(): Result<TuitionInfo> = runCatching {
        val uid = ensureAuthUid()
        val userDoc = firestore.collection("users").document(uid).get().awaitTask()

        if (userDoc.exists()) {
            mapSnapshot(userDoc)
        } else {
            // Fallback sang doc students nếu có
            val studentDoc = firestore.collection("students")
                .document("2023040123")
                .collection("tuition")
                .document("hk1")
                .get()
                .awaitTask()
            mapSnapshot(studentDoc)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapSnapshot(snapshot: DocumentSnapshot): TuitionInfo {
        val coursesRaw = (snapshot.get("tuitionCourses") as? List<Map<String, Any>>)
            ?: (snapshot.get("courses") as? List<Map<String, Any>>)
            ?: emptyList()

        val courses = coursesRaw.map {
            TuitionCourse(
                code = it["code"] as? String ?: "",
                name = it["name"] as? String ?: "",
                credits = (it["credits"] as? Number)?.toInt() ?: 0,
                fee = it["fee"] as? String ?: "",
            )
        }

        return TuitionInfo(
            semester = snapshot.getString("tuitionSemester")
                ?: snapshot.getString("semester")
                ?: "Học kỳ 1 (Năm học 2025 - 2026)",
            isPaid = snapshot.getBoolean("tuitionIsPaid")
                ?: snapshot.getBoolean("isPaid")
                ?: true,
            totalAmount = snapshot.getString("tuitionTotalAmount")
                ?: snapshot.getString("totalAmount")
                ?: "12.650.000 VNĐ",
            paidAmount = snapshot.getString("tuitionPaidAmount")
                ?: snapshot.getString("paidAmount")
                ?: "12.650.000 VNĐ",
            remainingAmount = snapshot.getString("tuitionRemainingAmount")
                ?: snapshot.getString("remainingAmount")
                ?: "0 VNĐ",
            creditsCount = snapshot.getLong("tuitionCreditsCount")?.toInt()
                ?: snapshot.getLong("creditsCount")?.toInt()
                ?: 18,
            dueDate = snapshot.getString("tuitionDueDate")
                ?: snapshot.getString("dueDate")
                ?: "15/11/2025",
            courses = courses,
        )
    }
}

internal suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

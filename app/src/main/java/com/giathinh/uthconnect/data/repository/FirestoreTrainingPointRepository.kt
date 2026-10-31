package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TrainingCriteria
import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTrainingPointRepository @Inject constructor() : TrainingPointRepository {

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

    override suspend fun getTrainingPoint(): Result<TrainingPointInfo> = runCatching {
        val uid = ensureAuthUid()
        val userDoc = firestore.collection("users").document(uid).get().awaitTask()

        if (userDoc.exists()) {
            mapSnapshot(userDoc)
        } else {
            val studentDoc = firestore.collection("students")
                .document("2023040123")
                .collection("training_points")
                .document("hk1")
                .get()
                .awaitTask()
            mapSnapshot(studentDoc)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapSnapshot(snapshot: DocumentSnapshot): TrainingPointInfo {
        val criteriaRaw = (snapshot.get("trainingCriteria") as? List<Map<String, Any>>)
            ?: (snapshot.get("criteria") as? List<Map<String, Any>>)
            ?: emptyList()

        val criteriaList = criteriaRaw.map {
            TrainingCriteria(
                title = it["title"] as? String ?: "",
                score = (it["score"] as? Number)?.toInt() ?: 0,
                maxScore = (it["maxScore"] as? Number)?.toInt() ?: 0,
                description = it["description"] as? String ?: "",
            )
        }

        return TrainingPointInfo(
            semester = snapshot.getString("trainingSemester")
                ?: snapshot.getString("semester")
                ?: "Học kỳ 1 (Năm học 2025 - 2026)",
            totalScore = snapshot.getLong("trainingTotalScore")?.toInt()
                ?: snapshot.getLong("totalScore")?.toInt()
                ?: 92,
            maxScore = snapshot.getLong("trainingMaxScore")?.toInt()
                ?: snapshot.getLong("maxScore")?.toInt()
                ?: 100,
            rank = snapshot.getString("trainingRank")
                ?: snapshot.getString("rank")
                ?: "XUẤT SẮC",
            criteriaList = criteriaList,
        )
    }
}

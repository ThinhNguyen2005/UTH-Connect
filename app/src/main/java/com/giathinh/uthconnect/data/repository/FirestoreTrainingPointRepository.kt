package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.TrainingCriteria
import com.giathinh.uthconnect.domain.model.TrainingPointInfo
import com.giathinh.uthconnect.domain.repository.TrainingPointRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTrainingPointRepository @Inject constructor() : TrainingPointRepository {

    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override suspend fun getTrainingPoint(): Result<TrainingPointInfo> = runCatching {
        val studentId = "2023040123"
        val docRef = firestore.collection("students")
            .document(studentId)
            .collection("training_points")
            .document("hk1")

        val snapshot = docRef.get().awaitTask()

        if (!snapshot.exists()) {
            val initialCriteria = listOf(
                mapOf("title" to "1. Ý thức tham gia học tập", "score" to 18, "maxScore" to 20, "description" to "Đi học đầy đủ, đúng giờ, không vi phạm quy chế kiểm tra thi cử."),
                mapOf("title" to "2. Ý thức chấp hành nội quy, quy chế", "score" to 25, "maxScore" to 25, "description" to "Chấp hành tốt quy định của Nhà nước, nhà trường và nội quy sinh viên."),
                mapOf("title" to "3. Hoạt động phong trào Đoàn - Hội", "score" to 18, "maxScore" to 20, "description" to "Tham gia các buổi hội thảo, hoạt động tình nguyện, CLB học thuật."),
                mapOf("title" to "4. Ý thức công dân & Quan hệ cộng đồng", "score" to 22, "maxScore" to 25, "description" to "Tích cực tham gia hoạt động văn hóa, giúp đỡ cộng đồng, lối sống lành mạnh."),
                mapOf("title" to "5. Khen thưởng và thành tích đặc biệt", "score" to 5, "maxScore" to 10, "description" to "Đạt giải trong các cuộc thi học thuật hoặc phong trào nghiên cứu khoa học."),
            )
            val seedData = hashMapOf(
                "semester" to "Học kỳ 1 (Năm học 2025 - 2026)",
                "totalScore" to 88,
                "maxScore" to 100,
                "rank" to "TỐT",
                "criteria" to initialCriteria,
            )
            docRef.set(seedData).awaitTask()
            return@runCatching mapSnapshot(docRef.get().awaitTask())
        }

        mapSnapshot(snapshot)
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapSnapshot(snapshot: DocumentSnapshot): TrainingPointInfo {
        val criteriaRaw = snapshot.get("criteria") as? List<Map<String, Any>> ?: emptyList()
        val criteriaList = criteriaRaw.map {
            TrainingCriteria(
                title = it["title"] as? String ?: "",
                score = (it["score"] as? Number)?.toInt() ?: 0,
                maxScore = (it["maxScore"] as? Number)?.toInt() ?: 0,
                description = it["description"] as? String ?: "",
            )
        }

        return TrainingPointInfo(
            semester = snapshot.getString("semester") ?: "Học kỳ 1 (Năm học 2025 - 2026)",
            totalScore = snapshot.getLong("totalScore")?.toInt() ?: 88,
            maxScore = snapshot.getLong("maxScore")?.toInt() ?: 100,
            rank = snapshot.getString("rank") ?: "TỐT",
            criteriaList = criteriaList,
        )
    }
}

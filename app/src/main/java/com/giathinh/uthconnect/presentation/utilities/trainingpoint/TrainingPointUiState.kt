package com.giathinh.uthconnect.presentation.utilities.trainingpoint

/**
 * Một tiêu chí đánh giá điểm rèn luyện.
 */
data class TrainingCriteria(
    val title: String,
    val score: Int,
    val maxScore: Int,
    val description: String,
)

/**
 * Trạng thái giao diện màn hình Tra cứu điểm rèn luyện.
 */
data class TrainingPointUiState(
    val semester: String = "Học kỳ 1 (Năm học 2025 - 2026)",
    val totalScore: Int = 88,
    val maxScore: Int = 100,
    val rank: String = "TỐT",
    val criteriaList: List<TrainingCriteria> = listOf(
        TrainingCriteria(
            title = "1. Ý thức tham gia học tập",
            score = 18,
            maxScore = 20,
            description = "Đi học đầy đủ, đúng giờ, không vi phạm quy chế kiểm tra thi cử.",
        ),
        TrainingCriteria(
            title = "2. Ý thức chấp hành nội quy, quy chế",
            score = 25,
            maxScore = 25,
            description = "Chấp hành tốt quy định của Nhà nước, nhà trường và nội quy sinh viên.",
        ),
        TrainingCriteria(
            title = "3. Hoạt động phong trào Đoàn - Hội",
            score = 18,
            maxScore = 20,
            description = "Tham gia các buổi hội thảo, hoạt động tình nguyện, CLB học thuật.",
        ),
        TrainingCriteria(
            title = "4. Ý thức công dân & Quan hệ cộng đồng",
            score = 22,
            maxScore = 25,
            description = "Tích cực tham gia hoạt động văn hóa, giúp đỡ cộng đồng, lối sống lành mạnh.",
        ),
        TrainingCriteria(
            title = "5. Khen thưởng và thành tích đặc biệt",
            score = 5,
            maxScore = 10,
            description = "Đạt giải trong các cuộc thi học thuật hoặc phong trào nghiên cứu khoa học.",
        ),
    ),
)

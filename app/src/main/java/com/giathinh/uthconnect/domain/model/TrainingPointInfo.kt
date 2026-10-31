package com.giathinh.uthconnect.domain.model

data class TrainingCriteria(
    val title: String,
    val score: Int,
    val maxScore: Int,
    val description: String,
)

data class TrainingPointInfo(
    val semester: String,
    val totalScore: Int,
    val maxScore: Int,
    val rank: String,
    val criteriaList: List<TrainingCriteria>,
)

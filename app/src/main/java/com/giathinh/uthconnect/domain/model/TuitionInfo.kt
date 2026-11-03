package com.giathinh.uthconnect.domain.model

data class TuitionCourse(
    val code: String,
    val name: String,
    val credits: Int,
    val fee: String,
)

data class TuitionInfo(
    val semester: String,
    val isPaid: Boolean,
    val totalAmount: String,
    val paidAmount: String,
    val remainingAmount: String,
    val creditsCount: Int,
    val dueDate: String,
    val courses: List<TuitionCourse>,
)

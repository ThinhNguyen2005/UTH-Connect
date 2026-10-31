package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

/** Dữ liệu Auth chỉ trả entity thuần Kotlin, có thể thay demo bằng Firebase sau. */
interface AuthRepository {
    suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
    suspend fun findStudent(studentId: String): Result<StudentProfile?>
}

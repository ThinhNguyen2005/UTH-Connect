package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

interface AuthRepository {
    suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?>
    suspend fun findStudent(studentId: String): Result<StudentProfile?>
    /** False if old password does not match. Result.failure means storage failure. */
    suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean>
}

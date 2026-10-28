package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

/** Swap the demo implementation with a Firebase/API repository later. */
interface AuthRepository {
    suspend fun authenticate(identifier: String, password: String): StudentProfile?
    suspend fun getStudentProfile(studentId: String): StudentProfile?
}

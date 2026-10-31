package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.StudentProfile

interface StudentContactRepository {
    suspend fun updateContact(
        studentId: String,
        phone: String,
        personalEmail: String,
        currentAddress: String,
    ): Result<StudentProfile>
}

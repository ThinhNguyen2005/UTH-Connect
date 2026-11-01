package com.giathinh.uthconnect.domain.repository

/** Supplies the signed-in student's ID to features that need it. */
interface StudentIdProvider {
    suspend fun studentId(): Result<String>
}

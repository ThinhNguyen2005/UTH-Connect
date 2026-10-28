package com.giathinh.uthconnect.domain.repository

/** Only an identifier is remembered in the offline demo; never passwords. */
interface SessionRepository {
    suspend fun getRememberedStudentId(): String?
    suspend fun save(studentId: String, remember: Boolean)
    suspend fun clear()
}

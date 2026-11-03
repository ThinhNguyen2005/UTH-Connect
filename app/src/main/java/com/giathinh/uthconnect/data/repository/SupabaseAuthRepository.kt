package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseRestClient,
) : AuthRepository {
    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = runCatching {
        val email = if ('@' in identifier) identifier.trim() else "${identifier.trim()}@ut.edu.vn"
        try {
            client.signIn(email, password)
        } catch (e: SupabaseHttpException) {
            if (e.status == 400 || e.status == 401) return@runCatching null
            throw e
        }
        try {
            ownProfile()
        } catch (e: Exception) {
            client.signOut()
            throw e
        }
    }

    private suspend fun ownProfile(): StudentProfile {
        val profiles = client.table(
            "profiles?select=student_id,student_id_verified,email,display_name,class_name,major,faculty,avatar_url,phone_number,role&id=eq.${client.userId}",
        ).jsonArray
        return profiles.singleOrNull()?.jsonObject?.toDomainProfile()
            ?: error("Không tìm thấy hồ sơ người dùng đã xác minh")
    }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = runCatching {
        ownProfile().takeIf { it.studentId == studentId }
    }

    override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = runCatching {
        val profile = ownProfile()
        check(profile.studentId == studentId) { "Not signed in as this student" }
        try {
            client.signIn(profile.email, oldPassword, replaceExisting = false)
        } catch (e: SupabaseHttpException) {
            if (e.status == 400 || e.status == 401) return@runCatching false
            throw e
        }
        client.updatePassword(newPassword)
        true
    }

    override suspend fun signOut() = client.signOut()
}

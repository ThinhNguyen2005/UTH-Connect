package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.DomainRestrictionException
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseRestClient,
    private val google: SupabaseGoogleExchange,
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

    private suspend fun ownProfile(): StudentProfile =
        ownProfileRow()?.toDomainProfile()
            ?: error("Không tìm thấy hồ sơ người dùng đã xác minh")

    private suspend fun ownProfileRow(): JsonObject? = client.table(
        "profiles?select=student_id,student_id_verified,email,display_name,class_name,major,faculty,avatar_url,phone_number,role&id=eq.${urlEncode(uuid(client.userId))}",
    ).jsonArray.singleOrNull()?.jsonObject

    override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> = runCatching {
        google.exchange(idToken, rawNonce)
        val row = ownProfileRow()
        val email = row?.string("email").orEmpty()
        if (!email.endsWith("@ut.edu.vn", ignoreCase = true)) {
            client.signOut()
            throw DomainRestrictionException(email)
        }
        // handle_new_user() creates an unverified profile for every signup, so a first-time
        // Google login legitimately has a row but no confirmed MSSV. Keep the session dropped:
        // RLS would only let it read events, never register, check in or claim a student ID.
        if (row == null || !row.bool("student_id_verified") || row.string("student_id").isNullOrBlank()) {
            val displayName = row?.string("display_name").orEmpty()
            client.signOut()
            throw StudentVerificationRequiredException(email = email, displayName = displayName)
        }
        try {
            row.toDomainProfile()
        } catch (error: Exception) {
            client.signOut()
            throw error
        }
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

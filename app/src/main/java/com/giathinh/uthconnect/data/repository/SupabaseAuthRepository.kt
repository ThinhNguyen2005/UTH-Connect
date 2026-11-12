package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.DomainRestrictionException
import com.giathinh.uthconnect.domain.repository.SignUpResult
import com.giathinh.uthconnect.domain.repository.StudentContactRepository
import com.giathinh.uthconnect.domain.repository.StudentVerificationRequiredException
import com.giathinh.uthconnect.domain.repository.VerificationRequestResult
import com.giathinh.uthconnect.domain.repository.VerificationStatus
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val client: SupabaseRestClient,
    private val google: SupabaseGoogleExchange,
) : AuthRepository,
    StudentContactRepository {

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = suspendResult {
        val trimmed = identifier.trim()
        val email = if ('@' in trimmed) {
            trimmed
        } else if (trimmed.all { it.isDigit() } && trimmed.length in 8..12) {
            "$trimmed@ut.edu.vn"
        } else {
            trimmed
        }

        try {
            client.signIn(email, password)
        } catch (e: SupabaseHttpException) {
            if (e.status == 400 || e.status == 401) return@suspendResult null
            throw e
        }

        val row = ownProfileRow()
        val isVerified = row?.bool("student_id_verified") == true && !row.string("student_id").isNullOrBlank()
        if (!isVerified) {
            val displayName = row?.string("display_name").orEmpty()
            throw StudentVerificationRequiredException(email = email, displayName = displayName)
        }

        try {
            row.toDomainProfile().also { CrashIdentity.set(client.userId, it.role.name) }
        } catch (e: Exception) {
            client.signOut()
            throw e
        }
    }

    private suspend fun ownProfile(): StudentProfile = (
        ownProfileRow()?.toDomainProfile()
            ?: error("Không tìm thấy hồ sơ người dùng đã xác minh")
        )
        .also { CrashIdentity.set(client.userId, it.role.name) }

    /** Never trust a remembered student ID: verify identity and MSSV against Supabase. */
    suspend fun currentVerifiedProfile(): StudentProfile {
        val row = ownProfileRow()
        check(row?.bool("student_id_verified") == true && !row.string("student_id").isNullOrBlank()) {
            "Tài khoản chưa được xác minh MSSV"
        }
        return row.toDomainProfile().also { CrashIdentity.set(client.userId, it.role.name) }
    }

    private suspend fun ownProfileRow(): JsonObject? = client.table(
        "profiles?select=student_id,student_id_verified,email,display_name,class_name,major,faculty,avatar_url,phone_number,personal_email,current_address,role&id=eq.${urlEncode(uuid(client.userId))}",
    ).jsonArray.singleOrNull()?.jsonObject

    override suspend fun authenticateWithGoogle(idToken: String, rawNonce: String): Result<StudentProfile> = suspendResult {
        google.exchange(idToken, rawNonce)
        val row = ownProfileRow()
        val email = row?.string("email").orEmpty()
        val isUtEmail = email.endsWith("@ut.edu.vn", ignoreCase = true) || email.endsWith("@uth.edu.vn", ignoreCase = true)
        if (!isUtEmail) {
            client.signOut()
            throw DomainRestrictionException(email)
        }
        // If not verified yet, preserve active session so student can submit student ID verification request
        if (row == null || !row.bool("student_id_verified") || row.string("student_id").isNullOrBlank()) {
            val displayName = row?.string("display_name").orEmpty()
            throw StudentVerificationRequiredException(email = email, displayName = displayName)
        }
        try {
            row.toDomainProfile().also { CrashIdentity.set(client.userId, it.role.name) }
        } catch (error: Exception) {
            client.signOut()
            throw error
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        faculty: String,
    ): Result<SignUpResult> = suspendResult {
        try {
            val response = client.signUp(email, password, displayName, faculty)
            val hasSession = response.containsKey("access_token")
            val userObj = response["user"]?.jsonObject
            val confirmedAt = userObj?.get("confirmed_at")?.jsonPrimitive?.content
            val needsConfirmation = !hasSession && confirmedAt.isNullOrBlank()

            if (hasSession) {
                CrashIdentity.set(client.userId, "pending_profile")
                try {
                    val row = ownProfileRow()
                    if (row != null) {
                        row.toDomainProfile().also { CrashIdentity.set(client.userId, it.role.name) }
                    }
                } catch (_: Exception) {
                    // Retain "pending_profile" identity if profile fetch fails
                }
            } else {
                CrashIdentity.clear()
            }

            SignUpResult(needsEmailConfirmation = needsConfirmation, email = email)
        } catch (error: Exception) {
            CrashIdentity.clear()
            throw error
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = suspendResult {
        client.recoverPassword(email)
    }

    override suspend fun completePasswordReset(email: String, otp: String, newPassword: String): Result<Unit> = suspendResult {
        client.resetPasswordWithOtp(email, otp, newPassword)
    }

    override suspend fun submitVerificationRequest(studentId: String): Result<VerificationRequestResult> = suspendResult {
        val response = client.rpc(
            "submit_student_verification_request",
            buildJsonObject { put("p_student_id", studentId) },
        ).jsonObject
        val reqId = response["request_id"]?.jsonPrimitive?.content.orEmpty()
        VerificationRequestResult(requestId = reqId, status = VerificationStatus.PENDING)
    }

    override suspend fun checkVerificationStatus(): Result<VerificationStatus?> = suspendResult {
        val requests = client.table(
            "student_verification_requests?select=status&order=created_at.desc&limit=1",
        ).jsonArray
        val latest = requests.firstOrNull()?.jsonObject
        when (latest?.string("status")) {
            "APPROVED" -> VerificationStatus.APPROVED
            "REJECTED" -> VerificationStatus.REJECTED
            "PENDING" -> VerificationStatus.PENDING
            else -> null
        }
    }

    override suspend fun updateContact(
        studentId: String,
        phone: String,
        personalEmail: String,
        currentAddress: String,
    ): Result<StudentProfile> = suspendResult {
        val payload = buildJsonObject {
            put("phone_number", phone)
            put("personal_email", personalEmail)
            put("current_address", currentAddress)
        }
        client.patchTable("profiles?id=eq.${urlEncode(uuid(client.userId))}", payload)
        ownProfile()
    }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = suspendResult {
        ownProfile().takeIf { it.studentId == studentId }
    }

    override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String): Result<Boolean> = suspendResult {
        val profile = ownProfile()
        check(profile.studentId == studentId) { "Not signed in as this student" }
        try {
            if (!client.verifyCurrentPassword(profile.email, oldPassword)) return@suspendResult false
        } catch (e: SupabaseHttpException) {
            if (e.status == 400 || e.status == 401) return@suspendResult false
            throw e
        }
        client.updatePassword(newPassword)
        true
    }

    override suspend fun signOut() = client.signOut()
}

package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Triển khai AuthRepository sử dụng Firebase Authentication và Firestore Database.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> = runCatching {
        val targetEmail = resolveEmail(identifier) ?: return@runCatching null

        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(targetEmail, password).await()
            val firebaseUser = authResult.user ?: return@runCatching null

            fetchProfileFromFirestore(firebaseUser.uid, identifier, targetEmail)
        } catch (_: FirebaseAuthInvalidUserException) {
            null
        } catch (_: FirebaseAuthInvalidCredentialsException) {
            null
        } catch (e: FirebaseAuthException) {
            if (e.errorCode == "ERROR_WRONG_PASSWORD" || e.errorCode == "ERROR_USER_NOT_FOUND") {
                null
            } else {
                throw e
            }
        }
    }

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> = runCatching {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .whereEqualTo(FIELD_STUDENT_ID, studentId)
            .limit(1)
            .get()
            .await()

        if (snapshot.isEmpty) {
            null
        } else {
            val doc = snapshot.documents.first()
            doc.toStudentProfile(studentId)
        }
    }

    private suspend fun resolveEmail(identifier: String): String? {
        if (identifier.contains("@")) {
            return identifier
        }

        // Nếu identifier là MSSV: kiểm tra trong Firestore để tìm email tương ứng
        val query = firestore.collection(USERS_COLLECTION)
            .whereEqualTo(FIELD_STUDENT_ID, identifier)
            .limit(1)
            .get()
            .await()

        if (!query.isEmpty) {
            val email = query.documents.first().getString(FIELD_EMAIL)
            if (!email.isNullOrBlank()) return email
        }

        // Định dạng email mặc định của sinh viên UTH theo MSSV
        return "$identifier@ut.edu.vn"
    }

    private suspend fun fetchProfileFromFirestore(
        uid: String,
        fallbackStudentId: String,
        email: String,
    ): StudentProfile {
        val doc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return if (doc.exists()) {
            doc.toStudentProfile(fallbackStudentId)
        } else {
            val cleanStudentId = if (!fallbackStudentId.contains("@")) {
                fallbackStudentId
            } else {
                email.substringBefore("@")
            }

            StudentProfile(
                studentId = cleanStudentId,
                email = email,
                displayName = firebaseAuth.currentUser?.displayName ?: "Sinh viên UTH",
                className = "Chưa cập nhật",
                major = "Công nghệ thông tin",
                faculty = "Khoa Công nghệ thông tin",
                avatarUrl = firebaseAuth.currentUser?.photoUrl?.toString(),
            )
        }
    }

    private fun DocumentSnapshot.toStudentProfile(fallbackStudentId: String): StudentProfile {
        return StudentProfile(
            studentId = getString(FIELD_STUDENT_ID) ?: fallbackStudentId,
            email = getString(FIELD_EMAIL) ?: "",
            displayName = getString(FIELD_DISPLAY_NAME) ?: "Sinh viên UTH",
            className = getString(FIELD_CLASS_NAME) ?: "Chưa cập nhật",
            major = getString(FIELD_MAJOR) ?: "Chưa cập nhật",
            faculty = getString(FIELD_FACULTY) ?: "Chưa cập nhật",
            avatarUrl = getString(FIELD_AVATAR_URL),
        )
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FIELD_STUDENT_ID = "studentId"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_CLASS_NAME = "className"
        private const val FIELD_MAJOR = "major"
        private const val FIELD_FACULTY = "faculty"
        private const val FIELD_AVATAR_URL = "avatarUrl"
    }
}

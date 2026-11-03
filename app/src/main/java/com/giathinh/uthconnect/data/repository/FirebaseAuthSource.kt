package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Ranh giới SDK để kiểm thử chính sách repository mà không cần mạng. */
interface FirebaseAuthSource {
    val changes: Flow<Boolean>
    suspend fun signIn(email: String, password: String)
    suspend fun account(): FirebaseAccount?
    fun signOut()
    suspend fun resetPassword(email: String)
    suspend fun changePassword(old: String, updated: String)
    suspend fun updateContact(fields: Map<String, String>)
    suspend fun sendVerification()
}

data class FirebaseAccount(
    val uid: String,
    val email: String,
    val emailVerified: Boolean,
    val claims: Map<String, Any>,
    val fields: Map<String, Any>,
)

/** Không tạo FirebaseOptions giả khi google-services.json chưa được cung cấp. */
@Singleton
class SdkFirebaseAuthSource @Inject constructor(
    private val authProvider: Provider<FirebaseAuth>,
    private val firestoreProvider: Provider<FirebaseFirestore>,
) : FirebaseAuthSource {
    private val app: FirebaseApp
        get() = try { FirebaseApp.getInstance() }
        catch (_: IllegalStateException) { throw AuthException(AuthError.Configuration) }
    private val auth: FirebaseAuth get() { app; return authProvider.get() }
    private val store: FirebaseFirestore get() { app; return firestoreProvider.get() }

    override val changes: Flow<Boolean> = callbackFlow {
        val instance = try { auth } catch (_: AuthException) { close(); return@callbackFlow }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        instance.addAuthStateListener(listener)
        awaitClose { instance.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String) {
        // Task đăng nhập không hỗ trợ hủy. Chờ kết thúc trước khi repository dọn phiên bị hủy,
        // tránh SDK đăng nhập muộn sau khi signOut đã chạy.
        withContext(NonCancellable) { auth.signInWithEmailAndPassword(email, password).await() }
        currentCoroutineContext().ensureActive()
    }

    override suspend fun account(): FirebaseAccount? {
        val user = auth.currentUser ?: return null
        // Reload và token mới bắt buộc: không cấp phiên chỉ từ cache hoặc MSSV đã lưu.
        user.reload().await()
        val token = user.getIdToken(true).await()
        if (auth.currentUser?.uid != user.uid) throw AuthException(AuthError.InvalidSession)
        if (!user.isEmailVerified || token.claims["uthVerified"] != true) {
            return FirebaseAccount(user.uid, user.email.orEmpty(), user.isEmailVerified, token.claims, emptyMap())
        }
        val document = store.collection("users").document(user.uid).get(Source.SERVER).await()
        if (!document.exists()) throw AuthException(AuthError.ProfileMissing)
        return FirebaseAccount(user.uid, user.email.orEmpty(), user.isEmailVerified, token.claims,
            document.data.orEmpty())
    }

    override fun signOut() {
        try { auth.signOut() }
        catch (error: AuthException) { if (error.reason != AuthError.Configuration) throw error }
    }
    override suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email).await() }
    override suspend fun sendVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
    }
    override suspend fun changePassword(old: String, updated: String) {
        val user = auth.currentUser ?: throw AuthException(AuthError.InvalidSession)
        val email = user.email ?: throw AuthException(AuthError.InvalidSession)
        user.reauthenticate(EmailAuthProvider.getCredential(email, old)).await()
        user.updatePassword(updated).await()
    }
    override suspend fun updateContact(fields: Map<String, String>) {
        val user = auth.currentUser ?: throw AuthException(AuthError.InvalidSession)
        val reference = store.collection("users").document(user.uid)
        // Transaction không xếp hàng offline vô thời hạn như update(): UI nhận được lỗi mất mạng.
        store.runTransaction { transaction ->
            if (!transaction.get(reference).exists()) throw AuthException(AuthError.ProfileMissing)
            transaction.update(reference, fields)
        }.await()
    }
}

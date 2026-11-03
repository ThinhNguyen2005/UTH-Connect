package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.repository.AuthError
import com.giathinh.uthconnect.domain.repository.AuthException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.tasks.await

/** Ranh giới SDK để kiểm thử chính sách repository mà không cần mạng. */
interface FirebaseAuthSource {
    val tokenChanges: Flow<Unit> get() = emptyFlow()
    suspend fun tokenAccount(): FirebaseAccount? = account()
    suspend fun identity(): FirebaseAccount? = account()
    suspend fun register(email: String, password: String) { throw AuthException(AuthError.Configuration) }
    suspend fun signInGoogle(token: String) { throw AuthException(AuthError.Configuration) }
    suspend fun verifyUthAccount() { throw AuthException(AuthError.ApprovalRequired) }
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
    // Giữ constructor/provider Firestore chung tương thích DI remote; hồ sơ dùng transport không disk cache.
    private val profileStore = FirebaseProfileRemoteStore(authProvider)

    override val changes: Flow<Boolean> = callbackFlow {
        val instance = try { auth } catch (_: AuthException) { close(); return@callbackFlow }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser != null) }
        instance.addAuthStateListener(listener)
        awaitClose { instance.removeAuthStateListener(listener) }
    }

    override val tokenChanges: Flow<Unit> = callbackFlow {
        val instance = try { auth } catch (_: AuthException) { close(); return@callbackFlow }
        val listener = FirebaseAuth.IdTokenListener { trySend(Unit) }
        instance.addIdTokenListener(listener)
        awaitClose { instance.removeIdTokenListener(listener) }
    }
    override suspend fun tokenAccount(): FirebaseAccount? {
        val user = auth.currentUser ?: return null
        val token = user.getIdToken(false).await()
        val verified = token.claims["email_verified"] == true
        val fields = if (verified && token.claims["uthVerified"] == true) profileStore.read() else emptyMap()
        return FirebaseAccount(user.uid, user.email.orEmpty(), verified, token.claims, fields)
    }

    override suspend fun signIn(email: String, password: String) {
        // Task đăng nhập không hỗ trợ hủy. Chờ kết thúc trước khi repository dọn phiên bị hủy,
        // tránh SDK đăng nhập muộn sau khi signOut đã chạy.
        withContext(NonCancellable) { auth.signInWithEmailAndPassword(email, password).await() }
        currentCoroutineContext().ensureActive()
    }

    override suspend fun identity(): FirebaseAccount? {
        val user = auth.currentUser ?: return null
        user.reload().await()
        val token = user.getIdToken(true).await()
        if (auth.currentUser?.uid != user.uid) throw AuthException(AuthError.InvalidSession)
        return FirebaseAccount(user.uid, user.email.orEmpty(), user.isEmailVerified, token.claims, emptyMap())
    }

    override suspend fun register(email: String, password: String) {
        withContext(NonCancellable) { auth.createUserWithEmailAndPassword(email, password).await() }
        currentCoroutineContext().ensureActive()
    }
    override suspend fun signInGoogle(token: String) {
        withContext(NonCancellable) { auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await() }
        currentCoroutineContext().ensureActive()
    }
    override suspend fun verifyUthAccount() {
        identity() ?: throw AuthException(AuthError.InvalidSession)
        FirebaseFunctions.getInstance(app).getHttpsCallable("verifyUthAccount").call(emptyMap<String, String>()).await()
        auth.currentUser?.getIdToken(true)?.await()
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
        return FirebaseAccount(user.uid, user.email.orEmpty(), user.isEmailVerified, token.claims, profileStore.read())
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
    override suspend fun updateContact(fields: Map<String, String>) = profileStore.update(fields)
}

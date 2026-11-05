package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.User
import com.giathinh.uthconnect.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fake/Static implementation của [UserRepository] dùng để UI team
 * (KAN-32 – Anh Phú) có thể lấy dữ liệu ngay mà không cần Firestore thật.
 *
 * Thay thế bằng FirestoreUserRepository khi backend sẵn sàng.
 */
class FakeUserRepository @Inject constructor() : UserRepository {

    // -----------------------------------------------------------------------
    // In-memory state – giả lập một sinh viên đang đăng nhập
    // -----------------------------------------------------------------------
    private val currentUserFlow = MutableStateFlow<User?>(DEMO_USER)
    private val userMapFlow = MutableStateFlow(mapOf(DEMO_USER.uid to DEMO_USER))

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    override fun getUserProfile(uid: String): Flow<User?> = userMapFlow.map { it[uid] }

    override suspend fun updateProfile(user: User): Result<Unit> = runCatching {
        userMapFlow.value = userMapFlow.value.toMutableMap().apply { put(user.uid, user) }
        if (currentUserFlow.value?.uid == user.uid) {
            currentUserFlow.value = user
        }
    }

    override fun getCurrentUser(): Flow<User?> = currentUserFlow

    // -----------------------------------------------------------------------
    // Demo data
    // -----------------------------------------------------------------------
    companion object {
        val DEMO_USER: User = DemoAccount.user
    }
}

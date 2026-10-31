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
 * Thay thế bằng [FirestoreUserRepository] khi backend sẵn sàng.
 */
class FakeUserRepository @Inject constructor() : UserRepository {

    // -----------------------------------------------------------------------
    // In-memory state – giả lập một sinh viên đang đăng nhập
    // -----------------------------------------------------------------------
    private val _currentUser = MutableStateFlow<User?>(DEMO_USER)
    private val _userMap = MutableStateFlow(mapOf(DEMO_USER.uid to DEMO_USER))

    // -----------------------------------------------------------------------
    // Interface implementation
    // -----------------------------------------------------------------------

    override fun getUserProfile(uid: String): Flow<User?> =
        _userMap.map { it[uid] }

    override suspend fun updateProfile(user: User): Result<Unit> = runCatching {
        _userMap.value = _userMap.value.toMutableMap().apply { put(user.uid, user) }
        if (_currentUser.value?.uid == user.uid) {
            _currentUser.value = user
        }
    }

    override fun getCurrentUser(): Flow<User?> = _currentUser

    // -----------------------------------------------------------------------
    // Demo data
    // -----------------------------------------------------------------------
    companion object {
        val DEMO_USER = User(
            uid = "demo-uid-001",
            studentId = "2251010001",
            email = "nguyen.trongnghia@uth.edu.vn",
            displayName = "Nguyễn Trọng Nghĩa",
            className = "22DHTH04",
            major = "Công nghệ Thông tin",
            faculty = "Khoa Công nghệ Thông tin",
            avatarUrl = null,
            phoneNumber = "0901234567",
            role = "student",
        )
    }
}

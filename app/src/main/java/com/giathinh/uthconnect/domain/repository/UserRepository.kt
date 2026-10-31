package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository quản lý thông tin người dùng / sinh viên UTH.
 * Phục vụ KAN-32 (Anh Phú – Profile) và các feature cần getUserProfile.
 */
interface UserRepository {
    /** Lấy thông tin người dùng theo [uid] Firebase Auth. Emit lại khi có thay đổi. */
    fun getUserProfile(uid: String): Flow<User?>

    /** Cập nhật thông tin profile người dùng. */
    suspend fun updateProfile(user: User): Result<Unit>

    /** Lấy người dùng hiện tại (đang đăng nhập), null nếu chưa đăng nhập. */
    fun getCurrentUser(): Flow<User?>
}

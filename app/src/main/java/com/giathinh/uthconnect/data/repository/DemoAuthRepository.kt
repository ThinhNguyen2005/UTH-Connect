package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/** Chỉ phục vụ đồ án theo lựa chọn giả lập được cho phép trong ảnh giao việc. */
@Singleton
class DemoAuthRepository @Inject constructor() : AuthRepository {
    private val demo = DemoAccount.profile

    override suspend fun authenticate(identifier: String, password: String): Result<StudentProfile?> =
        Result.success(demo.takeIf {
            (identifier == it.studentId || identifier.equals(it.email, ignoreCase = true)) &&
                password == DEMO_PASSWORD
        })

    override suspend fun findStudent(studentId: String): Result<StudentProfile?> =
        Result.success(demo.takeIf { it.studentId == studentId })

    companion object { const val DEMO_PASSWORD = DemoAccount.PASSWORD }
}

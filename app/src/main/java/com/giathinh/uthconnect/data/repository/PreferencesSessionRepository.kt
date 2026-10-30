package com.giathinh.uthconnect.data.repository

import android.content.Context
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionFailure
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** SharedPreferences đáp ứng ảnh giao việc và không cần sửa dependency của repo nhóm. */
@Singleton
class PreferencesSessionRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val auth: AuthRepository,
) : SessionRepository {
    private val prefs = context.getSharedPreferences("uth_auth_demo", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val _session = MutableStateFlow(StudentSession())
    override val session: StateFlow<StudentSession> = _session.asStateFlow()
    private var restored = false

    override suspend fun restore() {
        mutex.withLock {
            if (restored) return
            restored = true
            try {
                val studentId = prefs.getString(KEY_ID, null)
                val profile = studentId?.let { auth.findStudent(it).getOrThrow() }
                if (studentId != null && profile == null) {
                    prefs.edit().remove(KEY_ID).commit()
                }
                _session.value = StudentSession(
                    isRestoring = false,
                    profile = profile,
                    remembered = profile != null,
                )
            } catch (cancelled: CancellationException) {
                restored = false
                throw cancelled
            } catch (_: Exception) {
                _session.value = StudentSession(isRestoring = false, failure = SessionFailure.RestoreFailed)
            }
        }
    }

    override suspend fun start(profile: StudentProfile, remember: Boolean) {
        mutex.withLock {
            val edit = prefs.edit()
            if (remember) edit.putString(KEY_ID, profile.studentId) else edit.remove(KEY_ID)
            if (!edit.commit()) throw IllegalStateException("Không ghi được phiên demo")
            _session.value = StudentSession(
                isRestoring = false,
                profile = profile,
                remembered = remember,
            )
        }
    }

    override suspend fun signOut() {
        mutex.withLock {
            if (!prefs.edit().remove(KEY_ID).commit()) {
                throw IllegalStateException("Không thể xóa phiên đã lưu")
            }
            _session.value = StudentSession(isRestoring = false)
        }
    }

    private companion object { const val KEY_ID = "remembered_student_id" }
}

package com.giathinh.uthconnect.data.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PreferencesSessionSignOutTest {
    @Test fun logoutClearsLocalStateWhenBackendFails() = runBlocking {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val name = "test_session_${UUID.randomUUID()}"
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(ignored: String, mode: Int) = base.getSharedPreferences(name, mode)
        }
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val auth = object : AuthRepository {
            override suspend fun authenticate(identifier: String, password: String) = Result.success<StudentProfile?>(null)
            override suspend fun findStudent(studentId: String) = Result.success<StudentProfile?>(null)
            override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(false)
            override suspend fun signOut() { error("backend unavailable") }
        }
        try {
            val repository = PreferencesSessionRepository(context, auth)
            repository.start(StudentProfile("12345678", "a@ut.edu.vn", "A", "C", "M", "F"), false)
            prefs.edit().putString("remembered_student_id", "12345678").putString("biometric_student_id", "12345678").commit()

            assertThrows(IllegalStateException::class.java) { runBlocking { repository.signOut() } }
            assertNull(repository.session.value.profile)
            assertFalse(repository.session.value.isRestoring)
            assertFalse(prefs.contains("remembered_student_id"))
            assertFalse(prefs.contains("biometric_student_id"))
        } finally {
            prefs.edit().clear().commit()
        }
    }
}

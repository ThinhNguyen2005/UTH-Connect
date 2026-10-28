package com.giathinh.uthconnect.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giathinh.uthconnect.domain.repository.SessionRepository
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "uth_demo_session")

/** Keeps only the remembered demo Student ID; no password is written to disk. */
class DataStoreSessionRepository(context: Context) : SessionRepository {
    private val dataStore = context.applicationContext.authDataStore
    private val studentIdKey = stringPreferencesKey("remembered_student_id")

    override suspend fun getRememberedStudentId(): String? = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[studentIdKey] }
        .first()

    override suspend fun save(studentId: String, remember: Boolean) {
        dataStore.edit { preferences ->
            if (remember) preferences[studentIdKey] = studentId
            else preferences.remove(studentIdKey)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(studentIdKey) }
    }
}

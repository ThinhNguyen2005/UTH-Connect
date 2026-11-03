package com.giathinh.uthconnect.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SessionGrants(val rememberedId: String? = null, val biometricId: String? = null)

interface SessionPreferences {
    suspend fun read(): SessionGrants
    suspend fun write(grants: SessionGrants)
}

/** Không đọc lại quyền demo cũ; không lưu mật khẩu hay token Firebase. */
@Singleton
class SharedSessionPreferences @Inject constructor(@ApplicationContext context: Context) : SessionPreferences {
    private val prefs = context.getSharedPreferences("uth_auth_session", Context.MODE_PRIVATE)
    override suspend fun read(): SessionGrants = withContext(Dispatchers.IO) {
        SessionGrants(prefs.getString("remembered_uid", null), prefs.getString("biometric_uid", null))
    }
    override suspend fun write(grants: SessionGrants) = withContext(Dispatchers.IO) {
        check(prefs.edit().putString("remembered_uid", grants.rememberedId)
            .putString("biometric_uid", grants.biometricId).commit()) { "Không lưu được tùy chọn phiên" }
    }
}

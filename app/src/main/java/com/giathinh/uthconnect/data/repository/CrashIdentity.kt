package com.giathinh.uthconnect.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal object CrashIdentity {
    fun set(userId: String, role: String) {
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setUserId(userId)
                setCustomKey("user_role", role)
            }
        }
    }

    fun clear() = set("", "")
}

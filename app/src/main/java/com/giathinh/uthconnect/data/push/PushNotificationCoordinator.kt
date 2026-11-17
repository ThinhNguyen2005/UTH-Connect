package com.giathinh.uthconnect.data.push

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Serializes notification display with account changes and local sign-out. */
@Singleton
class PushNotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Any()
    private val preferences = context.getSharedPreferences(PUSH_PREFERENCES, Context.MODE_PRIVATE)

    @Volatile private var signOutInProgress = false

    fun sessionStarted(userId: String?) {
        if (userId.isNullOrBlank()) return
        synchronized(lock) {
            if (preferences.getString(KEY_ACTIVE_USER_ID, null) != userId) {
                NotificationManagerCompat.from(context).cancelAll()
            }
            preferences.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
            signOutInProgress = false
        }
    }

    fun signOutStarted() {
        synchronized(lock) {
            signOutInProgress = true
            preferences.edit().remove(KEY_ACTIVE_USER_ID).apply()
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    fun notifyForCurrentUser(
        expectedUserId: String,
        currentUserId: () -> String?,
        notificationId: Int,
        notification: Notification,
    ): Boolean = synchronized(lock) {
        if (signOutInProgress || currentUserId() != expectedUserId) return@synchronized false
        val activeUserId = preferences.getString(KEY_ACTIVE_USER_ID, null)
        if (activeUserId != null && activeUserId != expectedUserId) {
            NotificationManagerCompat.from(context).cancelAll()
        }
        preferences.edit().putString(KEY_ACTIVE_USER_ID, expectedUserId).apply()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        true
    }

    private companion object {
        const val PUSH_PREFERENCES = "uth_push"
        const val KEY_ACTIVE_USER_ID = "active_notification_user_id"
    }
}

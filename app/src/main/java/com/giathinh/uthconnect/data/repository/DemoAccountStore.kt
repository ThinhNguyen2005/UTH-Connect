package com.giathinh.uthconnect.data.repository

import android.content.SharedPreferences

/**
 * Storage seam for demo account credentials and contact fields.
 *
 * Decouples [DemoAuthRepository] from the Android [android.content.Context] so the demo login
 * behaviour can be unit tested with an in-memory store, keeping the project's "no Robolectric,
 * no Mockito" convention.
 */
interface DemoAccountStore {
    /** Returns the stored value or null when the key is absent. */
    fun getString(key: String): String?

    /** Atomically writes several string values; returns false when the commit fails. */
    fun putAll(values: Map<String, String>): Boolean
}

/** [DemoAccountStore] backed by Android [SharedPreferences]. */
class SharedPreferencesDemoAccountStore(
    private val prefs: SharedPreferences,
) : DemoAccountStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putAll(values: Map<String, String>): Boolean =
        prefs.edit().apply { values.forEach { (k, v) -> putString(k, v) } }.commit()
}

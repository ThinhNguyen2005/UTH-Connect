package com.giathinh.uthconnect

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppAvailabilityTest {
    @Test
    fun maintenanceTakesPriority() {
        assertEquals(AppAvailability.MAINTENANCE, availability(true, "1.0", "2.0"))
    }

    @Test
    fun versionComponentsCompareNumerically() {
        assertEquals(AppAvailability.AVAILABLE, availability(false, "1.0", "1.0.0"))
        assertEquals(AppAvailability.UPDATE_REQUIRED, availability(false, "1.9.0", "1.10.0"))
        assertEquals(AppAvailability.AVAILABLE, availability(false, "2.0.0", "1.10.0"))
    }

    @Test
    fun malformedRemoteVersionDoesNotBlockUsers() {
        assertEquals(AppAvailability.AVAILABLE, availability(false, "1.0", "invalid"))
    }

    @Test
    fun remoteConfigFetchIntervalSecondsRespectsUserInitiatedAndDebug() {
        assertEquals(3600L, remoteConfigFetchIntervalSeconds(isDebug = false, userInitiated = false))
        assertEquals(0L, remoteConfigFetchIntervalSeconds(isDebug = false, userInitiated = true))
        assertEquals(0L, remoteConfigFetchIntervalSeconds(isDebug = true, userInitiated = false))
        assertEquals(0L, remoteConfigFetchIntervalSeconds(isDebug = true, userInitiated = true))
    }

    @Test
    fun isValidUpdateUrlValidatesHttpsAndMarket() {
        assertTrue(isValidUpdateUrl("https://play.google.com/store/apps/details?id=com.giathinh.uthconnect"))
        assertTrue(isValidUpdateUrl("market://details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl(""))
        assertFalse(isValidUpdateUrl("   "))
        assertFalse(isValidUpdateUrl("http://insecure.site/app.apk"))
        assertFalse(isValidUpdateUrl("ftp://invalid.scheme"))
    }

    @Test
    fun savedGateMaintenanceUnblocksWhenFetchSucceedsWithDefaultSource() {
        val saved = SavedGate(maintenanceMode = true, forceUpdateMinVersion = "1.0.0", hasSavedState = true)
        val (state, newSaved) = resolveGate(
            saved = saved,
            fetchSucceeded = true,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.AVAILABLE, state)
        assertEquals(false, newSaved.maintenanceMode)
    }

    @Test
    fun savedGateHighMinVersionResetsWhenFetchSucceedsWithDefaultSource() {
        val saved = SavedGate(maintenanceMode = false, forceUpdateMinVersion = "9.9.9", hasSavedState = true)
        val (state, newSaved) = resolveGate(
            saved = saved,
            fetchSucceeded = true,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.AVAILABLE, state)
        assertEquals("1.0.0", newSaved.forceUpdateMinVersion)
    }

    @Test
    fun resolveGatePreservesDynamicUpdateUrl() {
        val saved = SavedGate(
            maintenanceMode = false,
            forceUpdateMinVersion = "1.0.0",
            updateUrl = "https://play.google.com/store/apps/details?id=com.giathinh.uthconnect",
            hasSavedState = true,
        )
        val (state, newSaved) = resolveGate(
            saved = saved,
            fetchSucceeded = true,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            remoteUpdateUrl = "",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.AVAILABLE, state)
        assertEquals("https://play.google.com/store/apps/details?id=com.giathinh.uthconnect", newSaved.updateUrl)
    }

    @Test
    fun fetchFailurePreservesSavedBlockingGate() {
        val saved = SavedGate(maintenanceMode = true, forceUpdateMinVersion = "1.0.0", hasSavedState = true)
        val (state, _) = resolveGate(
            saved = saved,
            fetchSucceeded = false,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.MAINTENANCE, state)
    }

    @Test
    fun fetchFailurePreservesSavedUpdateRequiredGate() {
        val saved = SavedGate(maintenanceMode = false, forceUpdateMinVersion = "9.9.9", hasSavedState = true)
        val (state, _) = resolveGate(
            saved = saved,
            fetchSucceeded = false,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.UPDATE_REQUIRED, state)
    }

    @Test
    fun freshInstallFailsOpenAsAvailableWhenFetchFails() {
        val saved = SavedGate(hasSavedState = false)
        val (state, _) = resolveGate(
            saved = saved,
            fetchSucceeded = false,
            remoteMaintenance = false,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.AVAILABLE, state)
    }

    @Test
    fun fetchSuccessWithRemoteSourceUsesRemoteValues() {
        val saved = SavedGate(maintenanceMode = false, forceUpdateMinVersion = "1.0.0", hasSavedState = true)
        val (state, newSaved) = resolveGate(
            saved = saved,
            fetchSucceeded = true,
            remoteMaintenance = true,
            remoteMinVersion = "1.0.0",
            currentVersion = "1.0.0",
        )
        assertEquals(AppAvailability.MAINTENANCE, state)
        assertEquals(true, newSaved.maintenanceMode)
    }

    @Test
    fun unsavedSharedPreferencesReturnsDefaultSavedGate() {
        val fake = FakeSharedPreferences()
        val loaded = loadSavedGate(fake)
        assertFalse(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
    }

    @Test
    fun saveGateAndLoadSavedGateRoundTrip() {
        val fake = FakeSharedPreferences()
        val initial = SavedGate(
            maintenanceMode = true,
            forceUpdateMinVersion = "2.0.0",
            updateUrl = "https://play.google.com/store/apps/details?id=com.giathinh.uthconnect",
            hasSavedState = true,
        )
        saveGate(fake, initial)

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertTrue(loaded.maintenanceMode)
        assertEquals("2.0.0", loaded.forceUpdateMinVersion)
        assertEquals("https://play.google.com/store/apps/details?id=com.giathinh.uthconnect", loaded.updateUrl)
    }

    @Test
    fun saveGateOverwritesPreviousValues() {
        val fake = FakeSharedPreferences()
        saveGate(fake, SavedGate(maintenanceMode = true, forceUpdateMinVersion = "2.0.0", hasSavedState = true))
        saveGate(fake, SavedGate(maintenanceMode = false, forceUpdateMinVersion = "1.5.0", hasSavedState = true))

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.5.0", loaded.forceUpdateMinVersion)
    }

    @Test
    fun missingKeysReturnSafeDefaults() {
        val fake = FakeSharedPreferences()
        fake.edit().putBoolean("has_saved_state", true).apply()
        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
        assertEquals("", loaded.updateUrl)
    }

    @Test
    fun wrongTypedKeysFallBackToDefaultGate() {
        val fake = FakeSharedPreferences()
        fake.edit().putString("maintenance_mode", "corrupted_string").apply()
        val loaded = loadSavedGate(fake)
        assertFalse(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
        assertEquals("", loaded.updateUrl)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map

        override fun getString(key: String, defValue: String?): String? {
            if (!map.containsKey(key)) return defValue
            val value = map[key]
            if (value != null && value !is String) throw ClassCastException("Value $value for $key is not String")
            @Suppress("UNCHECKED_CAST")
            return (value as? String) ?: defValue
        }

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues

        override fun getInt(key: String, defValue: Int): Int {
            if (!map.containsKey(key)) return defValue
            val value = map[key]
            if (value !is Int) throw ClassCastException("Value $value for $key is not Int")
            return value
        }

        override fun getLong(key: String, defValue: Long): Long {
            if (!map.containsKey(key)) return defValue
            val value = map[key]
            if (value !is Long) throw ClassCastException("Value $value for $key is not Long")
            return value
        }

        override fun getFloat(key: String, defValue: Float): Float {
            if (!map.containsKey(key)) return defValue
            val value = map[key]
            if (value !is Float) throw ClassCastException("Value $value for $key is not Float")
            return value
        }

        override fun getBoolean(key: String, defValue: Boolean): Boolean {
            if (!map.containsKey(key)) return defValue
            val value = map[key]
            if (value !is Boolean) throw ClassCastException("Value $value for $key is not Boolean")
            return value
        }

        override fun contains(key: String): Boolean = map.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(map)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            // No-op for unit test
        }

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            // No-op for unit test
        }

        private class FakeEditor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val changes = mutableMapOf<String, Any?>()
            private val removedKeys = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                changes[key] = value
                removedKeys.remove(key)
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                changes[key] = value
                removedKeys.remove(key)
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                changes[key] = value
                removedKeys.remove(key)
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                changes[key] = value
                removedKeys.remove(key)
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                changes[key] = value
                removedKeys.remove(key)
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                changes.remove(key)
                removedKeys.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) {
                    target.clear()
                }
                for (key in removedKeys) {
                    target.remove(key)
                }
                target.putAll(changes)
            }
        }
    }
}

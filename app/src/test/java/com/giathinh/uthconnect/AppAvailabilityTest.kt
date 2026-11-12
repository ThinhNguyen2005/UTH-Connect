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
        val initial = SavedGate(maintenanceMode = true, forceUpdateMinVersion = "2.0.0", hasSavedState = true)
        saveGate(fake, initial)

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertTrue(loaded.maintenanceMode)
        assertEquals("2.0.0", loaded.forceUpdateMinVersion)
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
    fun missingOrCorruptedKeysReturnSafeDefaults() {
        val fake = FakeSharedPreferences()
        fake.edit().putBoolean("has_saved_state", true).apply()
        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
    }

    private class FakeSharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = map

        override fun getString(key: String, defValue: String?): String? = (map[key] as? String) ?: defValue

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues

        override fun getInt(key: String, defValue: Int): Int = (map[key] as? Int) ?: defValue

        override fun getLong(key: String, defValue: Long): Long = (map[key] as? Long) ?: defValue

        override fun getFloat(key: String, defValue: Float): Float = (map[key] as? Float) ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue

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
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                changes[key] = value
                return this
            }

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = this

            override fun putInt(key: String, value: Int): SharedPreferences.Editor {
                changes[key] = value
                return this
            }

            override fun putLong(key: String, value: Long): SharedPreferences.Editor {
                changes[key] = value
                return this
            }

            override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
                changes[key] = value
                return this
            }

            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
                changes[key] = value
                return this
            }

            override fun remove(key: String): SharedPreferences.Editor {
                changes.remove(key)
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
                if (clearAll) target.clear()
                target.putAll(changes)
            }
        }
    }
}

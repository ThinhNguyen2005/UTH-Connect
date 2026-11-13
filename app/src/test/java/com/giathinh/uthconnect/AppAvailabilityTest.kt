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
    fun isValidUpdateUrlAcceptsApprovedTargets() {
        assertTrue(isValidUpdateUrl("https://play.google.com/store/apps/details?id=com.giathinh.uthconnect"))
        assertTrue(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases"))
        assertTrue(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases/"))
        assertTrue(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases/latest"))
        assertTrue(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases/tag/v1.0.0"))
        assertTrue(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases/download/v1.0.0/app.apk"))
        assertTrue(isValidUpdateUrl("https://ut.edu.vn/download/app.apk"))
        assertTrue(isValidUpdateUrl("https://uth.edu.vn/download/uth-connect.apk"))
        assertTrue(isValidUpdateUrl("market://details?id=com.giathinh.uthconnect"))
    }

    @Test
    fun isValidUpdateUrlRejectsInsecureSchemesAndBlankInputs() {
        assertFalse(isValidUpdateUrl(""))
        assertFalse(isValidUpdateUrl("   "))
        assertFalse(isValidUpdateUrl("http://play.google.com/store/apps/details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("http://github.com/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("http://ut.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("ftp://ut.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("javascript:alert(1)"))
    }

    @Test
    fun isValidUpdateUrlRejectsUserinfoSpoofingAndCustomPorts() {
        assertFalse(isValidUpdateUrl("https://github.com:443@evil.example/app.apk"))
        assertFalse(isValidUpdateUrl("https://github.com@evil.example/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://user:pass@play.google.com/store/apps/details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("https://@github.com/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://github.com:8443/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://github.com:443/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://play.google.com:8080/store/apps/details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn:/download/app.apk"))
        assertFalse(isValidUpdateUrl("market://user@details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("market://details:443?id=com.giathinh.uthconnect"))
    }

    @Test
    fun isValidUpdateUrlRejectsSpoofedSubdomainsAndLookalikeDomains() {
        assertFalse(isValidUpdateUrl("https://evil.github.com/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://fake.play.google.com/store/apps/details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("https://evil.ut.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://evil.uth.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://github.com.evil.example/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn.evil.com/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://notuth.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://fake-github.com/ThinhNguyen2005/UTH-Connect/releases"))
    }

    @Test
    fun isValidUpdateUrlRejectsMalformedMissingHostWhitespaceAndControlChars() {
        assertFalse(isValidUpdateUrl("https:///ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://:443/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://not valid/path"))
        assertFalse(isValidUpdateUrl("https:github.com/ThinhNguyen2005/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://github.com\\@evil.example/app.apk"))
        assertFalse(isValidUpdateUrl("https://[::1]/download/app.apk"))
        assertFalse(isValidUpdateUrl(" https://ut.edu.vn/download/app.apk"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download/app.apk "))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download/app.apk\n"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download\t/app.apk"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download/app\u0000.apk"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download/app\u200B.apk"))
    }

    @Test
    fun isValidUpdateUrlRejectsInvalidMarketAndDisallowedPathsQueriesFragments() {
        assertFalse(isValidUpdateUrl("market://"))
        assertFalse(isValidUpdateUrl("market://details"))
        assertFalse(isValidUpdateUrl("market://details?id=com.other.app"))
        assertFalse(isValidUpdateUrl("market://search?q=uth"))
        assertFalse(isValidUpdateUrl("market://details/extra?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("market://details?id=com.giathinh.uthconnect&url=https://evil.example"))
        assertFalse(isValidUpdateUrl("market://details?id=com.giathinh.uthconnect#frag"))
        assertFalse(isValidUpdateUrl("market:details?id=com.giathinh.uthconnect"))
        assertFalse(isValidUpdateUrl("https://github.com/attacker/UTH-Connect/releases"))
        assertFalse(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releasesEvil"))
        assertFalse(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases/../../../evil/releases"))
        assertFalse(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases%2f..%2fevil"))
        assertFalse(
            isValidUpdateUrl(
                "https://github.com/ThinhNguyen2005/UTH-Connect/releases?return_to=https://evil.example",
            ),
        )
        assertFalse(isValidUpdateUrl("https://github.com/ThinhNguyen2005/UTH-Connect/releases#frag"))
        assertFalse(isValidUpdateUrl("https://play.google.com/store/apps/details?id=com.evil.app"))
        assertFalse(
            isValidUpdateUrl(
                "https://play.google.com/store/apps/details?id=com.giathinh.uthconnect&redirect=https://evil.example",
            ),
        )
        assertFalse(isValidUpdateUrl("https://play.google.com/store/apps/details?id=com.giathinh.uthconnect#frag"))
        assertFalse(isValidUpdateUrl("https://play.google.com/store/search?q=uth"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/"))
        assertFalse(isValidUpdateUrl("https://ut.edu.vn/download/app.apk?redirect=https://evil.example"))
        assertFalse(isValidUpdateUrl("https://uth.edu.vn/download/app.apk#frag"))
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
        fake.edit()
            .putBoolean("has_saved_state", true)
            .putString("maintenance_mode", "corrupted_string")
            .putInt("force_update_min_version", 123)
            .putBoolean("update_url", true)
            .apply()
        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
        assertEquals("", loaded.updateUrl)
    }

    @Test
    fun corruptedMinVersionPreservesValidMaintenanceMode() {
        val fake = FakeSharedPreferences()
        val validUrl = "https://play.google.com/store/apps/details?id=com.giathinh.uthconnect"
        fake.edit()
            .putBoolean("has_saved_state", true)
            .putBoolean("maintenance_mode", true)
            .putInt("force_update_min_version", 999)
            .putString("update_url", validUrl)
            .apply()

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertTrue("Maintenance mode preserved even if min_version type is corrupted", loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
        assertEquals(validUrl, loaded.updateUrl)
    }

    @Test
    fun corruptedMaintenanceModePreservesValidMinVersion() {
        val fake = FakeSharedPreferences()
        val validUrl = "https://github.com/ThinhNguyen2005/UTH-Connect/releases"
        fake.edit()
            .putBoolean("has_saved_state", true)
            .putString("maintenance_mode", "invalid_string_type")
            .putString("force_update_min_version", "2.0.0")
            .putString("update_url", validUrl)
            .apply()

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse("Maintenance mode falls back to false on ClassCastException", loaded.maintenanceMode)
        assertEquals("2.0.0", loaded.forceUpdateMinVersion)
        assertEquals(validUrl, loaded.updateUrl)
    }

    @Test
    fun corruptedFieldsPreserveValidUpdateUrl() {
        val fake = FakeSharedPreferences()
        val validUrl = "https://uth.edu.vn/download/app.apk"
        fake.edit()
            .putBoolean("has_saved_state", true)
            .putInt("maintenance_mode", 1)
            .putBoolean("force_update_min_version", true)
            .putString("update_url", validUrl)
            .apply()

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertFalse(loaded.maintenanceMode)
        assertEquals("1.0.0", loaded.forceUpdateMinVersion)
        assertEquals(validUrl, loaded.updateUrl)
    }

    @Test
    fun corruptedUpdateUrlPreservesValidMaintenanceAndMinVersion() {
        val fake = FakeSharedPreferences()
        fake.edit()
            .putBoolean("has_saved_state", true)
            .putBoolean("maintenance_mode", true)
            .putString("force_update_min_version", "2.1.0")
            .putInt("update_url", 404)
            .apply()

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertTrue(loaded.maintenanceMode)
        assertEquals("2.1.0", loaded.forceUpdateMinVersion)
        assertEquals("", loaded.updateUrl)
    }

    @Test
    fun wrongTypedHasSavedStateDoesNotCrashAndPreservesValidFields() {
        val fake = FakeSharedPreferences()
        val validUrl = "https://play.google.com/store/apps/details?id=com.giathinh.uthconnect"
        fake.edit()
            .putString("has_saved_state", "corrupted_boolean")
            .putBoolean("maintenance_mode", true)
            .putString("force_update_min_version", "2.0.0")
            .putString("update_url", validUrl)
            .apply()

        val loaded = loadSavedGate(fake)
        assertTrue(loaded.hasSavedState)
        assertTrue(loaded.maintenanceMode)
        assertEquals("2.0.0", loaded.forceUpdateMinVersion)
        assertEquals(validUrl, loaded.updateUrl)
    }

    @Test
    fun updateRequiredWithBlankUpdateUrlStaysSafeAndDefaultVersionIsAvailable() {
        assertEquals(AppAvailability.AVAILABLE, availability(false, "1.0", "1.0.0"))
        val (state, newSaved) = resolveGate(
            saved = SavedGate(hasSavedState = false),
            fetchSucceeded = true,
            remoteMaintenance = false,
            remoteMinVersion = "2.0.0",
            remoteUpdateUrl = "",
            currentVersion = "1.0",
        )
        assertEquals(AppAvailability.UPDATE_REQUIRED, state)
        assertEquals("", newSaved.updateUrl)
        assertFalse(isValidUpdateUrl(newSaved.updateUrl))
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

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
            val value = if (map.containsKey(key)) map[key] else return defValues
            if (value != null && value !is Set<*>) {
                throw ClassCastException("Value $value for $key is not Set")
            }
            @Suppress("UNCHECKED_CAST")
            return (value as? Set<String>)?.toMutableSet() ?: defValues
        }

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

            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                changes[key] = values?.toSet()
                removedKeys.remove(key)
                return this
            }

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

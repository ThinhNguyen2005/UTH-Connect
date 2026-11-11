package com.giathinh.uthconnect

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        val saved = SavedGate(maintenanceMode = false, forceUpdateMinVersion = "2.0.0", hasSavedState = true)
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
    fun newlyActivatedConfigurationChangesTheAppGate() = runTest {
        var maintenance = false
        val published = mutableListOf<AppAvailability>()
        refreshAvailability(
            read = { availability(maintenance, "1.0", "1.0.0") },
            fetchAndActivate = { maintenance = true },
            publish = { published.add(it) },
        )
        assertEquals(listOf(AppAvailability.AVAILABLE, AppAvailability.MAINTENANCE), published)
    }

    @Test
    fun failedFetchPreservesPreviouslyActivatedGate() = runTest {
        val published = mutableListOf<AppAvailability>()
        try {
            refreshAvailability(
                read = { AppAvailability.MAINTENANCE },
                fetchAndActivate = { throw IllegalStateException("network unavailable") },
                publish = { published.add(it) },
            )
        } catch (_: IllegalStateException) {
            // The caller handles the network failure after the cached state is published.
        }
        assertEquals(listOf(AppAvailability.MAINTENANCE), published)
    }
}

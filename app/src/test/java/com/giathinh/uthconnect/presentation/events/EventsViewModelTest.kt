package com.giathinh.uthconnect.presentation.events

import org.junit.Assert.assertEquals
import org.junit.Test

class EventsViewModelTest {
    @Test
    fun `check-in time accepts UTC timestamps with Z and explicit offset`() {
        assertEquals("07:00 19/11", formatCheckInTime("2026-11-19T00:00:00Z"))
        assertEquals("07:00 19/11", formatCheckInTime("2026-11-19T00:00:00+00:00"))
    }
}

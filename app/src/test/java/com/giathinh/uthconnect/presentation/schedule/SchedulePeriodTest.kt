package com.giathinh.uthconnect.presentation.schedule

import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulePeriodTest {
    @Test
    fun `period changes at noon and six in the evening`() {
        assertEquals(SchedulePeriod.MORNING, SchedulePeriod.fromHour(11))
        assertEquals(SchedulePeriod.AFTERNOON, SchedulePeriod.fromHour(12))
        assertEquals(SchedulePeriod.AFTERNOON, SchedulePeriod.fromHour(17))
        assertEquals(SchedulePeriod.EVENING, SchedulePeriod.fromHour(18))
    }
}

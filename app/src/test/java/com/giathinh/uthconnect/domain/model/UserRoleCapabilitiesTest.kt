package com.giathinh.uthconnect.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleCapabilitiesTest {
    @Test fun `lecturer keeps the organizer capabilities previously mapped by client`() {
        assertTrue(UserRole.LECTURER.hasOrganizerCapabilities)
        assertTrue(UserRole.ORGANIZER.hasOrganizerCapabilities)
        assertFalse(UserRole.STUDENT.hasOrganizerCapabilities)
        assertFalse(UserRole.ADMIN.hasOrganizerCapabilities)
    }
}

package com.giathinh.uthconnect.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers session transitions after an abandoned password recovery. */
class SupabaseSessionTest {
    /** A newly attached session must not retain the previous recovery token. */
    @Test fun `attaching a new session clears an abandoned recovery token`() = runBlocking {
        val client = SupabaseRestClient()
        client.recoveryToken = "old-recovery-token"

        client.attachSession("user-id", "access-token", "refresh-token", 3600)

        assertNull(client.recoveryToken)
        assertEquals("user-id", client.userId)
    }
}

package com.giathinh.uthconnect.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseLoginEmailTest {
    @Test fun `student ID uses the documented university email convention`() {
        assertEquals("2023040123@ut.edu.vn", firebaseLoginEmail(" 2023040123 "))
    }

    @Test fun `explicit email is preserved`() {
        assertEquals("student@uth.edu.vn", firebaseLoginEmail(" student@uth.edu.vn "))
    }
}

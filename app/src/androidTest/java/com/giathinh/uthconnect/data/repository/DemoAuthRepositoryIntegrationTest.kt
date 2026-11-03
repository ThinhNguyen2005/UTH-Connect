package com.giathinh.uthconnect.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Giữ kiểm tra fixture remote; repository SharedPreferences cần Android runtime. */
@RunWith(AndroidJUnit4::class)
class DemoAuthRepositoryIntegrationTest {
    @Test fun demoLoginMatchesSharedProfile() = runBlocking {
        val auth = DemoAuthRepository(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context)
        val user = FakeUserRepository().getUserProfile("demo-uid-001").first()!!
        for (identifier in listOf(user.studentId, user.email.uppercase())) {
            val profile = auth.authenticate(identifier, DemoAuthRepository.DEMO_PASSWORD).getOrThrow()!!
            assertEquals(user.studentId, profile.studentId)
            assertEquals(user.email, profile.email)
            assertEquals(user.displayName, profile.displayName)
            assertEquals(profile, auth.findStudent(user.studentId).getOrThrow())
        }
        assertNull(auth.authenticate(user.email, "wrong").getOrThrow())
        assertNull(auth.authenticate("unknown", DemoAuthRepository.DEMO_PASSWORD).getOrThrow())
    }

}

package com.giathinh.uthconnect.data.session

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Android instrumentation test: checks actual DataStore persistence. */
class DataStoreSessionRepositoryTest {
    @Test fun rememberSessionThenClear() = runBlocking {
        val repository = DataStoreSessionRepository(
            InstrumentationRegistry.getInstrumentation().targetContext,
        )
        try {
            repository.clear()
            assertNull(repository.getRememberedStudentId())
            repository.save("2023040123", remember = true)
            assertEquals("2023040123", repository.getRememberedStudentId())
            repository.save("2023040123", remember = false)
            assertNull(repository.getRememberedStudentId())
        } finally {
            repository.clear()
        }
    }
}

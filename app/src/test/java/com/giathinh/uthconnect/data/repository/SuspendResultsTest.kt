package com.giathinh.uthconnect.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendResultsTest {
    @Test
    fun `ordinary failures are returned`() = runTest {
        val failure = IllegalStateException("failure")

        val result = suspendResult<Unit> { throw failure }

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `cancellation propagates instead of becoming a failure result`() = runTest {
        val cancellation = CancellationException("cancelled")
        var caught: CancellationException? = null

        try {
            suspendResult<Unit> { throw cancellation }
        } catch (error: CancellationException) {
            caught = error
        }

        assertTrue(caught === cancellation)
    }
}

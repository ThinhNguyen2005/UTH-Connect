package com.giathinh.uthconnect.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionMutationGateTest {
    @Test
    fun signInWaitsForInFlightRestoreAndRemainsTheCurrentSession() = runTest {
        val gate = SessionMutationGate()
        val restoreStarted = CompletableDeferred<Unit>()
        val finishRestore = CompletableDeferred<Unit>()
        var currentUser = "user-a"

        val restore = async {
            gate.withLock {
                restoreStarted.complete(Unit)
                finishRestore.await()
                currentUser = "refreshed-user-a"
            }
        }
        restoreStarted.await()

        val signIn = async {
            gate.withLock { currentUser = "user-b" }
        }
        runCurrent()

        assertFalse(signIn.isCompleted)
        finishRestore.complete(Unit)
        restore.await()
        signIn.await()

        assertEquals("user-b", currentUser)
    }
}

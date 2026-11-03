package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.AuthRepository
import com.giathinh.uthconnect.domain.repository.SessionFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesSessionRepositoryTest {
    private val profile = StudentProfile("2023040123", "student@ut.edu.vn", "Student", "Class", "Major", "Faculty", authUid = "uid-a")
    private class Preferences(var grants: SessionGrants = SessionGrants()) : SessionPreferences {
        var fail = false
        override suspend fun read() = grants
        override suspend fun write(grants: SessionGrants) {
            if (fail) error("Disk failure")
            this.grants = grants
        }
    }
    private class Auth(var profile: StudentProfile?) : AuthRepository {
        override val authChanges = MutableSharedFlow<Boolean>()
        val tokenProfiles = MutableSharedFlow<StudentProfile?>()
        override fun observeProfileChanges() = tokenProfiles
        var error: Exception? = null
        var restores = 0
        var signOuts = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun restoreAuthenticatedProfile(): Result<StudentProfile?> {
            restores++
            gate?.await()
            error?.let { throw it }
            return Result.success(profile)
        }
        override suspend fun signOut() { signOuts++; profile = null }
        override suspend fun authenticate(identifier: String, password: String) = Result.success(profile)
        override suspend fun findStudent(studentId: String): Result<StudentProfile?> = error("Must not trust stored student ID")
        override suspend fun changePassword(studentId: String, oldPassword: String, newPassword: String) = Result.success(true)
    }
    @Test fun `token role refresh updates only an already unlocked session`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants("uid-a")))
        repository.restore()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.monitorAuthChanges() }
        val changed = profile.copy(role = com.giathinh.uthconnect.domain.model.UserRole.ADMIN)
        auth.tokenProfiles.emit(changed)
        assertEquals(changed, repository.session.value.profile)
        auth.tokenProfiles.emit(null)
        assertNull(repository.session.value.profile)
    }
    @Test fun `token refresh cannot bypass biometric lock`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants("uid-a", "uid-a")))
        repository.restore()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.monitorAuthChanges() }
        auth.tokenProfiles.emit(profile)
        assertNull(repository.session.value.profile)
        assertEquals("uid-a", repository.session.value.biometricStudentId)
    }
    @Test fun `remembered ID alone never authenticates`() = runTest {
        val preferences = Preferences(SessionGrants("uid-a"))
        val repository = PreferencesSessionRepository(Auth(null), preferences)
        repository.restore()
        assertNull(repository.session.value.profile)
        assertEquals(SessionGrants(), preferences.grants)
    }
    @Test fun `remembered Firebase identity restores verified profile`() = runTest {
        val repository = PreferencesSessionRepository(Auth(profile), Preferences(SessionGrants("uid-a")))
        repository.restore()
        assertEquals(profile, repository.session.value.profile)
        assertFalse(repository.session.value.isRestoring)
    }
    @Test fun `unremembered Firebase credential is signed out on cold start`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences())
        repository.restore()
        assertNull(repository.session.value.profile)
        assertEquals(1, auth.signOuts)
    }
    @Test fun `biometric cold start requires explicit successful unlock`() = runTest {
        val repository = PreferencesSessionRepository(Auth(profile), Preferences(SessionGrants("uid-a", "uid-a")))
        repository.restore()
        assertNull(repository.session.value.profile)
        assertEquals("uid-a", repository.session.value.biometricStudentId)
        repository.unlockWithBiometric()
        assertEquals(profile, repository.session.value.profile)
    }
    @Test fun `biometric unlock refuses a different Firebase account`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants(biometricId = "uid-a")))
        repository.restore()
        auth.profile = profile.copy(authUid = "uid-b")
        try { repository.unlockWithBiometric(); throw AssertionError("Must reject account switch") }
        catch (_: IllegalStateException) { assertNull(repository.session.value.profile) }
    }
    @Test fun `restore failure supports retry in same repository`() = runTest {
        val auth = Auth(profile).apply { error = IllegalStateException("Network") }
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants("uid-a")))
        repository.restore()
        assertEquals(SessionFailure.RestoreFailed, repository.session.value.failure)
        auth.error = null
        repository.restore()
        assertEquals(profile, repository.session.value.profile)
        assertNull(repository.session.value.failure)
    }
    @Test fun `cancelled restore propagates and remains retryable`() = runTest {
        val auth = Auth(profile).apply { error = CancellationException("cancel") }
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants("uid-a")))
        try { repository.restore(); throw AssertionError("Must cancel") } catch (_: CancellationException) { }
        auth.error = null
        repository.restore()
        assertEquals(profile, repository.session.value.profile)
    }
    @Test fun `concurrent restores contact server once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val auth = Auth(profile).apply { this.gate = gate }
        val repository = PreferencesSessionRepository(auth, Preferences(SessionGrants("uid-a")))
        val first = async { repository.restore() }
        val second = async { repository.restore() }
        gate.complete(Unit)
        first.await(); second.await()
        assertEquals(1, auth.restores)
    }
    @Test fun `logout revokes Firebase and both device grants`() = runTest {
        val auth = Auth(profile)
        val prefs = Preferences(SessionGrants("uid-a", "uid-a"))
        val repository = PreferencesSessionRepository(auth, prefs)
        repository.start(profile, true)
        repository.signOut()
        assertNull(repository.session.value.profile)
        assertEquals(SessionGrants(), prefs.grants)
        assertEquals(1, auth.signOuts)
    }
    @Test fun `logout disk failure still revokes Firebase and UI access`() = runTest {
        val auth = Auth(profile)
        val prefs = Preferences()
        val repository = PreferencesSessionRepository(auth, prefs)
        repository.start(profile, true)
        prefs.fail = true
        try { repository.signOut(); throw AssertionError("Must report disk failure") }
        catch (_: IllegalStateException) { assertNull(repository.session.value.profile); assertNull(auth.profile) }
    }
    @Test fun `Firebase sign out event removes active profile`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences())
        repository.start(profile, false)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.monitorAuthChanges() }
        auth.authChanges.emit(false)
        assertNull(repository.session.value.profile)
    }
    @Test fun `refresh does not retain a deleted account`() = runTest {
        val auth = Auth(profile)
        val repository = PreferencesSessionRepository(auth, Preferences())
        repository.start(profile, false)
        auth.profile = null
        repository.refresh()
        assertNull(repository.session.value.profile)
    }
    @Test fun `foreground network or disabled account error closes active session and supports retry`() = runTest {
        for (reason in listOf(com.giathinh.uthconnect.domain.repository.AuthError.Network,
                com.giathinh.uthconnect.domain.repository.AuthError.Disabled,
                com.giathinh.uthconnect.domain.repository.AuthError.InvalidSession)) {
            val auth = Auth(profile)
            val repository = PreferencesSessionRepository(auth, Preferences())
            repository.start(profile, true)
            auth.error = com.giathinh.uthconnect.domain.repository.AuthException(reason)
            repository.refresh()
            assertNull(repository.session.value.profile)
            assertEquals(SessionFailure.RestoreFailed, repository.session.value.failure)
        }
    }
    @Test fun `new session instance restores remembered UID but logout prevents another restart`() = runTest {
        val auth = Auth(profile); val prefs = Preferences()
        PreferencesSessionRepository(auth, prefs).start(profile, true)
        val restarted = PreferencesSessionRepository(auth, prefs)
        restarted.restore()
        assertEquals(profile, restarted.session.value.profile)
        restarted.signOut()
        val afterLogout = PreferencesSessionRepository(auth, prefs)
        afterLogout.restore()
        assertNull(afterLogout.session.value.profile)
        assertEquals(SessionGrants(), prefs.grants)
    }

}

package com.giathinh.uthconnect.presentation.profile

import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.repository.SessionRepository
import com.giathinh.uthconnect.domain.repository.StudentSession
import com.giathinh.uthconnect.domain.usecase.GetProfileUseCase
import com.giathinh.uthconnect.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule val dispatcherRule = MainDispatcherRule()
    private val demo = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
        "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")
    private class FakeSession(initial: StudentProfile) : SessionRepository {
        private val state = MutableStateFlow(StudentSession(isRestoring = false, profile = initial))
        override val session: StateFlow<StudentSession> = state
        override suspend fun restore() = Unit
        override suspend fun start(profile: StudentProfile, remember: Boolean) {
            state.value = StudentSession(profile = profile, remembered = remember)
        }
        override suspend fun signOut() { state.value = StudentSession(isRestoring = false) }
    }

    @Test fun `Hien thong tin dung sinh vien va dang xuat xoa session`() = runTest {
        val fake = FakeSession(demo)
        val vm = ProfileViewModel(GetProfileUseCase(fake), fake)
        advanceUntilIdle()
        assertEquals("2023040123", vm.uiState.value.student?.studentId)
        vm.onAction(ProfileAction.Logout)
        advanceUntilIdle()
        assertNull(vm.uiState.value.student)
    }
}

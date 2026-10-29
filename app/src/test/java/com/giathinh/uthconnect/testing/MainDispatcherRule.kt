package com.giathinh.uthconnect.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Đổi `Dispatchers.Main` sang dispatcher test quanh mỗi test case, vì
 * `viewModelScope` luôn pin vào Main. Không có rule này thì mọi test ViewModel
 * treo hoặc fail.
 *
 * Dùng `UnconfinedTestDispatcher` mặc định để block trong `launch` chạy ngay —
 * test đọc `uiState.value` không cần `advanceUntilIdle()` cho luồng đơn giản.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

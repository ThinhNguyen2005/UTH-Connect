package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.UtilityModuleId

/**
 * Ý định của người dùng trên tab Tiện ích — một chiều vào ViewModel.
 *
 * Đây là chữ ký mà Screen nhận: `onAction: (UtilitiesAction) -> Unit`. Composable
 * không tự suy ra hành vi, chỉ phát action.
 *
 * Dẫn đường giữa các tab **không** nằm trong Action: đó là việc của
 * `presentation/navigation/`. Action chỉ mang ý nghĩa nghiệp vụ trong module.
 */
sealed interface UtilitiesAction {

    /** Người dùng bấm "Thử lại" sau khi tải danh sách thất bại. */
    data object Retry : UtilitiesAction

    /**
     * Mở một module. Chưa xử lý vì graph điều hướng con của từng module sẽ do
     * branch của module đó thêm (`<Module>Navigation.kt` — `docs/GIT_WORKFLOW.md` mục 4).
     */
    data class OpenModule(val moduleId: UtilityModuleId) : UtilitiesAction
}

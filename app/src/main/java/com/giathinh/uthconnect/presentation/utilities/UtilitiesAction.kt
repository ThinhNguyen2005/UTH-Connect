package com.giathinh.uthconnect.presentation.utilities

import com.giathinh.uthconnect.domain.model.ThemeMode
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

    /**
     * Bật/tắt nhắc nộp học phí. Màn Cài đặt là màn con của tab Tiện ích nên ý định
     * của nó đi chung một kênh action, không có hàm public riêng cho từng công tắc.
     */
    data class TuitionReminderChanged(val enabled: Boolean) : UtilitiesAction

    /** Bật/tắt thông báo điểm rèn luyện. */
    data class TrainingPointNotifChanged(val enabled: Boolean) : UtilitiesAction

    /**
     * Chọn ngôn ngữ toàn app (BCP-47). ViewModel ghi store rồi phát
     * `UtilitiesEvent.LanguageChanged`; `UtilitiesRoute` là nơi gọi `recreate()`.
     */
    data class LanguageSelected(val languageTag: String) : UtilitiesAction

    /** Chọn giao diện — áp ngay vì theme là state đang compose, không cần dựng lại activity. */
    data class ThemeModeSelected(val themeMode: ThemeMode) : UtilitiesAction
}

package com.giathinh.uthconnect.domain.usecase

import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import javax.inject.Inject

/**
 * Một UseCase = một nghiệp vụ mà Screen cần, đặt tên theo ngôi thứ ba dạng
 * `Get/Set/...` + `UseCase`.
 *
 * Quy tắc cả nhóm phải giữ (`README.md` mục 5.2): Screen **không** gọi Repository
 * trực tiếp mà đi qua UseCase. ViewModel cũng không tự chứa nghiệp vụ — nó chỉ
 * điều phối UseCase và gói kết quả thành `UiState`.
 *
 * UseCase thuần Kotlin: không `Context`, không `Modifier`, không tài nguyên UI.
 * Annotation duy nhất được phép ở `domain/` là `@Inject` của JSR-330
 * (`javax.inject`) — nó không phải `android.*`/`androidx.*` nên không phá quy tắc
 * tầng; Hilt tự sinh binding từ constructor này, không cần `@Provides`.
 *
 * Lỗi trả về bằng `Result` để UI quyết định cách hiển thị (ViewModel không được
 * cầm `@StringRes`).
 */
class GetUtilityModulesUseCase @Inject constructor(
    private val repository: UtilityModuleRepository,
) {

    /**
     * Chưa có rule lọc nào — toàn bộ module đều trả về kèm `phase` để UI gắn badge.
     * Khi cần chốt rule (ví dụ ẩn module BACKLOG, hoặc chỉ hiện module đã bật),
     * thêm **ở đây**, không thêm trong ViewModel hay Composable.
     */
    suspend operator fun invoke(): Result<List<UtilityModule>> = repository.modules()
}

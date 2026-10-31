package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.domain.model.DeliveryPhase
import com.giathinh.uthconnect.domain.model.UtilityModule
import com.giathinh.uthconnect.domain.model.UtilityModuleId
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Danh sách module khai báo theo đúng tiến độ trong `docs/Design.md` mục 3.
 * Đây là nguồn tĩnh tạm thời cho tới khi có Firestore + repository thật (M0).
 *
 * `@Singleton` vì repository không giữ trạng thái và chỉ cần một instance cho cả
 * app; binding interface → impl nằm trong `data/di/UtilitiesDataModule.kt`.
 */
@Singleton
class StaticUtilityModuleRepository @Inject constructor() : UtilityModuleRepository {

    override suspend fun modules(): Result<List<UtilityModule>> = Result.success(
        listOf(
            UtilityModule(UtilityModuleId.EVENTS, DeliveryPhase.MVP, enabled = false),
            UtilityModule(UtilityModuleId.TIMETABLE, DeliveryPhase.PHASE_2, enabled = false),
            UtilityModule(UtilityModuleId.LOST_FOUND, DeliveryPhase.PHASE_3, enabled = false),
            UtilityModule(UtilityModuleId.BOOKS, DeliveryPhase.BACKLOG, enabled = false),
            UtilityModule(UtilityModuleId.HOUSING, DeliveryPhase.BACKLOG, enabled = false),
            UtilityModule(UtilityModuleId.RIDE_SHARE, DeliveryPhase.BACKLOG, enabled = false),
        ),
    )
}

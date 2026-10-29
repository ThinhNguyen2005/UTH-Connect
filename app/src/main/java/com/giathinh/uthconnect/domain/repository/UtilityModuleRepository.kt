package com.giathinh.uthconnect.domain.repository

import com.giathinh.uthconnect.domain.model.UtilityModule

/**
 * Nguồn dữ liệu danh sách module hiển thị trên tab Tiện ích.
 *
 * Interface nằm ở domain, implementation ở `data/`. UI không gọi thẳng interface
 * này — chỉ UseCase mới được gọi (`README.md` mục 5.2).
 *
 * `suspend` + `Result`: từ đầu M0 nguồn sẽ là Firestore (bất đồng bộ, có lỗi).
 * Khai báo suspend một lần ở đây để về sau không phải sửa mọi tầng.
 */
interface UtilityModuleRepository {
    suspend fun modules(): Result<List<UtilityModule>>
}

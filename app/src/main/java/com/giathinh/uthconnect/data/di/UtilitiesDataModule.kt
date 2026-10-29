package com.giathinh.uthconnect.data.di

import com.giathinh.uthconnect.data.repository.StaticUtilityModuleRepository
import com.giathinh.uthconnect.domain.repository.UtilityModuleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binding của tầng `data` thuộc module Tiện ích: interface ở `domain` → implementation
 * thật ở `data`. Đây là chỗ duy nhất hai tầng này chạm nhau.
 *
 * Vì sao cần module: `UtilityModuleRepository` là interface nên Hilt không tự tạo
 * instance được; `GetUtilityModulesUseCase` có `@Inject constructor` nên Hilt tự
 * sinh binding, không cần khai ở đây.
 *
 * `@Singleton` đặt trên `StaticUtilityModuleRepository`, không lặp lại ở hàm bind —
 * binding @Binds trả chính instance đã được scope đó.
 *
 * **Mỗi module một file** (`<Module>DataModule.kt`, đặt theo quyền sở hữu ở
 * `REPO_TREE.md`). Không gộp các module vào một file DI chung — đó sẽ thành file
 * mà cả 6 người cùng sửa và sinh conflict mỗi lần merge.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class UtilitiesDataModule {

    @Binds
    abstract fun bindUtilityModuleRepository(
        implementation: StaticUtilityModuleRepository,
    ): UtilityModuleRepository
}

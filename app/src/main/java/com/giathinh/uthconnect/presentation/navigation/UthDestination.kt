package com.giathinh.uthconnect.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.giathinh.uthconnect.R
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

// 4 route cấp 1 — @Serializable object, không có tham số. Navigation dựng
// đường dẫn từ tên class, không còn chuỗi route tự do (docs/Design.md mục 8.1).
@Serializable object HomeDestination
@Serializable object ScheduleDestination
@Serializable object UtilitiesDestination
@Serializable object ProfileDestination

/**
 * 4 tab cấp 1 của app — khung Navigation theo `docs/Design.md` mục 8.
 *
 * Mỗi tab ánh xạ một route type-safe ([route]/[routeClass]) với nhãn và icon.
 * Thêm màn hình chi tiết trong một module thì khai báo ở `navigation/` của
 * feature đó (graph riêng + `<Module>Navigation.kt`), không cộng thêm entry
 * vào enum này.
 */
enum class UthDestination(
    /** Thể hiện route để `navigate(...)`/`popUpTo(...)` — instance của @Serializable object. */
    val route: Any,
    /** Class của route, dùng cho `composable<T>()` gián tiếp và so khớp tab hiện tại. */
    val routeClass: KClass<out Any>,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(HomeDestination, HomeDestination::class, R.string.tab_home, Icons.Filled.Home),
    SCHEDULE(
        ScheduleDestination,
        ScheduleDestination::class,
        R.string.tab_schedule,
        Icons.Filled.DateRange,
    ),
    UTILITIES(
        UtilitiesDestination,
        UtilitiesDestination::class,
        R.string.tab_utilities,
        Icons.Filled.Build,
    ),
    PROFILE(
        ProfileDestination,
        ProfileDestination::class,
        R.string.tab_profile,
        Icons.Filled.Person,
    ),
    ;

    companion object {
        val start: UthDestination = HOME

        /**
         * Tab chỉ được xác định bằng destination hiện tại của back stack, không bằng
         * biến state riêng. `hasRoute` so theo @Serializable class — không parse chuỗi.
         */
        fun from(destination: NavDestination?): UthDestination? =
            destination?.let { cur -> entries.firstOrNull { cur.hasRoute(it.routeClass) } }
    }
}

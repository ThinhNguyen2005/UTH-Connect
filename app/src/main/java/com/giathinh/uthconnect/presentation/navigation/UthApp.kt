package com.giathinh.uthconnect.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.presentation.announcement.AnnouncementDetailDestination
import com.giathinh.uthconnect.presentation.announcement.announcementGraph
import com.giathinh.uthconnect.presentation.home.HomeRoute
import com.giathinh.uthconnect.presentation.profile.ProfileScreen
import com.giathinh.uthconnect.presentation.home.HomeScreen
import com.giathinh.uthconnect.presentation.profile.ProfileRoute
import com.giathinh.uthconnect.presentation.auth.AuthRoute
import com.giathinh.uthconnect.presentation.schedule.ScheduleScreen
import com.giathinh.uthconnect.presentation.utilities.UtilitiesRoute

/**
 * Khung điều hướng tổng: [UthBottomBar] + [NavHost] cho 4 tab cấp 1 và các graph con.
 *
 * Trạng thái tab đang chọn lấy từ `currentBackStackEntry`, không nhân bản sang
 * biến riêng — tránh lệch khi quay lại bằng nút hệ thống.
 *
 * `selectedDestination == null` = đang ở màn hình con (ví dụ Chi tiết thông báo): thanh tab bị
 * ẩn, vì màn hình con có thanh trên riêng và không có tab nào đang được chọn.
 *
 * File không còn giữ vai trò DI: trước đây nó cấp `AppContainer` cho cả cây,
 * nay Hilt lo việc đó qua `@AndroidEntryPoint` trên `MainActivity`.
 */
/** Login -> Home -> Profile: chỉ mở 4 tab sau khi xác thực demo. */
@Composable
fun UthApp() {
    AuthRoute(signedInContent = { SignedInUthApp() })
}
@Composable
private fun SignedInUthApp(
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = UthDestination.from(backStackEntry?.destination)

    Scaffold(
        bottomBar = {
            if (selectedDestination != null) {
                UthBottomBar(
                    selected = selectedDestination,
                    onSelect = { destination ->
                        if (destination != selectedDestination) {
                            navController.navigate(destination.route) {
                                // Tab cấp 1: lưu/trả trạng thái riêng từng tab, không xếp chồng bản sao.
                                popUpTo(UthDestination.start.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        UthNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun UthNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = UthDestination.start.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(UthDimens.Motion.RouteFadeMillis)) },
        exitTransition = { fadeOut(tween(UthDimens.Motion.RouteFadeMillis)) },
    ) {
        // Route type-safe: mỗi tab là một @Serializable object (xem UthDestination.kt).
        // Tab nào có state holder thì gọi `<Module>Route()` (nối ViewModel với Screen);
        // ba tab còn lại chưa có dữ liệu nên đang gọi thẳng Screen stateless.
        // Screen không được tự cầm repository (README mục 5.2); ViewModel do Hilt cung cấp.
        composable<HomeDestination> {
            HomeRoute(
                onOpenAnnouncement = { announcementId ->
                    navController.navigate(AnnouncementDetailDestination(announcementId))
                },
            )
        }
        composable<ScheduleDestination> { ScheduleScreen() }
        composable<UtilitiesDestination> { UtilitiesRoute() }
        composable<ProfileDestination> { ProfileScreen() }

        // Graph con của module Thông báo (route Chi tiết). Mỗi module tự khai báo graph của mình;
        // file này chỉ gọi một dòng, không biết chi tiết bên trong.
        announcementGraph(navController)
    }
}

@Composable
private fun UthBottomBar(
    selected: UthDestination?,
    onSelect: (UthDestination) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = UthDimens.Elevation.BottomBar,
    ) {
        UthDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes),
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

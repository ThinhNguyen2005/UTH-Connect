package com.giathinh.uthconnect.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.UtilityModuleId
import com.giathinh.uthconnect.presentation.announcement.AnnouncementDetailDestination
import com.giathinh.uthconnect.presentation.announcement.announcementGraph
import com.giathinh.uthconnect.presentation.attendance.attendanceGraph
import com.giathinh.uthconnect.presentation.auth.AuthRoute
import com.giathinh.uthconnect.presentation.events.EventsDestination
import com.giathinh.uthconnect.presentation.events.eventsGraph
import com.giathinh.uthconnect.presentation.home.HomeRoute
import com.giathinh.uthconnect.presentation.lostfound.LostFoundDestination
import com.giathinh.uthconnect.presentation.lostfound.lostFoundGraph
import com.giathinh.uthconnect.presentation.profile.ProfileRoute
import com.giathinh.uthconnect.presentation.scanner.scannerGraph
import com.giathinh.uthconnect.presentation.schedule.ScheduleRoute
import com.giathinh.uthconnect.presentation.tickets.TicketsDestination
import com.giathinh.uthconnect.presentation.tickets.ticketsGraph
import com.giathinh.uthconnect.presentation.utilities.UtilitiesRoute

/**
 * Cửa vào app: 4 tab chỉ mở sau khi xác thực demo.
 *
 * Phần đăng nhập nằm trong [AuthRoute] và phần đã đăng nhập nằm trong [SignedInUthApp], nên
 * khung điều hướng không phải biết gì về form đăng nhập, và ngược lại.
 */
@Composable
fun UthApp() {
    AuthRoute(signedInContent = { SignedInUthApp() })
}

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
        composable<ScheduleDestination> { ScheduleRoute() }
        composable<UtilitiesDestination> {
            UtilitiesRoute(
                onOpenModule = { module ->
                    when (module) {
                        UtilityModuleId.LOST_FOUND -> navController.navigate(LostFoundDestination)

                        UtilityModuleId.EVENTS -> navController.navigate(EventsDestination)

                        // Module chưa có graph riêng thì ở lại lưới Tiện ích với badge giai đoạn;
                        // graph của nó do branch feature của module đó thêm (docs/GIT_WORKFLOW.md mục 4).
                        else -> Unit
                    }
                },
            )
        }
        composable<ProfileDestination> {
            // Vé chuyển từ tab đáy vào tab Cá nhân (docs/Design.md mục 8.2) để thanh nav giữ 4 điểm đến.
            ProfileRoute(onOpenTickets = { navController.navigate(TicketsDestination) })
        }
        attendanceGraph(navController)
        ticketsGraph(navController)
        scannerGraph(navController)
        eventsGraph(navController)

        // Graph con của module Thông báo (route Chi tiết). Mỗi module tự khai báo graph của mình;
        // file này chỉ gọi một dòng, không biết chi tiết bên trong.
        announcementGraph(navController)
        lostFoundGraph(navController)
    }
}

@Composable
private fun UthBottomBar(
    selected: UthDestination?,
    onSelect: (UthDestination) -> Unit,
) {
    val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale
    val destinations = UthDestination.entries
    val scrollState = rememberScrollState()
    NavigationBar(
        modifier = if (largeText) {
            Modifier
                .horizontalScroll(scrollState)
                .width(UthDimens.Layout.LargeTextNavigationItemWidth * destinations.size)
        } else {
            Modifier
        },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = UthDimens.Elevation.BottomBar,
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null, // NavigationBarItem đã có label cho TalkBack.
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

package com.giathinh.uthconnect.presentation.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat

/**
 * Tầng kết nối Composable Route cho màn hình Thông Báo:
 * Lấy ViewModel từ Hilt và hứng các sự kiện chuyển màn hình (Navigation Events).
 */
@Composable
fun NotificationRoute(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDetail: (String) -> Unit,
    notificationId: String? = null,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(notificationId) {
        notificationId?.let { viewModel.onAction(NotificationAction.LoadDetail(it)) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NotificationNavigationEvent.Back -> onBack()
                is NotificationNavigationEvent.OpenSettings -> onOpenSettings()
                is NotificationNavigationEvent.OpenProfile -> onOpenProfile()
                is NotificationNavigationEvent.OpenDetail -> onOpenDetail(event.notificationId)
            }
        }
    }

    NotificationScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

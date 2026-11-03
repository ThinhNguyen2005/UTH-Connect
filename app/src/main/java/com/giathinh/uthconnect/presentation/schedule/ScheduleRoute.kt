package com.giathinh.uthconnect.presentation.schedule

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScheduleRoute(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exactAlarmPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onAction(ScheduleAction.RefreshReminders)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshOrRequestExactAlarm(context, viewModel) { intent -> exactAlarmPermission.launch(intent) }
    }
    ScheduleScreen(
        uiState,
        viewModel::onAction,
        modifier,
        onEnableReminders = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                refreshOrRequestExactAlarm(context, viewModel) { intent -> exactAlarmPermission.launch(intent) }
            }
        },
    )
}

private fun refreshOrRequestExactAlarm(
    context: android.content.Context,
    viewModel: ScheduleViewModel,
    exactAlarmPermission: (Intent) -> Unit,
) {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        exactAlarmPermission(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
    } else viewModel.onAction(ScheduleAction.RefreshReminders)
}

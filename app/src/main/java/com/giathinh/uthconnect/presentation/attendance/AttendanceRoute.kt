package com.giathinh.uthconnect.presentation.attendance

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.usecase.AttendanceCsvLabels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun AttendanceRoute(
    entry: NavBackStackEntry,
    navController: NavHostController,
    viewModel: AttendanceViewModel = hiltViewModel(),
) {
    val eventId = entry.toRoute<AttendanceDestination>().eventId
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val labels = AttendanceCsvLabels(
        event = stringResource(R.string.attendance_event),
        exportedAt = stringResource(R.string.attendance_exported_at),
        studentId = stringResource(R.string.attendance_student_id),
        name = stringResource(R.string.attendance_name),
        email = stringResource(R.string.attendance_email),
        status = stringResource(R.string.attendance_status),
        checkedInAt = stringResource(R.string.attendance_checked_in_at),
        checkedInBy = stringResource(R.string.attendance_checked_in_by),
        present = stringResource(R.string.attendance_present),
        missing = stringResource(R.string.attendance_missing),
    )
    val createCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) {
            exporting = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val csv = viewModel.createExport(labels, Instant.now()).getOrThrow()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        output.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open output")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                viewModel.showExportFailure(error)
            } finally {
                exporting = false
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(eventId) }
    AttendanceScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = { navController.navigateUp() },
        exporting = exporting,
        onExport = {
            if (!exporting) {
                exporting = true
                createCsv.launch("attendance-$eventId.csv")
            }
        },
    )
}

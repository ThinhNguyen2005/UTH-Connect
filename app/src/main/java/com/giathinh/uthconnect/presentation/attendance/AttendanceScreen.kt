package com.giathinh.uthconnect.presentation.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.AttendanceEntry
import com.giathinh.uthconnect.domain.model.AttendanceRoster
import com.giathinh.uthconnect.presentation.common.messageText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val attendanceTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    state: AttendanceUiState,
    onAction: (AttendanceAction) -> Unit,
    onBack: () -> Unit,
    onExport: () -> Unit,
    exporting: Boolean = false,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.attendance_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.nav_back)) } },
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            state.roster?.let { roster ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                        Text(roster.eventTitle, style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(
                                R.string.attendance_summary,
                                roster.checkedInCount,
                                roster.registeredCount,
                                roster.notCheckedInCount,
                            ),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                            OutlinedButton(onClick = { onAction(AttendanceAction.Refresh) }) {
                                Text(stringResource(R.string.attendance_refresh))
                            }
                            Button(onClick = onExport, enabled = !state.loading && !exporting) {
                                Text(
                                    stringResource(
                                        if (exporting) R.string.attendance_exporting else R.string.attendance_export,
                                    ),
                                )
                            }
                        }
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { onAction(AttendanceAction.Search(it)) },
                            label = { Text(stringResource(R.string.attendance_search)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                            AttendanceFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = state.filter == filter,
                                    onClick = { onAction(AttendanceAction.Filter(filter)) },
                                    label = {
                                        Text(
                                            stringResource(
                                                when (filter) {
                                                    AttendanceFilter.ALL -> R.string.attendance_all
                                                    AttendanceFilter.PRESENT -> R.string.attendance_present
                                                    AttendanceFilter.MISSING -> R.string.attendance_missing
                                                },
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (state.loading) item { CircularProgressIndicator() }
            state.failure?.let { failure ->
                item {
                    val message = when (failure) {
                        AttendanceFailure.ExportFailed -> stringResource(R.string.attendance_export_error)

                        AttendanceFailure.LoadFailed -> state.errorCode?.messageText(state.errorDetail)
                            ?: stringResource(R.string.attendance_load_error)
                    }
                    Text(message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { onAction(AttendanceAction.Refresh) }) {
                        Text(stringResource(R.string.attendance_retry))
                    }
                }
            }
            if (!state.loading && state.failure == null && state.roster?.entries?.isEmpty() == true) {
                item { Text(stringResource(R.string.attendance_empty)) }
            }
            if (!state.loading && state.roster?.entries?.isNotEmpty() == true && state.visibleEntries.isEmpty()) {
                item { Text(stringResource(R.string.attendance_no_match)) }
            }
            items(state.visibleEntries, key = { it.userId }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(UthDimens.Spacing.Lg),
                        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                    ) {
                        Text(
                            entry.displayName.ifBlank { entry.studentId },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(stringResource(R.string.attendance_student_id_value, entry.studentId))
                        Text(
                            stringResource(
                                if (entry.checkedIn) R.string.attendance_present else R.string.attendance_missing,
                            ),
                            color = if (entry.checkedIn) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        entry.checkedInAt?.let { timestamp ->
                            Text(
                                stringResource(
                                    R.string.attendance_time_value,
                                    attendanceTime.format(Instant.parse(timestamp)),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttendancePreview() = UthPreview {
    AttendanceScreen(
        state = AttendanceUiState(
            loading = false,
            roster = AttendanceRoster(
                eventId = "preview",
                eventTitle = "Hội thao UTH",
                entries = listOf(
                    AttendanceEntry("1", "20230001", "Nguyễn Minh Anh", "", "2026-11-03T08:00:00Z", "organizer"),
                    AttendanceEntry("2", "20230002", "Trần Gia Bảo", "", null, null),
                ),
            ),
        ),
        onAction = {},
        onBack = {},
        onExport = {},
    )
}

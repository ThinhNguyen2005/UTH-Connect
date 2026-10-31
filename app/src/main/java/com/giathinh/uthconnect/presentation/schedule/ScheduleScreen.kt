package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.ClassSession
import com.giathinh.uthconnect.domain.model.Timetable
import java.time.LocalDateTime
import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Deadline

import java.time.format.DateTimeFormatter

@Composable
fun ScheduleScreen(uiState: ScheduleUiState, onAction: (ScheduleAction) -> Unit, modifier: Modifier = Modifier) {
    val urgent = uiState.urgentDeadlines
    val later = uiState.laterDeadlines
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        Text(stringResource(R.string.timetable_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.timetable_memory), style = MaterialTheme.typography.bodySmall)
        Button(onClick = { onAction(ScheduleAction.OpenForm) }) { Text(stringResource(R.string.timetable_add)) }
        Text(stringResource(R.string.timetable_tomorrow), style = MaterialTheme.typography.titleLarge)
        val sessions = uiState.tomorrowClasses
        if (sessions.isEmpty()) Text(stringResource(R.string.timetable_empty_classes))
        sessions.forEach { session ->
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                Text(courseName(session.course), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.timetable_class_detail,
                    session.startsAt.format(DateTimeFormatter.ofPattern("HH:mm")),
                    session.endsAt.format(DateTimeFormatter.ofPattern("HH:mm")), session.room))
            }
        }
        Text(stringResource(R.string.timetable_urgent), style = MaterialTheme.typography.titleLarge)
        if (urgent.isEmpty()) Text(stringResource(R.string.timetable_empty_deadlines))
        urgent.forEach { DeadlineRow(it, uiState, onAction) }
        Text(stringResource(R.string.timetable_later), style = MaterialTheme.typography.titleLarge)
        later.forEach { DeadlineRow(it, uiState, onAction) }
        Text(stringResource(R.string.timetable_completed, uiState.timetable.deadlines.count { it.completed }))
    }
    if (uiState.showForm) DeadlineForm(uiState, onAction)
}

@Composable
private fun DeadlineRow(item: Deadline, state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Text(item.title.ifBlank { stringResource(R.string.timetable_assignment, courseName(item.course)) },
            style = MaterialTheme.typography.titleMedium)
        Text(courseName(item.course))
        Text(stringResource(R.string.timetable_due, item.dueAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))))
        if (item.dueAt.isBefore(state.now)) Text(stringResource(R.string.timetable_overdue), color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { onAction(ScheduleAction.Complete(item.id)) }) { Text(stringResource(R.string.timetable_complete)) }
    }
}

@Composable
private fun DeadlineForm(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ScheduleAction.CloseForm) },
        title = { Text(stringResource(R.string.timetable_add)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                Course.entries.forEach { course ->
                    FilterChip(selected = state.course == course, onClick = { onAction(ScheduleAction.SelectCourse(course)) },
                        label = { Text(courseName(course)) })
                }
                OutlinedTextField(state.title, { onAction(ScheduleAction.Title(it)) }, label = { Text(stringResource(R.string.timetable_label_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.date, { onAction(ScheduleAction.Date(it)) }, label = { Text(stringResource(R.string.timetable_label_date)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(state.time, { onAction(ScheduleAction.Time(it)) }, label = { Text(stringResource(R.string.timetable_label_time)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (state.invalidForm) Text(stringResource(R.string.timetable_invalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { onAction(ScheduleAction.Save) }) { Text(stringResource(R.string.timetable_save)) } },
        dismissButton = { TextButton(onClick = { onAction(ScheduleAction.CloseForm) }) { Text(stringResource(R.string.timetable_cancel)) } },
    )
}

@Composable
private fun courseName(course: Course): String = stringResource(when (course) {
    Course.ANDROID -> R.string.timetable_android
    Course.DATABASE -> R.string.timetable_database
    Course.ENGLISH -> R.string.timetable_english
})

@Preview(showBackground = true)
@Composable
private fun LightPreview() = UthPreview { ScheduleScreen(previewState(), {}) }
@Preview(showBackground = true)
@Composable
private fun DarkPreview() = UthPreview(darkTheme = true) { ScheduleScreen(previewState(), {}) }

private fun previewState(): ScheduleUiState {
    val now = LocalDateTime.of(2026, 10, 31, 12, 0)
    return ScheduleUiState(
        now = now,
        timetable = Timetable(
            sessions = listOf(ClassSession(
                "preview-class", Course.ANDROID, now.plusDays(1).withHour(7),
                now.plusDays(1).withHour(9), "B101",
            )),
            deadlines = listOf(Deadline("preview-deadline", Course.ANDROID, "", now.plusHours(6))),
        ),
    )
}

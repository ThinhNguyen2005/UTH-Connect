package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.ClassSession
import com.giathinh.uthconnect.domain.model.Course
import com.giathinh.uthconnect.domain.model.Deadline
import com.giathinh.uthconnect.domain.model.Timetable
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val urgent = uiState.urgentDeadlines
    val later = uiState.laterDeadlines
    val dateFormatter = dateFormatter()
    Box(modifier = modifier.fillMaxSize()) {
      Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Xl)
            .padding(bottom = UthDimens.Spacing.Xxl),
      ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        ScheduleHeader(
            title = stringResource(R.string.timetable_title),
            subtitle = stringResource(R.string.timetable_memory),
            date = uiState.now.toLocalDate().plusDays(1).format(dateFormatter),
        )

        if (uiState.isLoading) {
            UthCard {
                Row(
                    modifier = Modifier.padding(vertical = UthDimens.Spacing.Md),
                    horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(UthDimens.Size.Icon))
                    Text(
                        text = stringResource(R.string.timetable_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
        UthSectionHeader(title = stringResource(R.string.timetable_tomorrow))
        val sessions = uiState.tomorrowClasses
        if (sessions.isEmpty()) {
            UthCard {
                UthEmptyState(
                    title = stringResource(R.string.timetable_empty_classes),
                    body = stringResource(R.string.timetable_memory),
                    icon = Icons.Filled.DateRange,
                    modifier = Modifier.padding(vertical = UthDimens.Spacing.Md),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
                sessions.forEach { session ->
                    ClassSessionCard(session)
                }
            }
        }

        UthSectionHeader(title = stringResource(R.string.timetable_urgent))
        if (urgent.isEmpty()) {
            UthCard {
                Row(
                    modifier = Modifier.padding(vertical = UthDimens.Spacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(UthDimens.Size.Icon),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Md))
                    Text(
                        text = stringResource(R.string.timetable_empty_deadlines),
                        style = MaterialTheme.typography.bodyMedium,
                        color = UthTheme.extendedColors.textSecondary,
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
                urgent.forEach { deadline ->
                    DeadlineCard(deadline, uiState, onAction)
                }
            }
        }

        if (later.isNotEmpty()) {
            UthSectionHeader(title = stringResource(R.string.timetable_later))
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
                later.forEach { deadline ->
                    DeadlineCard(deadline, uiState, onAction)
                }
            }
        }
        }

        if (!uiState.isLoading) Text(
            text = stringResource(
                R.string.timetable_completed,
                uiState.timetable.deadlines.count { it.completed },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = UthDimens.Spacing.Md),
        )
      }
      FloatingActionButton(
          onClick = { onAction(ScheduleAction.OpenForm) },
          modifier = Modifier.align(Alignment.BottomEnd).padding(UthDimens.Spacing.Lg),
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          shape = FloatingActionButtonDefaults.largeShape,
      ) {
          Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.timetable_add_content_description))
      }
    }

    if (uiState.showForm) {
        DeadlineForm(uiState, onAction)
    }
}

@Composable
private fun ScheduleHeader(title: String, subtitle: String, date: String) {
    UthCard(contentPadding = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
    }
}

@Composable
private fun ClassSessionCard(session: ClassSession) {
    UthCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = session.startsAt.format(timeFormatter()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = session.endsAt.format(timeFormatter()),
                    style = MaterialTheme.typography.labelSmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Lg))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                Text(courseName(session.course), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = UthTheme.extendedColors.textSecondary,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    Text(
                        text = stringResource(R.string.timetable_room, session.room),
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeadlineCard(
    item: Deadline,
    state: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
) {
    val overdue = item.dueAt.isBefore(state.now)
    val urgent = !overdue && !item.dueAt.isAfter(state.now.plusHours(48))
    UthCard {
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
                ) {
                    Text(
                        text = item.title.ifBlank {
                            stringResource(R.string.timetable_assignment, courseName(item.course))
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = courseName(item.course),
                        style = MaterialTheme.typography.bodySmall,
                        color = UthTheme.extendedColors.textSecondary,
                    )
                }
                UthStatusBadge(
                    text = stringResource(
                        when {
                            overdue -> R.string.timetable_overdue
                            urgent -> R.string.timetable_urgent_badge
                            else -> R.string.timetable_upcoming_badge
                        },
                    ),
                    tone = when {
                        overdue -> BadgeTone.Danger
                        urgent -> BadgeTone.Warning
                        else -> BadgeTone.Info
                    },
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
                Text(
                    text = stringResource(
                        R.string.timetable_due,
                        item.dueAt.format(deadlineDateTimeFormatter()),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.error else UthTheme.extendedColors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onAction(ScheduleAction.Complete(item.id)) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    Text(stringResource(R.string.timetable_complete))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DeadlineForm(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDate = runCatching { LocalDate.parse(state.date) }
        .getOrElse { state.now.toLocalDate().plusDays(1) }
    val selectedTime = runCatching {
        java.time.LocalTime.parse(state.time)
    }.getOrElse { java.time.LocalTime.of(23, 59) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onAction(ScheduleAction.Date(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()))
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.timetable_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.timetable_cancel))
                }
            },
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.timetable_label_time)) },
            confirmButton = {
                TextButton(onClick = {
                    onAction(ScheduleAction.Time("%02d:%02d".format(timePickerState.hour, timePickerState.minute)))
                    showTimePicker = false
                }) { Text(stringResource(R.string.timetable_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.timetable_cancel))
                }
            },
        ) { TimePicker(state = timePickerState) }
    }

    ModalBottomSheet(
        onDismissRequest = { onAction(ScheduleAction.CloseForm) },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl)
                .padding(bottom = UthDimens.Spacing.Xl),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
        ) {
            Text(
                text = stringResource(R.string.timetable_add),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.timetable_form_hint),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = stringResource(R.string.timetable_label_course),
                style = MaterialTheme.typography.labelLarge,
            )
            Course.entries.forEach { course ->
                FilterChip(
                    selected = state.course == course,
                    onClick = { onAction(ScheduleAction.SelectCourse(course)) },
                    label = { Text(courseName(course)) },
                )
            }
            OutlinedTextField(
                value = state.title,
                onValueChange = { onAction(ScheduleAction.Title(it)) },
                label = { Text(stringResource(R.string.timetable_label_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.date,
                onValueChange = {},
                label = { Text(stringResource(R.string.timetable_label_date)) },
                leadingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.timetable_label_date),
                        )
                    }
                },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
            )
            OutlinedTextField(
                value = state.time,
                onValueChange = {},
                label = { Text(stringResource(R.string.timetable_label_time)) },
                leadingIcon = {
                    IconButton(onClick = { showTimePicker = true }) {
                        Icon(
                            Icons.Filled.DateRange,
                            contentDescription = stringResource(R.string.timetable_label_time),
                        )
                    }
                },
                readOnly = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().clickable { showTimePicker = true },
            )
            if (state.invalidForm) {
                Text(
                    text = stringResource(R.string.timetable_invalid),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                TextButton(
                    onClick = { onAction(ScheduleAction.CloseForm) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.timetable_cancel))
                }
                Button(
                    onClick = { onAction(ScheduleAction.Save) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(stringResource(R.string.timetable_save))
                }
            }
        }
    }
}
@Composable
private fun courseName(course: Course): String = stringResource(
    when (course) {
        Course.ANDROID -> R.string.timetable_android
        Course.DATABASE -> R.string.timetable_database
        Course.ENGLISH -> R.string.timetable_english
    },
)

@Composable
private fun dateFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

private fun deadlineDateTimeFormatter(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMMM · HH:mm", Locale.getDefault())

private fun timeFormatter(): DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
        isLoading = false,
        timetable = Timetable(
            sessions = listOf(
                ClassSession(
                    "preview-class",
                    Course.ANDROID,
                    now.plusDays(1).withHour(7),
                    now.plusDays(1).withHour(9),
                    "B101",
                ),
            ),
            deadlines = listOf(
                Deadline("preview-deadline", Course.ANDROID, "", now.plusHours(6)),
            ),
        ),
    )
}

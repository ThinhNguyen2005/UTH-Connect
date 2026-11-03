package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.model.CourseSchedule
import com.giathinh.uthconnect.domain.model.Deadline
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

@Composable
fun ScheduleScreen(
    uiState: ScheduleUiState,
    onAction: (ScheduleAction) -> Unit,
    modifier: Modifier = Modifier,
    onEnableReminders: () -> Unit = {},
) {
    var showScheduleForm by rememberSaveable { mutableStateOf(false) }
    var showDeadlineForm by rememberSaveable { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<CourseSchedule?>(null) }
    var editingDeadline by remember { mutableStateOf<Deadline?>(null) }
    var pendingDelete by remember { mutableStateOf<(() -> Unit)?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        item {
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            TimetableHeader(
                schedules = uiState.schedules.size,
                deadlines = uiState.deadlines.size,
                onEnableReminders = onEnableReminders,
            )
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = uiState.selectedTab == ScheduleTab.Classes,
                    onClick = { onAction(ScheduleAction.ShowClasses) },
                    label = {
                        Text("${stringResource(R.string.timetable_classes)}  ·  ${uiState.schedules.size}")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = uiState.selectedTab == ScheduleTab.Deadlines,
                    onClick = { onAction(ScheduleAction.ShowDeadlines) },
                    label = {
                        Text("${stringResource(R.string.timetable_deadlines)}  ·  ${uiState.deadlines.size}")
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        if (uiState.failure == ScheduleFailure.SaveFailed) {
            item {
                Text(
                    stringResource(R.string.timetable_save_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = UthDimens.Spacing.Sm),
                )
            }
        }

        if (uiState.isLoading) {
            item { Text(stringResource(R.string.timetable_loading), modifier = Modifier.padding(vertical = UthDimens.Spacing.Xl)) }
        } else if (uiState.failure == ScheduleFailure.LoadFailed) {
            item {
                UthEmptyState(
                    title = stringResource(R.string.timetable_load_error_title),
                    body = stringResource(R.string.timetable_load_error_body),
                    icon = Icons.Filled.Warning,
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(ScheduleAction.Retry) },
                )
            }
        } else if (uiState.selectedTab == ScheduleTab.Classes) {
            item {
                ScheduleControls(uiState, onAction)
                UthPrimaryButton(
                    text = stringResource(R.string.timetable_add_class),
                    onClick = { showScheduleForm = true },
                    modifier = Modifier.fillMaxWidth().padding(top = UthDimens.Spacing.Sm),
                    leadingIcon = Icons.Filled.Add,
                )
            }
            val week = selectedWeek(uiState.selectedDate)
            if (uiState.isWeekView) {
                items(week, key = { it.toEpochDay() }) { date ->
                    ScheduleDay(
                        date = date,
                        schedules = uiState.schedules.filter { it.dayOfWeek == date.dayOfWeek.value },
                        onEdit = { editingSchedule = it; showScheduleForm = true },
                        onDelete = { schedule -> pendingDelete = { onAction(ScheduleAction.DeleteSchedule(schedule.id)) } },
                    )
                }
            } else {
                val schedules = uiState.schedules.filter { it.dayOfWeek == uiState.selectedDate.dayOfWeek.value }
                item {
                    Text(
                        uiState.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = UthDimens.Spacing.Md),
                    )
                    if (schedules.isEmpty()) {
                        UthEmptyState(
                            title = stringResource(R.string.timetable_empty_classes_title),
                            body = stringResource(R.string.timetable_empty_classes_body),
                        )
                    }
                }
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onEdit = { editingSchedule = schedule; showScheduleForm = true },
                        onDelete = { pendingDelete = { onAction(ScheduleAction.DeleteSchedule(schedule.id)) } },
                    )
                }
            }
        } else {
            item {
                UthPrimaryButton(
                    text = stringResource(R.string.timetable_add_deadline),
                    onClick = { showDeadlineForm = true },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = Icons.Filled.Add,
                )
                if (uiState.deadlines.isEmpty()) {
                    UthEmptyState(
                        title = stringResource(R.string.timetable_empty_deadlines_title),
                        body = stringResource(R.string.timetable_empty_deadlines_body),
                    )
                }
            }
            items(uiState.deadlines, key = { it.id }) { deadline ->
                DeadlineCard(
                    deadline = deadline,
                    onEdit = { editingDeadline = deadline; showDeadlineForm = true },
                    onDelete = { pendingDelete = { onAction(ScheduleAction.DeleteDeadline(deadline.id)) } },
                )
            }
        }
        item { Spacer(Modifier.height(UthDimens.Spacing.Xl)) }
    }

    if (showScheduleForm) {
        ScheduleFormDialog(
            initial = editingSchedule,
            onDismiss = { showScheduleForm = false; editingSchedule = null },
            onSave = { schedule ->
                onAction(ScheduleAction.SaveSchedule(schedule))
                showScheduleForm = false
                editingSchedule = null
            },
        )
    }
    if (showDeadlineForm) {
        DeadlineFormDialog(
            initial = editingDeadline,
            onDismiss = { showDeadlineForm = false; editingDeadline = null },
            onSave = { deadline ->
                onAction(ScheduleAction.SaveDeadline(deadline))
                showDeadlineForm = false
                editingDeadline = null
            },
        )
    }
    pendingDelete?.let { delete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.timetable_confirm_delete_title)) },
            text = { Text(stringResource(R.string.timetable_confirm_delete_body)) },
            confirmButton = {
                TextButton(onClick = { delete(); pendingDelete = null }) {
                    Text(stringResource(R.string.timetable_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.timetable_cancel))
                }
            },
        )
    }
}

@Composable
private fun ScheduleControls(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    val week = selectedWeek(state.selectedDate)
    UthCard {
        Text(
            stringResource(R.string.timetable_week_range, week.first().format(dateFormat), week.last().format(dateFormat)),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = UthDimens.Spacing.Xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onAction(ScheduleAction.PreviousWeek) }) {
                Text(stringResource(R.string.timetable_previous))
            }
            TextButton(onClick = { onAction(ScheduleAction.Today) }) {
                Text(stringResource(R.string.timetable_today))
            }
            TextButton(onClick = { onAction(ScheduleAction.NextWeek) }) {
                Text(stringResource(R.string.timetable_next))
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
            items(week, key = { it.toEpochDay() }) { date ->
                FilterChip(
                    selected = date == state.selectedDate,
                    onClick = { onAction(ScheduleAction.SelectDate(date)) },
                    label = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                            Text(date.format(DateTimeFormatter.ofPattern("dd/MM")))
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            FilterChip(
                selected = !state.isWeekView,
                onClick = { onAction(ScheduleAction.SetWeekView(false)) },
                label = { Text(stringResource(R.string.timetable_day_view)) },
            )
            FilterChip(
                selected = state.isWeekView,
                onClick = { onAction(ScheduleAction.SetWeekView(true)) },
                label = { Text(stringResource(R.string.timetable_week_view)) },
            )
        }
    }
}

@Composable
private fun TimetableHeader(schedules: Int, deadlines: Int, onEnableReminders: () -> Unit) {
    Surface(
        shape = UthDimens.Radius.LgShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(Modifier.fillMaxWidth().padding(UthDimens.Spacing.Lg)) {
            Text(stringResource(R.string.timetable_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.timetable_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = UthDimens.Spacing.Xs),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = UthDimens.Spacing.Md),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                HeaderCount(schedules, stringResource(R.string.timetable_classes))
                HeaderCount(deadlines, stringResource(R.string.timetable_deadlines))
            }
            Button(
                onClick = onEnableReminders,
                modifier = Modifier.fillMaxWidth().padding(top = UthDimens.Spacing.Sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
            ) {
                Text(stringResource(R.string.timetable_enable_reminders), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HeaderCount(count: Int, label: String) {
    Surface(
        shape = UthDimens.Radius.MdShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleSmall)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ScheduleDay(
    date: LocalDate,
    schedules: List<CourseSchedule>,
    onEdit: (CourseSchedule) -> Unit,
    onDelete: (CourseSchedule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = UthDimens.Spacing.Md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.getDefault())),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = UthDimens.Radius.MdShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(
                    schedules.size.toString(),
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (schedules.isEmpty()) {
            Text(
                stringResource(R.string.timetable_no_class),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
        schedules.forEach { schedule ->
            ScheduleCard(schedule, { onEdit(schedule) }, { onDelete(schedule) })
        }
    }
}

@Composable
private fun ScheduleCard(schedule: CourseSchedule, onEdit: () -> Unit, onDelete: () -> Unit) {
    UthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.timetable_period_format, schedule.startPeriod, schedule.endPeriod),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "${schedule.startTime} – ${schedule.endTime}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.timetable_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.timetable_delete))
            }
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(schedule.courseName, style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.timetable_course_code_format, schedule.courseCode, schedule.credits),
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Text(stringResource(R.string.timetable_room_campus_format, schedule.room, schedule.campus))
        if (schedule.lecturer.isNotBlank()) {
            Text(
                stringResource(R.string.timetable_lecturer_format, schedule.lecturer),
                style = MaterialTheme.typography.bodySmall,
                color = UthTheme.extendedColors.textSecondary,
            )
        }
    }
}

@Composable
private fun DeadlineCard(deadline: Deadline, onEdit: () -> Unit, onDelete: () -> Unit) {
    val overdue = deadline.dueDate.atTime(deadline.dueTime).isBefore(LocalDateTime.now())
    UthCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(deadline.title, style = MaterialTheme.typography.titleLarge)
                Surface(
                    modifier = Modifier.padding(top = UthDimens.Spacing.Sm),
                    shape = UthDimens.Radius.MdShape,
                    color = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (overdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        stringResource(
                            R.string.timetable_due_date_time_format,
                            deadline.dueDate.format(dateFormat),
                            deadline.dueTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        ),
                        modifier = Modifier.padding(horizontal = UthDimens.Spacing.Sm, vertical = UthDimens.Spacing.Xs),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.timetable_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.timetable_delete))
            }
        }
        if (deadline.courseName.isNotBlank()) {
            Text(deadline.courseName, modifier = Modifier.padding(top = UthDimens.Spacing.Sm))
        }
        if (deadline.note.isNotBlank()) {
            Text(deadline.note, color = UthTheme.extendedColors.textSecondary)
        }
    }
}

@Composable
private fun ScheduleFormDialog(
    initial: CourseSchedule?,
    onDismiss: () -> Unit,
    onSave: (CourseSchedule) -> Unit,
) {
    var courseName by rememberSaveable(initial?.id) { mutableStateOf(initial?.courseName.orEmpty()) }
    var courseCode by rememberSaveable(initial?.id) { mutableStateOf(initial?.courseCode.orEmpty()) }
    var lecturer by rememberSaveable(initial?.id) { mutableStateOf(initial?.lecturer.orEmpty()) }
    var room by rememberSaveable(initial?.id) { mutableStateOf(initial?.room.orEmpty()) }
    var campus by rememberSaveable(initial?.id) { mutableStateOf(initial?.campus.orEmpty()) }
    var day by rememberSaveable(initial?.id) { mutableIntStateOf(initial?.dayOfWeek ?: LocalDate.now().dayOfWeek.value) }
    var startPeriod by rememberSaveable(initial?.id) { mutableStateOf(initial?.startPeriod?.toString().orEmpty()) }
    var endPeriod by rememberSaveable(initial?.id) { mutableStateOf(initial?.endPeriod?.toString().orEmpty()) }
    var startTime by rememberSaveable(initial?.id) { mutableStateOf(initial?.startTime.orEmpty()) }
    var endTime by rememberSaveable(initial?.id) { mutableStateOf(initial?.endTime.orEmpty()) }
    var credits by rememberSaveable(initial?.id) { mutableStateOf(initial?.credits?.toString() ?: "3") }
    val start = startPeriod.toIntOrNull()
    val end = endPeriod.toIntOrNull()
    val creditCount = credits.toIntOrNull()
    val periodsValid = start != null && end != null && start in 1..20 && end in start..20
    val creditsValid = creditCount != null && creditCount in 1..10
    val timesValid = runCatching { LocalTime.parse(startTime).isBefore(LocalTime.parse(endTime)) }.getOrDefault(false)
    val valid = courseName.isNotBlank() && room.isNotBlank() && campus.isNotBlank() &&
        periodsValid && creditsValid && timesValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.timetable_add_class else R.string.timetable_edit_class)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                FormField(courseName, { courseName = it }, R.string.timetable_course_name)
                FormField(courseCode, { courseCode = it }, R.string.timetable_course_code)
                FormField(room, { room = it }, R.string.timetable_room)
                FormField(campus, { campus = it }, R.string.timetable_campus)
                FormField(lecturer, { lecturer = it }, R.string.timetable_lecturer)
                Text(stringResource(R.string.timetable_day_of_week), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
                    (1..7).forEach { value ->
                        FilterChip(
                            selected = day == value,
                            onClick = { day = value },
                            label = { Text(DayOfWeek.of(value).getDisplayName(TextStyle.SHORT, Locale.getDefault())) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    FormField(startPeriod, { startPeriod = it }, R.string.timetable_start_period, Modifier.weight(1f), true)
                    FormField(endPeriod, { endPeriod = it }, R.string.timetable_end_period, Modifier.weight(1f), true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    FormField(startTime, { startTime = it }, R.string.timetable_start_time, Modifier.weight(1f))
                    FormField(endTime, { endTime = it }, R.string.timetable_end_time, Modifier.weight(1f))
                }
                FormField(credits, { credits = it }, R.string.timetable_credits, numeric = true)
                if (!valid) Text(
                    stringResource(R.string.timetable_form_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        CourseSchedule(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            courseName = courseName.trim(),
                            courseCode = courseCode.trim(),
                            lecturer = lecturer.trim(),
                            room = room.trim(),
                            campus = campus.trim(),
                            dayOfWeek = day,
                            startPeriod = requireNotNull(start),
                            endPeriod = requireNotNull(end),
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            credits = requireNotNull(creditCount),
                        ),
                    )
                },
            ) { Text(stringResource(R.string.timetable_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.timetable_cancel)) } },
    )
}

@Composable
private fun DeadlineFormDialog(
    initial: Deadline?,
    onDismiss: () -> Unit,
    onSave: (Deadline) -> Unit,
) {
    var title by rememberSaveable(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var course by rememberSaveable(initial?.id) { mutableStateOf(initial?.courseName.orEmpty()) }
    var dueDate by rememberSaveable(initial?.id) { mutableStateOf(initial?.dueDate?.toString() ?: LocalDate.now().toString()) }
    var dueTime by rememberSaveable(initial?.id) { mutableStateOf(initial?.dueTime?.toString() ?: "23:59") }
    var note by rememberSaveable(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    val parsedDate = runCatching { LocalDate.parse(dueDate) }.getOrNull()
    val parsedTime = runCatching { LocalTime.parse(dueTime) }.getOrNull()
    val valid = title.isNotBlank() && parsedDate != null && parsedTime != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.timetable_add_deadline else R.string.timetable_edit_deadline)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                FormField(title, { title = it }, R.string.timetable_deadline_title)
                FormField(course, { course = it }, R.string.timetable_course_name_optional)
                FormField(dueDate, { dueDate = it }, R.string.timetable_due_date_iso)
                FormField(dueTime, { dueTime = it }, R.string.timetable_due_time, numeric = false)
                FormField(note, { note = it }, R.string.timetable_note)
                if (!valid) Text(
                    stringResource(R.string.timetable_deadline_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        Deadline(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            title = title.trim(),
                            courseName = course.trim(),
                            dueDate = requireNotNull(parsedDate),
                            note = note.trim(),
                            dueTime = requireNotNull(parsedTime),
                        ),
                    )
                },
            ) { Text(stringResource(R.string.timetable_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.timetable_cancel)) } },
    )
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
    )
}

private fun selectedWeek(date: LocalDate): List<LocalDate> {
    val monday = date.minusDays(date.dayOfWeek.value.toLong() - 1)
    return (0L..6L).map(monday::plusDays)
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ScheduleScreenPreview() = UthPreview {
    ScheduleScreen(ScheduleUiState(isLoading = false), {})
}

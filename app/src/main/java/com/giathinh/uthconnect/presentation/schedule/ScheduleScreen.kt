package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    Column(
        modifier = modifier
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
                    DeadlineCard(deadline, uiState, dateFormatter, onAction)
                }
            }
        }

        if (later.isNotEmpty()) {
            UthSectionHeader(title = stringResource(R.string.timetable_later))
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md)) {
                later.forEach { deadline ->
                    DeadlineCard(deadline, uiState, dateFormatter, onAction)
                }
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Button(
            onClick = { onAction(ScheduleAction.OpenForm) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
        ) {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.IconInline),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Text(stringResource(R.string.timetable_add))
        }
        Text(
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
    dateFormatter: DateTimeFormatter,
    onAction: (ScheduleAction) -> Unit,
) {
    val overdue = item.dueAt.isBefore(state.now)
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
                    text = stringResource(if (overdue) R.string.timetable_overdue else R.string.timetable_urgent_badge),
                    tone = if (overdue) BadgeTone.Danger else BadgeTone.Warning,
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
                    text = stringResource(R.string.timetable_due, item.dueAt.format(dateFormatter)),
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
private fun DeadlineForm(state: ScheduleUiState, onAction: (ScheduleAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(ScheduleAction.CloseForm) },
        title = { Text(stringResource(R.string.timetable_add), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                Text(
                    text = stringResource(R.string.timetable_form_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = UthTheme.extendedColors.textSecondary,
                )
                Text(stringResource(R.string.timetable_label_course), style = MaterialTheme.typography.labelLarge)
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
                    onValueChange = { onAction(ScheduleAction.Date(it)) },
                    label = { Text(stringResource(R.string.timetable_label_date)) },
                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.time,
                    onValueChange = { onAction(ScheduleAction.Time(it)) },
                    label = { Text(stringResource(R.string.timetable_label_time)) },
                    leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.invalidForm) {
                    Text(
                        text = stringResource(R.string.timetable_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(ScheduleAction.Save) }) {
                Text(stringResource(R.string.timetable_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ScheduleAction.CloseForm) }) {
                Text(stringResource(R.string.timetable_cancel))
            }
        },
    )
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

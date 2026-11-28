package com.giathinh.uthconnect.presentation.incidents

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentStatusLog
import com.giathinh.uthconnect.domain.validation.IncidentFieldError

@Composable
internal fun IncidentCreateDialog(
    form: IncidentFormState,
    onAction: (IncidentsAction) -> Unit,
    onPickPhotos: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(IncidentsAction.DismissCreateSheet) },
        title = {
            Text(
                text = stringResource(R.string.incident_create_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            IncidentCreateFormBody(
                form = form,
                onAction = onAction,
                onPickPhotos = onPickPhotos,
            )
        },
        confirmButton = {
            Button(
                onClick = { onAction(IncidentsAction.SubmitReport) },
                enabled = !form.isSubmitting && !form.isUploadingImage,
            ) {
                Text(
                    stringResource(
                        if (form.isSubmitting) {
                            R.string.incident_submitting
                        } else {
                            R.string.incident_submit_action
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(IncidentsAction.DismissCreateSheet) },
                enabled = !form.isSubmitting && !form.isUploadingImage,
            ) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun IncidentCreateFormBody(
    form: IncidentFormState,
    onAction: (IncidentsAction) -> Unit,
    onPickPhotos: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        Text(
            text = stringResource(R.string.incident_campus_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            INCIDENT_CAMPUSES.forEach { campus ->
                FilterChip(
                    selected = form.campus == campus,
                    onClick = { onAction(IncidentsAction.CampusChanged(campus)) },
                    label = { Text(campus) },
                )
            }
        }
        OutlinedTextField(
            value = form.roomCode,
            onValueChange = { onAction(IncidentsAction.RoomCodeChanged(it)) },
            label = { Text(stringResource(R.string.incident_room_label)) },
            isError = form.roomError != null,
            supportingText = form.roomError?.let { err -> { Text(stringResource(err.labelRes())) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.locationDetail,
            onValueChange = { onAction(IncidentsAction.LocationDetailChanged(it)) },
            label = { Text(stringResource(R.string.incident_location_detail_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        IncidentCategorySelector(
            selected = form.category,
            onSelect = { onAction(IncidentsAction.CategoryChanged(it)) },
        )
        IncidentSeveritySelector(
            selected = form.severity,
            onSelect = { onAction(IncidentsAction.SeverityChanged(it)) },
        )
        OutlinedTextField(
            value = form.title,
            onValueChange = { onAction(IncidentsAction.TitleChanged(it)) },
            label = { Text(stringResource(R.string.incident_title_label)) },
            isError = form.titleError != null,
            supportingText = form.titleError?.let { err -> { Text(stringResource(err.labelRes())) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = { onAction(IncidentsAction.DescriptionChanged(it)) },
            label = { Text(stringResource(R.string.incident_description_label)) },
            isError = form.descriptionError != null,
            supportingText = form.descriptionError?.let { err -> { Text(stringResource(err.labelRes())) } },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        IncidentPhotosSection(form = form, onAction = onAction, onPickPhotos = onPickPhotos)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = form.isAnonymousPublic,
                onCheckedChange = { onAction(IncidentsAction.AnonymousChanged(it)) },
            )
            Text(
                text = stringResource(R.string.incident_anonymous_label),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun IncidentCategorySelector(
    selected: IncidentCategory,
    onSelect: (IncidentCategory) -> Unit,
) {
    Text(
        text = stringResource(R.string.incident_category_label),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
    ) {
        IncidentCategory.entries.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes())) },
            )
        }
    }
}

@Composable
private fun IncidentSeveritySelector(
    selected: IncidentSeverity,
    onSelect: (IncidentSeverity) -> Unit,
) {
    Text(
        text = stringResource(R.string.incident_severity_label),
        style = MaterialTheme.typography.labelLarge,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
    ) {
        IncidentSeverity.entries.forEach { severity ->
            FilterChip(
                selected = selected == severity,
                onClick = { onSelect(severity) },
                label = { Text(stringResource(severity.labelRes())) },
            )
        }
    }
}

@Composable
private fun IncidentPhotosSection(
    form: IncidentFormState,
    onAction: (IncidentsAction) -> Unit,
    onPickPhotos: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                R.string.incident_images_count,
                form.imagePaths.size,
                MAX_INCIDENT_IMAGES,
            ),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = onPickPhotos,
            enabled = !form.isUploadingImage &&
                !form.isSubmitting &&
                form.imagePaths.size < MAX_INCIDENT_IMAGES,
        ) {
            Text(
                stringResource(
                    if (form.isUploadingImage) {
                        R.string.incident_uploading_image
                    } else {
                        R.string.incident_add_image
                    },
                ),
            )
        }
    }
    form.imagePaths.forEachIndexed { index, path ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.incident_image_item, index + 1),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { onAction(IncidentsAction.RemoveImage(path)) }) {
                Text(stringResource(R.string.books_remove_photo))
            }
        }
    }
    form.imageError?.let { err ->
        Text(
            text = stringResource(err.labelRes()),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun IncidentDetailDialog(
    report: IncidentReport,
    history: List<IncidentStatusLog>,
    isLoadingHistory: Boolean,
    isTransitioning: Boolean,
    reopenNote: String,
    onAction: (IncidentsAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(IncidentsAction.DismissDetail) },
        title = {
            Text(
                text = "${report.code} · ${report.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            IncidentDetailBody(
                report = report,
                history = history,
                isLoadingHistory = isLoadingHistory,
                isTransitioning = isTransitioning,
                reopenNote = reopenNote,
                onAction = onAction,
            )
        },
        confirmButton = {
            TextButton(onClick = { onAction(IncidentsAction.DismissDetail) }) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

@Composable
private fun IncidentDetailBody(
    report: IncidentReport,
    history: List<IncidentStatusLog>,
    isLoadingHistory: Boolean,
    isTransitioning: Boolean,
    reopenNote: String,
    onAction: (IncidentsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
    ) {
        Text(
            text = "${report.campus} · ${report.roomCode} · ${stringResource(report.status.labelRes())}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        report.locationDetail?.takeIf { it.isNotBlank() }?.let { detail ->
            Text(text = detail, style = MaterialTheme.typography.bodySmall)
        }
        Text(text = report.description, style = MaterialTheme.typography.bodyMedium)

        if (report.status == IncidentStatus.SUBMITTED) {
            OutlinedButton(
                onClick = { onAction(IncidentsAction.CancelReport(report)) },
                enabled = !isTransitioning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.incident_cancel_report_action))
            }
        }

        if (report.status == IncidentStatus.RESOLVED) {
            Button(
                onClick = { onAction(IncidentsAction.CloseResolvedReport(report)) },
                enabled = !isTransitioning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.incident_close_report_action))
            }
            OutlinedTextField(
                value = reopenNote,
                onValueChange = { onAction(IncidentsAction.ReopenNoteChanged(it)) },
                label = { Text(stringResource(R.string.incident_reopen_note_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = { onAction(IncidentsAction.ReopenResolvedReport(report)) },
                enabled = !isTransitioning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.incident_reopen_report_action))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.incident_history_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (isLoadingHistory && history.isEmpty()) {
            Text(
                text = stringResource(R.string.incident_history_loading),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            history.forEach { log ->
                Column(modifier = Modifier.padding(vertical = UthDimens.Spacing.Xxs)) {
                    Text(
                        text = "${stringResource(log.toStatus.labelRes())} · ${log.createdAt}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    log.note?.takeIf { it.isNotBlank() }?.let { note ->
                        Text(text = note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@StringRes
internal fun IncidentFieldError.labelRes(): Int = when (this) {
    IncidentFieldError.CAMPUS_REQUIRED -> R.string.incident_field_error_campus
    IncidentFieldError.INVALID_REQUEST_ID -> R.string.incident_error_create
    IncidentFieldError.ROOM_REQUIRED -> R.string.incident_field_error_room
    IncidentFieldError.TITLE_LENGTH -> R.string.incident_field_error_title
    IncidentFieldError.DESCRIPTION_LENGTH -> R.string.incident_field_error_description
    IncidentFieldError.INVALID_IMAGE_COUNT -> R.string.incident_field_error_images
}

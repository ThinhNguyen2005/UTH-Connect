package com.giathinh.uthconnect.presentation.incidents

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthContentSkeleton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.domain.model.IncidentCategory
import com.giathinh.uthconnect.domain.model.IncidentReport
import com.giathinh.uthconnect.domain.model.IncidentSeverity
import com.giathinh.uthconnect.domain.model.IncidentStatus
import com.giathinh.uthconnect.domain.model.IncidentSummary

internal val INCIDENT_CAMPUSES = listOf("CS1", "CS2", "CS3")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentsScreen(
    uiState: IncidentsUiState,
    onAction: (IncidentsAction) -> Unit,
    onPickPhotos: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.incident_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onAction(IncidentsAction.Refresh) },
                        enabled = !uiState.isRefreshing,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.incident_refresh),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(IncidentsAction.OpenCreateSheet) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.incident_create_action)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            IncidentTopControls(uiState = uiState, onAction = onAction)
            IncidentBodyContent(uiState = uiState, onAction = onAction)
        }
    }

    if (uiState.showCreateSheet) {
        IncidentCreateDialog(
            form = uiState.form,
            onAction = onAction,
            onPickPhotos = onPickPhotos,
        )
    }

    uiState.selectedReport?.let { report ->
        IncidentDetailDialog(
            report = report,
            history = uiState.statusHistory,
            isLoadingHistory = uiState.isLoadingDetail,
            isTransitioning = uiState.isTransitioning,
            reopenNote = uiState.reopenNote,
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentTopControls(
    uiState: IncidentsUiState,
    onAction: (IncidentsAction) -> Unit,
) {
    PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
        Tab(
            selected = uiState.selectedTab == IncidentTab.MY_REPORTS,
            onClick = { onAction(IncidentsAction.SelectTab(IncidentTab.MY_REPORTS)) },
            text = { Text(stringResource(R.string.incident_tab_mine)) },
        )
        Tab(
            selected = uiState.selectedTab == IncidentTab.CAMPUS_FEED,
            onClick = { onAction(IncidentsAction.SelectTab(IncidentTab.CAMPUS_FEED)) },
            text = { Text(stringResource(R.string.incident_tab_campus)) },
        )
    }

    if (uiState.selectedTab == IncidentTab.CAMPUS_FEED) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Sm),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            FilterChip(
                selected = uiState.campusFilter == null,
                onClick = { onAction(IncidentsAction.SelectCampusFilter(null)) },
                label = { Text(stringResource(R.string.incident_campus_all)) },
            )
            INCIDENT_CAMPUSES.forEach { campus ->
                FilterChip(
                    selected = uiState.campusFilter == campus,
                    onClick = { onAction(IncidentsAction.SelectCampusFilter(campus)) },
                    label = { Text(campus) },
                )
            }
        }
    }

    uiState.error?.let { errorCode ->
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = UthDimens.Spacing.Lg, vertical = UthDimens.Spacing.Xs),
            shape = MaterialTheme.shapes.small,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UthDimens.Spacing.Md, vertical = UthDimens.Spacing.Xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(errorCode.messageRes()),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onAction(IncidentsAction.DismissError) }) {
                    Text(stringResource(R.string.common_close))
                }
            }
        }
    }
}

@Composable
private fun IncidentBodyContent(
    uiState: IncidentsUiState,
    onAction: (IncidentsAction) -> Unit,
) {
    if (uiState.isLoading) {
        UthContentSkeleton()
        return
    }
    when (uiState.selectedTab) {
        IncidentTab.MY_REPORTS -> MyReportsList(
            reports = uiState.myReports,
            onOpenDetail = { onAction(IncidentsAction.OpenDetail(it)) },
        )

        IncidentTab.CAMPUS_FEED -> CampusOpenFeedList(
            items = uiState.openFeed,
            confirmingIds = uiState.confirmingIds,
            confirmedIds = uiState.confirmedIds,
            onConfirm = { onAction(IncidentsAction.ConfirmOpenIncident(it)) },
        )
    }
}

@Composable
private fun MyReportsList(
    reports: List<IncidentReport>,
    onOpenDetail: (String) -> Unit,
) {
    if (reports.isEmpty()) {
        IncidentEmptyBox(
            title = stringResource(R.string.incident_empty_mine_title),
            subtitle = stringResource(R.string.incident_empty_mine_desc),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        items(reports, key = IncidentReport::id) { report ->
            UthCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDetail(report.id) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${report.code} · ${report.campus} - ${report.roomCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(report.status.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = report.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xxs))
                val categoryLabel = stringResource(report.category.labelRes())
                val severityLabel = stringResource(report.severity.labelRes())
                Text(
                    text = "$categoryLabel · $severityLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CampusOpenFeedList(
    items: List<IncidentSummary>,
    confirmingIds: Set<String>,
    confirmedIds: Set<String>,
    onConfirm: (String) -> Unit,
) {
    if (items.isEmpty()) {
        IncidentEmptyBox(
            title = stringResource(R.string.incident_empty_feed_title),
            subtitle = stringResource(R.string.incident_empty_feed_desc),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
    ) {
        items(items, key = IncidentSummary::id) { item ->
            val isConfirming = item.id in confirmingIds
            val isConfirmed = item.id in confirmedIds
            UthCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${item.code} · ${item.campus} - ${item.roomCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(item.status.labelRes()),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            R.string.incident_confirm_count,
                            item.confirmationCount.toInt(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { onConfirm(item.id) },
                        enabled = !isConfirming && !isConfirmed,
                    ) {
                        Text(
                            text = stringResource(
                                if (isConfirmed) {
                                    R.string.incident_confirmed_action
                                } else {
                                    R.string.incident_confirm_action
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentEmptyBox(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(UthDimens.Spacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
internal fun IncidentStatus.labelRes(): Int = when (this) {
    IncidentStatus.SUBMITTED -> R.string.incident_status_submitted
    IncidentStatus.ACKNOWLEDGED -> R.string.incident_status_acknowledged
    IncidentStatus.IN_PROGRESS -> R.string.incident_status_in_progress
    IncidentStatus.RESOLVED -> R.string.incident_status_resolved
    IncidentStatus.REOPENED -> R.string.incident_status_reopened
    IncidentStatus.CLOSED -> R.string.incident_status_closed
    IncidentStatus.REJECTED -> R.string.incident_status_rejected
    IncidentStatus.DUPLICATE -> R.string.incident_status_duplicate
    IncidentStatus.CANCELLED -> R.string.incident_status_cancelled
}

@StringRes
internal fun IncidentCategory.labelRes(): Int = when (this) {
    IncidentCategory.DOOR_LOCK -> R.string.incident_category_door_lock
    IncidentCategory.ELECTRICAL -> R.string.incident_category_electrical
    IncidentCategory.WATER_PLUMBING -> R.string.incident_category_water_plumbing
    IncidentCategory.AC_FAN -> R.string.incident_category_ac_fan
    IncidentCategory.PROJECTOR_IT -> R.string.incident_category_projector_it
    IncidentCategory.FURNITURE -> R.string.incident_category_furniture
    IncidentCategory.HYGIENE -> R.string.incident_category_hygiene
    IncidentCategory.SAFETY -> R.string.incident_category_safety
    IncidentCategory.OTHER -> R.string.incident_category_other
}

@StringRes
internal fun IncidentSeverity.labelRes(): Int = when (this) {
    IncidentSeverity.LOW -> R.string.incident_severity_low
    IncidentSeverity.MEDIUM -> R.string.incident_severity_medium
    IncidentSeverity.HIGH -> R.string.incident_severity_high
    IncidentSeverity.CRITICAL -> R.string.incident_severity_critical
}

@StringRes
internal fun IncidentErrorCode.messageRes(): Int = when (this) {
    IncidentErrorCode.LOAD_FAILED -> R.string.incident_error_load
    IncidentErrorCode.DETAIL_LOAD_FAILED -> R.string.incident_error_detail
    IncidentErrorCode.UPLOAD_FAILED -> R.string.incident_error_upload
    IncidentErrorCode.CREATE_FAILED -> R.string.incident_error_create
    IncidentErrorCode.RATE_LIMIT -> R.string.incident_error_rate_limit
    IncidentErrorCode.INVALID_IMAGE -> R.string.incident_error_invalid_image
    IncidentErrorCode.CONFLICT -> R.string.incident_error_conflict
    IncidentErrorCode.SELF_CONFIRM -> R.string.incident_error_self_confirm
    IncidentErrorCode.NOTE_REQUIRED -> R.string.incident_error_note_required
    IncidentErrorCode.INVALID_TRANSITION -> R.string.incident_error_invalid_transition
    IncidentErrorCode.AUTH_REQUIRED -> R.string.common_error_auth_required
}

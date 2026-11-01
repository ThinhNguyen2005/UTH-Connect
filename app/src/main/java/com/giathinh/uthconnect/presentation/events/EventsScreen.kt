package com.giathinh.uthconnect.presentation.events

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.EventTopic
import java.time.LocalDateTime

/** Chỉ nhận dữ liệu và phát action; không tự tải dữ liệu hay lấy ViewModel. */
@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onAction: (EventsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.events_back)) }
                Text(stringResource(R.string.events_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.events_sample_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "campus_filter") { CampusFilter(uiState, onAction) }
        when {
            uiState.isLoading -> item(key = "loading") {
                Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(UthDimens.Size.Progress),
                        strokeWidth = UthDimens.Size.ProgressStroke,
                    )
                    Text(stringResource(R.string.events_loading))
                }
            }
            uiState.failure != null -> item(key = "error") {
                UthEmptyState(
                    title = stringResource(R.string.events_error_title),
                    body = stringResource(R.string.events_error_body),
                    actionLabel = stringResource(R.string.common_retry),
                    onAction = { onAction(EventsAction.Retry) },
                )
            }
            uiState.isEmpty -> item(key = "empty") {
                UthEmptyState(
                    title = stringResource(R.string.events_empty_title),
                    body = stringResource(R.string.events_empty_body),
                    actionLabel = stringResource(R.string.events_all_campuses),
                    onAction = { onAction(EventsAction.SelectCampus(null)) },
                )
            }
            else -> items(uiState.events, key = { it.id }) { event -> EventCard(event, onAction) }
        }
    }
}

@Composable
private fun CampusFilter(uiState: EventsUiState, onAction: (EventsAction) -> Unit) {
    // LazyRow giúp bộ lọc không bị tràn trên máy nhỏ hoặc khi tăng cỡ chữ.
    LazyRow(horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
        item(key = "all") {
            FilterChip(
                selected = uiState.selectedCampus == null,
                onClick = { onAction(EventsAction.SelectCampus(null)) },
                label = { Text(stringResource(R.string.events_all_campuses)) },
            )
        }
        items(Campus.entries, key = { it.name }) { campus ->
            FilterChip(
                selected = uiState.selectedCampus == campus,
                onClick = { onAction(EventsAction.SelectCampus(campus)) },
                label = { Text(stringResource(campus.labelRes())) },
            )
        }
    }
}

@Composable
private fun EventCard(event: Event, onAction: (EventsAction) -> Unit) {
    val (titleRes, descriptionRes, venueRes) = event.topic.contentRes()
    val (statusRes, tone) = event.status.badgeSpec()
    UthCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(R.string.events_location, stringResource(event.campus.labelRes()), stringResource(venueRes)),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(
                    R.string.events_date_time,
                    event.startAt.dayOfMonth, event.startAt.monthValue, event.startAt.year,
                    event.startAt.hour, event.startAt.minute, event.endAt.hour, event.endAt.minute,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            // FlowRow tự xuống hàng để hai badge không đè lên nhau khi chữ lớn.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                UthStatusBadge(text = stringResource(statusRes), tone = tone)
                UthStatusBadge(
                    text = if (event.remainingSeats == 0) stringResource(R.string.events_full)
                    else stringResource(R.string.events_capacity, event.remainingSeats, event.capacity),
                    tone = if (event.remainingSeats == 0) BadgeTone.Danger else BadgeTone.Info,
                )
            }
            TextButton(onClick = { onAction(EventsAction.OpenDetail(event.id)) }) {
                Text(stringResource(R.string.events_view_detail))
            }
            UthPrimaryButton(
                text = stringResource(R.string.events_register),
                onClick = { onAction(EventsAction.Register(event.id)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = event.canRegister,
            )
        }
    }
}

private fun Campus.labelRes(): Int = when (this) {
    Campus.BINH_THANH -> R.string.events_campus_binh_thanh
    Campus.THU_DUC -> R.string.events_campus_thu_duc
    Campus.QUAN_12 -> R.string.events_campus_quan_12
}

private fun EventStatus.badgeSpec(): Pair<Int, BadgeTone> = when (this) {
    EventStatus.OPEN -> R.string.events_status_open to BadgeTone.Success
    EventStatus.CLOSED -> R.string.events_status_closed to BadgeTone.Warning
    EventStatus.ENDED -> R.string.events_status_ended to BadgeTone.Neutral
    EventStatus.CANCELLED -> R.string.events_status_cancelled to BadgeTone.Danger
}

/** Nội dung mẫu được dịch ở UI, domain không phụ thuộc R.string hay Context. */
private fun EventTopic.contentRes(): Triple<Int, Int, Int> = when (this) {
    EventTopic.CAREER_DAY -> Triple(R.string.events_career_title, R.string.events_career_body, R.string.events_venue_hall)
    EventTopic.ANDROID_WORKSHOP -> Triple(R.string.events_android_title, R.string.events_android_body, R.string.events_venue_lab)
    EventTopic.ENGLISH_CLUB -> Triple(R.string.events_english_title, R.string.events_english_body, R.string.events_venue_club)
    EventTopic.GREEN_CAMPUS -> Triple(R.string.events_green_title, R.string.events_green_body, R.string.events_venue_yard)
    EventTopic.LOGISTICS_TALK -> Triple(R.string.events_logistics_title, R.string.events_logistics_body, R.string.events_venue_hall)
    EventTopic.WELCOME_FRESHMEN -> Triple(R.string.events_welcome_title, R.string.events_welcome_body, R.string.events_venue_hall)
}

private val previewEvent = Event(
    id = "preview", topic = EventTopic.CAREER_DAY, campus = Campus.BINH_THANH,
    startAt = LocalDateTime.of(2026, 11, 7, 8, 0), endAt = LocalDateTime.of(2026, 11, 7, 11, 30),
    capacity = 200, registeredCount = 145, status = EventStatus.OPEN,
)

@Preview(name = "Content light", showBackground = true)
@Preview(name = "Content dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventsContentPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventsScreen(EventsUiState(isLoading = false, events = listOf(previewEvent)), {}, {})
}

@Preview(name = "Loading light", showBackground = true)
@Preview(name = "Loading dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventsLoadingPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventsScreen(EventsUiState(), {}, {})
}

@Preview(name = "Empty light", showBackground = true)
@Preview(name = "Empty dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventsEmptyPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventsScreen(EventsUiState(isLoading = false), {}, {})
}

@Preview(name = "Error light", showBackground = true)
@Preview(name = "Error dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventsErrorPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventsScreen(EventsUiState(isLoading = false, failure = EventsFailure.LoadFailed), {}, {})
}

package com.giathinh.uthconnect.presentation.events.detail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.Campus
import com.giathinh.uthconnect.domain.model.Event
import com.giathinh.uthconnect.domain.model.EventStatus
import com.giathinh.uthconnect.domain.model.EventTopic
import java.time.LocalDateTime

@Composable
fun EventDetailScreen(
    uiState: EventDetailUiState,
    onAction: (EventDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.events_back)) }
        Text(stringResource(R.string.events_detail_title), style = MaterialTheme.typography.headlineMedium)

        when {
            uiState.isLoading -> {
                CircularProgressIndicator()
                Text(stringResource(R.string.events_loading))
            }
            uiState.failure != null -> {
                val notFound = uiState.failure == EventDetailFailure.EventNotFound
                UthEmptyState(
                    title = stringResource(
                        if (notFound) R.string.events_detail_not_found else R.string.events_detail_load_error,
                    ),
                    body = stringResource(
                        if (notFound) R.string.events_detail_not_found_body else R.string.events_detail_load_error_body,
                    ),
                    actionLabel = if (notFound) null else stringResource(R.string.common_retry),
                    onAction = if (notFound) null else ({ onAction(EventDetailAction.Retry) }),
                )
            }
            uiState.event != null -> {
                UthCard {
                    Text(
                        stringResource(R.string.events_detail_id, uiState.event.id),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.events_detail_placeholder),
                        modifier = Modifier.padding(top = UthDimens.Spacing.Sm),
                    )
                }

                if (uiState.registration != null) {
                    Text(stringResource(R.string.events_registration_success, uiState.registration.id))
                } else if (uiState.registrationFailed) {
                    Text(stringResource(R.string.events_registration_error), color = MaterialTheme.colorScheme.error)
                } else if (!uiState.event.canRegister) {
                    Text(stringResource(R.string.events_registration_unavailable))
                }

                UthPrimaryButton(
                    text = stringResource(R.string.events_register),
                    onClick = { onAction(EventDetailAction.Register) },
                    enabled = uiState.canRegister,
                    loading = uiState.isRegistering,
                )
            }
        }
    }
}

@Preview(name = "Detail light", showBackground = true)
@Preview(name = "Detail dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventDetailPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventDetailScreen(
        uiState = EventDetailUiState(isLoading = false, event = previewEvent),
        onAction = {},
        onBack = {},
    )
}

private val previewEvent = Event(
    id = "career-day",
    topic = EventTopic.CAREER_DAY,
    campus = Campus.BINH_THANH,
    startAt = LocalDateTime.of(2026, 11, 7, 8, 0),
    endAt = LocalDateTime.of(2026, 11, 7, 11, 30),
    capacity = 200,
    registeredCount = 145,
    status = EventStatus.OPEN,
)

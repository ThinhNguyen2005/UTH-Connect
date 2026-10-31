package com.giathinh.uthconnect.presentation.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.Ticket
import com.giathinh.uthconnect.domain.model.TicketStatus

/** Stateless list and placeholder QR screens. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TicketsScreen(
    uiState: TicketsUiState,
    onAction: (TicketsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(title = { Text(stringResource(R.string.tickets_title)) })
    }) { padding ->
        when {
            uiState.isLoading -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
                Spacer(Modifier.height(UthDimens.Spacing.Lg))
                Text(stringResource(R.string.tickets_loading))
            }
            uiState.failure != null -> UthEmptyState(
                title = stringResource(R.string.tickets_error_title),
                body = stringResource(R.string.tickets_error_body),
                icon = Icons.Filled.Warning,
                actionLabel = stringResource(R.string.common_retry),
                onAction = { onAction(TicketsAction.Retry) },
                modifier = Modifier.fillMaxSize().padding(padding).padding(UthDimens.Spacing.Xl),
            )
            uiState.isEmpty -> UthEmptyState(
                title = stringResource(R.string.tickets_empty_title),
                body = stringResource(R.string.tickets_empty_body),
                icon = Icons.Filled.DateRange,
                modifier = Modifier.fillMaxSize().padding(padding).padding(UthDimens.Spacing.Xl),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(UthDimens.Spacing.Xl),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                items(uiState.tickets, key = Ticket::id) { ticket ->
                    TicketCard(ticket) { onAction(TicketsAction.OpenTicket(ticket.id)) }
                }
            }
        }
    }
}

@Composable
private fun TicketCard(ticket: Ticket, onOpenTicket: () -> Unit) {
    UthCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenTicket) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ticket.eventTitle, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            val (label, tone) = when (ticket.status) {
                TicketStatus.VALID -> stringResource(R.string.tickets_status_valid) to BadgeTone.Success
                TicketStatus.CHECKED_IN -> stringResource(R.string.tickets_status_checked_in) to BadgeTone.Info
                TicketStatus.CANCELLED -> stringResource(R.string.tickets_status_cancelled) to BadgeTone.Danger
            }
            UthStatusBadge(label, tone)
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(ticket.eventLocation, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(ticket.eventStartsAt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(stringResource(R.string.tickets_open_qr), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

/** QR placeholder as text; it is intentionally not scannable in this task. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TicketQrScreen(
    ticket: Ticket?,
    secondsRemaining: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.tickets_qr_title)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back)) } },
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(UthDimens.Spacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
        ) {
            if (ticket == null) {
                Text(
                    text = stringResource(R.string.tickets_qr_missing),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            Text(ticket.eventTitle, style = MaterialTheme.typography.titleLarge)
            UthCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tickets_qr_placeholder), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(UthDimens.Spacing.Sm))
                Text(
                    text = stringResource(R.string.tickets_qr_text),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(UthDimens.Spacing.Sm))
                Text(stringResource(R.string.tickets_qr_payload, ticket.id), style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.tickets_fallback_code, ticket.fallbackCode), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.tickets_countdown, secondsRemaining), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.tickets_qr_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val PreviewTicket = Ticket("preview", "2023040123", "event", "Hội thao Sinh viên UTH 2026", "Cơ sở Quang Trung", "12/11/2026 · 07:00", TicketStatus.VALID, "482 917")

@Preview(name = "Tickets light", showBackground = true)
@Composable
private fun TicketsPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, tickets = listOf(PreviewTicket)),
)

@Preview(name = "Tickets dark", showBackground = true)
@Composable
private fun TicketsDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, tickets = listOf(PreviewTicket)), darkTheme = true,
)

@Preview(name = "Tickets loading light", showBackground = true)
@Composable
private fun TicketsLoadingPreview() = TicketsStatePreview(TicketsUiState())

@Preview(name = "Tickets loading dark", showBackground = true)
@Composable
private fun TicketsLoadingDarkPreview() = TicketsStatePreview(TicketsUiState(), darkTheme = true)

@Preview(name = "Tickets error light", showBackground = true)
@Composable
private fun TicketsErrorPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, failure = TicketsFailure.LoadFailed),
)

@Preview(name = "Tickets error dark", showBackground = true)
@Composable
private fun TicketsErrorDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false, failure = TicketsFailure.LoadFailed), darkTheme = true,
)

@Preview(name = "Tickets empty light", showBackground = true)
@Composable
private fun TicketsEmptyPreview() = TicketsStatePreview(TicketsUiState(isLoading = false))

@Preview(name = "Tickets empty dark", showBackground = true)
@Composable
private fun TicketsEmptyDarkPreview() = TicketsStatePreview(
    TicketsUiState(isLoading = false), darkTheme = true,
)

@Preview(name = "QR light", showBackground = true)
@Composable
private fun TicketQrPreview() = TicketQrStatePreview(PreviewTicket, 60)

@Preview(name = "QR dark", showBackground = true)
@Composable
private fun TicketQrDarkPreview() = TicketQrStatePreview(PreviewTicket, 60, darkTheme = true)

@Preview(name = "QR checked-in light", showBackground = true)
@Composable
private fun TicketQrCheckedInPreview() = TicketQrStatePreview(
    PreviewTicket.copy(status = TicketStatus.CHECKED_IN), 60,
)

@Preview(name = "QR checked-in dark", showBackground = true)
@Composable
private fun TicketQrCheckedInDarkPreview() = TicketQrStatePreview(
    PreviewTicket.copy(status = TicketStatus.CHECKED_IN), 60, darkTheme = true,
)

@Preview(name = "QR expired light", showBackground = true)
@Composable
private fun TicketQrExpiredPreview() = TicketQrStatePreview(PreviewTicket, 0)

@Preview(name = "QR expired dark", showBackground = true)
@Composable
private fun TicketQrExpiredDarkPreview() = TicketQrStatePreview(PreviewTicket, 0, darkTheme = true)

@Preview(name = "QR missing light", showBackground = true)
@Composable
private fun TicketQrMissingPreview() = TicketQrStatePreview(null, 0)

@Preview(name = "QR missing dark", showBackground = true)
@Composable
private fun TicketQrMissingDarkPreview() = TicketQrStatePreview(null, 0, darkTheme = true)

@Composable
private fun TicketsStatePreview(state: TicketsUiState, darkTheme: Boolean = false) =
    UthPreview(darkTheme = darkTheme) { TicketsScreen(state, onAction = {}) }

@Composable
private fun TicketQrStatePreview(
    ticket: Ticket?,
    seconds: Int,
    darkTheme: Boolean = false,
) = UthPreview(darkTheme = darkTheme) { TicketQrScreen(ticket, seconds, {}) }

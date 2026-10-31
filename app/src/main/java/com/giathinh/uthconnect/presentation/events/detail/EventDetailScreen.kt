package com.giathinh.uthconnect.presentation.events.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

/** Placeholder đúng phạm vi task: chưa ghi đăng ký, chưa phát vé hay QR. */
@Composable
fun EventDetailScreen(eventId: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.events_back)) }
        Text(stringResource(R.string.events_detail_title), style = MaterialTheme.typography.headlineMedium)
        UthCard {
            Text(stringResource(R.string.events_detail_id, eventId), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.events_detail_placeholder),
                modifier = Modifier.padding(top = UthDimens.Spacing.Sm),
            )
        }
    }
}

@Preview(name = "Detail light", showBackground = true)
@Preview(name = "Detail dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventDetailPreview() = UthPreview(darkTheme = isSystemInDarkTheme()) {
    EventDetailScreen(eventId = "career-day", onBack = {})
}

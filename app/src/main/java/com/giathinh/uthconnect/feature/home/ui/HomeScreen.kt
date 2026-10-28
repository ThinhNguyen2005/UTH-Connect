package com.giathinh.uthconnect.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.UthDimens
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.feature.profile.ui.PreviewStudent
import com.giathinh.uthconnect.ui.theme.UthConnectTheme

/** Minimal landing page required by the assignment's acceptance flow. */
@Composable
fun HomeScreen(
    student: StudentProfile,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(UthDimens.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = UthDimens.pageMaxWidth).fillMaxWidth()) {
            Spacer(Modifier.height(UthDimens.large))
            Text(
                stringResource(R.string.home_title),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(UthDimens.regular))
            Text(
                stringResource(R.string.home_greeting, student.fullName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(UthDimens.small))
            Text(
                stringResource(R.string.home_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(UthDimens.xLarge))
            Text(
                stringResource(R.string.home_quick_access),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(UthDimens.regular))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(UthDimens.corner),
            ) {
                Column(Modifier.padding(UthDimens.large)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(UthDimens.regular))
                        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(UthDimens.regular))
                    Button(
                        onClick = onOpenProfile,
                        modifier = Modifier.fillMaxWidth().testTag("open_profile"),
                    ) {
                        Text(stringResource(R.string.home_profile_button))
                    }
                }
            }
            Spacer(Modifier.height(UthDimens.xLarge))
            Text(
                stringResource(R.string.home_upcoming_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(UthDimens.medium))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(UthDimens.large), verticalArrangement = Arrangement.spacedBy(UthDimens.regular)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(Modifier.width(UthDimens.regular))
                        Text(stringResource(R.string.home_events))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(UthDimens.regular))
                        Text(stringResource(R.string.home_schedule))
                    }
                    Text(
                        stringResource(R.string.home_feature_status),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Preview(name = "Home Light", showBackground = true)
@Composable
private fun HomeLightPreview() {
    UthConnectTheme(darkTheme = false) {
        HomeScreen(student = PreviewStudent, onOpenProfile = {})
    }
}

@Preview(name = "Home Dark", showBackground = true)
@Composable
private fun HomeDarkPreview() {
    UthConnectTheme(darkTheme = true) {
        HomeScreen(student = PreviewStudent, onOpenProfile = {})
    }
}

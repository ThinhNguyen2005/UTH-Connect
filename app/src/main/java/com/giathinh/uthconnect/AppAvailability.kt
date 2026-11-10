package com.giathinh.uthconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

internal enum class AppAvailability { AVAILABLE, MAINTENANCE, UPDATE_REQUIRED }

internal suspend fun refreshAvailability(
    read: () -> AppAvailability,
    fetchAndActivate: suspend () -> Unit,
    publish: (AppAvailability) -> Unit,
) {
    publish(read())
    fetchAndActivate()
    publish(read())
}

private fun parseVersion(version: String): List<Int>? {
    val items = version.split('.')
    val parsed = items.mapNotNull { it.toIntOrNull() }
    return if (version.matches(Regex("[0-9]+(\\.[0-9]+)*")) && parsed.size == items.size) parsed else null
}

internal fun availability(maintenance: Boolean, current: String, minimum: String): AppAvailability {
    val actual = parseVersion(current)
    val required = parseVersion(minimum)
    val maxLen = if (actual != null && required != null) maxOf(actual.size, required.size) else 0
    val diff = (0 until maxLen)
        .map { i -> (actual?.getOrElse(i) { 0 } ?: 0) - (required?.getOrElse(i) { 0 } ?: 0) }
        .firstOrNull { it != 0 } ?: 0

    return when {
        maintenance -> AppAvailability.MAINTENANCE
        actual == null || required == null -> AppAvailability.AVAILABLE
        diff < 0 -> AppAvailability.UPDATE_REQUIRED
        else -> AppAvailability.AVAILABLE
    }
}

@Composable
internal fun AvailabilityScreen(state: AppAvailability) {
    val title = if (state == AppAvailability.MAINTENANCE) {
        R.string.maintenance_title
    } else {
        R.string.update_required_title
    }
    val message = if (state == AppAvailability.MAINTENANCE) {
        R.string.maintenance_message
    } else {
        R.string.update_required_message
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(top = 12.dp),
            textAlign = TextAlign.Center,
        )
    }
}

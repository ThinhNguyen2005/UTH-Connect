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

internal fun availability(maintenance: Boolean, current: String, minimum: String): AppAvailability {
    if (maintenance) return AppAvailability.MAINTENANCE
    fun parts(version: String): List<Int>? {
        if (!version.matches(Regex("[0-9]+(\\.[0-9]+)*"))) return null
        return version.split('.').map { it.toIntOrNull() ?: return null }
    }
    val actual = parts(current) ?: return AppAvailability.AVAILABLE
    val required = parts(minimum) ?: return AppAvailability.AVAILABLE
    for (i in 0 until maxOf(actual.size, required.size)) {
        val comparison = actual.getOrElse(i) { 0 }.compareTo(required.getOrElse(i) { 0 })
        if (comparison < 0) return AppAvailability.UPDATE_REQUIRED
        if (comparison > 0) break
    }
    return AppAvailability.AVAILABLE
}

@Composable
internal fun AvailabilityScreen(state: AppAvailability) {
    val title = if (state == AppAvailability.MAINTENANCE) R.string.maintenance_title else R.string.update_required_title
    val message = if (state == AppAvailability.MAINTENANCE) R.string.maintenance_message else R.string.update_required_message
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(stringResource(message), modifier = Modifier.padding(top = 12.dp), textAlign = TextAlign.Center)
    }
}

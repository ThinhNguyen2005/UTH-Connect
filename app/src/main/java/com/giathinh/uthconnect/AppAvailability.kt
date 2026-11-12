package com.giathinh.uthconnect

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

/**
 * Trạng thái khả dụng của ứng dụng, được quyết định bởi cờ Remote Config.
 */
internal enum class AppAvailability { AVAILABLE, MAINTENANCE, UPDATE_REQUIRED }

/**
 * Đại diện cho trạng thái gate khả dụng đã được lưu bền vững vào SharedPreferences.
 */
internal data class SavedGate(
    val maintenanceMode: Boolean = false,
    val forceUpdateMinVersion: String = "1.0.0",
    val updateUrl: String = "",
    val hasSavedState: Boolean = false,
)

// Default static fallback URL when Remote Config update_url is not set
private const val UPDATE_URL = ""

internal fun remoteConfigFetchIntervalSeconds(
    isDebug: Boolean,
    userInitiated: Boolean,
): Long = if (isDebug || userInitiated) 0L else 3600L

internal fun isValidUpdateUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val trimmed = url.trim()
    if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return false

    if (trimmed.startsWith("market://", ignoreCase = true)) {
        val target = trimmed.substring("market://".length)
        if (target.isBlank()) return false
        return target.startsWith("details?", ignoreCase = true) || target.startsWith("search?", ignoreCase = true)
    }

    if (trimmed.startsWith("https://", ignoreCase = true)) {
        val authorityAndPath = trimmed.substring("https://".length)
        val authority = authorityAndPath.substringBefore('/').substringBefore('?').substringBefore('#')

        if ('@' in authority) return false
        if (':' in authority) {
            val portString = authority.substringAfter(':')
            if (portString != "443") return false
        }

        val host = authority.substringBefore(':').lowercase()
        if (host.isEmpty()) return false

        return host == "play.google.com" ||
            host == "github.com" ||
            host.endsWith(".github.com") ||
            host == "ut.edu.vn" ||
            host.endsWith(".ut.edu.vn") ||
            host == "uth.edu.vn" ||
            host.endsWith(".uth.edu.vn")
    }

    return false
}

/**
 * Đọc trạng thái gate đã lưu từ SharedPreferences cục bộ.
 */
internal fun loadSavedGate(prefs: SharedPreferences): SavedGate {
    if (!prefs.contains("has_saved_state")) return SavedGate()
    val maintenance = runCatching { prefs.getBoolean("maintenance_mode", false) }.getOrDefault(false)
    val minVersion = runCatching { prefs.getString("force_update_min_version", "1.0.0") }.getOrNull() ?: "1.0.0"
    val url = runCatching { prefs.getString("update_url", "") }.getOrNull() ?: ""
    val hasState = runCatching { prefs.getBoolean("has_saved_state", true) }.getOrDefault(true)
    return SavedGate(
        maintenanceMode = maintenance,
        forceUpdateMinVersion = minVersion,
        updateUrl = url,
        hasSavedState = hasState,
    )
}

internal fun loadSavedGate(context: Context): SavedGate {
    val prefs = context.getSharedPreferences("uth_remote_config_gate", Context.MODE_PRIVATE)
    return loadSavedGate(prefs)
}

/**
 * Lưu trạng thái gate mới vào SharedPreferences cục bộ.
 */
internal fun saveGate(prefs: SharedPreferences, gate: SavedGate) {
    prefs.edit()
        .putBoolean("maintenance_mode", gate.maintenanceMode)
        .putString("force_update_min_version", gate.forceUpdateMinVersion)
        .putString("update_url", gate.updateUrl)
        .putBoolean("has_saved_state", true)
        .apply()
}

internal fun saveGate(context: Context, gate: SavedGate) {
    val prefs = context.getSharedPreferences("uth_remote_config_gate", Context.MODE_PRIVATE)
    saveGate(prefs, gate)
}

/**
 * Kết hợp trạng thái gate đã lưu và giá trị remote mới thu thập được.
 *
 * Khi [fetchSucceeded] là true, sử dụng giá trị remote thu thập được (bất kể từ nguồn
 * REMOTE hay DEFAULT) và cập nhật lại [SavedGate].
 * Khi [fetchSucceeded] là false (do lỗi mạng hoặc throttle), ưu tiên giữ trạng thái đã lưu
 * [SavedGate] để duy trì việc chặn truy cập đối với phiên bản cũ hoặc ứng dụng đang bảo trì.
 */
internal fun resolveGate(
    saved: SavedGate,
    fetchSucceeded: Boolean,
    remoteMaintenance: Boolean,
    remoteMinVersion: String,
    remoteUpdateUrl: String = "",
    currentVersion: String,
): Pair<AppAvailability, SavedGate> {
    val maintenance = if (fetchSucceeded) {
        remoteMaintenance
    } else if (saved.hasSavedState) {
        saved.maintenanceMode
    } else {
        false
    }

    val minVersion = if (fetchSucceeded) {
        remoteMinVersion
    } else if (saved.hasSavedState) {
        saved.forceUpdateMinVersion
    } else {
        "1.0.0"
    }

    val updateUrl = if (fetchSucceeded) {
        remoteUpdateUrl.ifBlank { saved.updateUrl }
    } else if (saved.hasSavedState) {
        saved.updateUrl
    } else {
        ""
    }

    val state = availability(maintenance, currentVersion, minVersion)
    val newSaved = SavedGate(
        maintenanceMode = maintenance,
        forceUpdateMinVersion = minVersion,
        updateUrl = updateUrl,
        hasSavedState = true,
    )
    return Pair(state, newSaved)
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
internal fun AvailabilityScreen(
    state: AppAvailability,
    updateUrl: String = UPDATE_URL,
    isRetrying: Boolean = false,
    onRetry: () -> Unit = {},
) {
    val context = LocalContext.current
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(UthDimens.Spacing.Xl),
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
                modifier = Modifier.padding(top = UthDimens.Spacing.Md),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Xl))
            if (state == AppAvailability.MAINTENANCE || state == AppAvailability.UPDATE_REQUIRED) {
                Button(
                    onClick = onRetry,
                    enabled = !isRetrying,
                    modifier = Modifier.height(UthDimens.Size.ButtonHeight),
                ) {
                    if (isRetrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(UthDimens.Size.ProgressInline),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = UthDimens.Size.ProgressInlineStroke,
                        )
                    } else {
                        Text(
                            text = stringResource(
                                if (state == AppAvailability.MAINTENANCE) {
                                    R.string.availability_retry_action
                                } else {
                                    R.string.availability_retry_action
                                },
                            ),
                        )
                    }
                }
            }
            val activeUrl = updateUrl.ifBlank { UPDATE_URL }
            if (state == AppAvailability.UPDATE_REQUIRED && isValidUpdateUrl(activeUrl)) {
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl.trim()))
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.availability_update_failed),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.height(UthDimens.Size.ButtonHeight),
                ) {
                    Text(text = stringResource(R.string.availability_update_action))
                }
            }
        }
    }
}

@Preview(name = "Maintenance Screen Light", showBackground = true)
@Composable
internal fun AvailabilityScreenMaintenancePreview() {
    UthPreview {
        AvailabilityScreen(state = AppAvailability.MAINTENANCE)
    }
}

@Preview(name = "Update Required Screen Dark", showBackground = true)
@Composable
internal fun AvailabilityScreenUpdateDarkPreview() {
    UthPreview(darkTheme = true) {
        AvailabilityScreen(state = AppAvailability.UPDATE_REQUIRED)
    }
}

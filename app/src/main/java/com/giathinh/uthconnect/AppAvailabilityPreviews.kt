package com.giathinh.uthconnect

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

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

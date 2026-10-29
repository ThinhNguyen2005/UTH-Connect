package com.giathinh.uthconnect.presentation.schedule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

/**
 * Tab Thời khóa biểu — khung chờ cho module Phase 2 (`docs/Design.md` mục 3).
 * Người nhận branch `feature/timetable-<tên>` thay màn hình này bằng màn hình thật.
 */
@Composable
fun ScheduleScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        UthEmptyState(
            title = stringResource(R.string.schedule_wip_title),
            body = stringResource(R.string.schedule_wip_body),
            icon = Icons.Filled.DateRange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScheduleScreenPreview() = UthPreview { ScheduleScreen() }

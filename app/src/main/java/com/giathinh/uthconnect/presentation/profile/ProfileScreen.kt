package com.giathinh.uthconnect.presentation.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview

/**
 * Tab Cá nhân — chờ module xác thực email trường (`Design.md` mục 6).
 * Vé của tôi / điểm danh của tôi sẽ nằm trong màn hình này sau khi đăng nhập.
 */
@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        UthEmptyState(
            title = stringResource(R.string.profile_wip_title),
            body = stringResource(R.string.profile_wip_body),
            icon = Icons.Filled.Person,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() = UthPreview { ProfileScreen() }

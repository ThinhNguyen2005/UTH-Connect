@file:Suppress("FunctionNaming")

package com.giathinh.uthconnect.presentation.utilities.campuses

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme

private fun dialPhoneNumber(context: Context, phoneNumber: String) {
    val cleanNumber = phoneNumber.replace(" ", "")
    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$cleanNumber") }
    context.startActivity(intent)
}

private fun openDirections(context: Context, address: String) {
    val intent =
        Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("geo:0,0?q=${Uri.encode(address)}") }
    context.startActivity(intent)
}

@Composable
private fun CampusCard(campus: CampusInfo, onCall: () -> Unit, onDirections: () -> Unit) {
    UthCard(modifier = Modifier.fillMaxWidth().padding(bottom = UthDimens.Spacing.Md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Text(
                campus.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(campus.address, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            campus.note,
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        ) {
            FilledTonalButton(
                onClick = onCall,
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    Icons.Filled.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Xs))
                Text(stringResource(R.string.campuses_call_action))
            }
            OutlinedButton(
                onClick = onDirections,
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Xs))
                Text(stringResource(R.string.campuses_directions_action))
            }
        }
    }
}

@Composable
private fun DepartmentCard(department: DepartmentInfo, onCall: () -> Unit) {
    UthCard(modifier = Modifier.fillMaxWidth().padding(bottom = UthDimens.Spacing.Sm)) {
        Text(
            department.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = "Vị trí: ${department.location}",
            style = MaterialTheme.typography.bodySmall,
            color = UthTheme.extendedColors.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Hotline: ${department.hotline}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            OutlinedButton(onClick = onCall) { Text(stringResource(R.string.campuses_call_action)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "LongMethod")
fun CampusesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: CampusesUiState = CampusesUiState(),
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.campuses_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = UthDimens.Spacing.Xl)
        ) {
            Spacer(Modifier.height(UthDimens.Spacing.Md))
            Text(
                text = stringResource(R.string.campuses_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = UthTheme.extendedColors.textSecondary,
            )

            // Danh sách cơ sở
            UthSectionHeader(title = stringResource(R.string.campuses_list_header))

            uiState.campuses.forEach { campus ->
                CampusCard(
                    campus = campus,
                    onCall = { dialPhoneNumber(context, campus.hotline) },
                    onDirections = { openDirections(context, campus.address) },
                )
            }

            // Danh bạ phòng ban
            UthSectionHeader(title = stringResource(R.string.campuses_departments_header))

            uiState.departments.forEach { department ->
                DepartmentCard(
                    department = department,
                    onCall = { dialPhoneNumber(context, department.hotline) },
                )
            }

            Spacer(Modifier.height(UthDimens.Spacing.Xl))
        }
    }
}

@Preview(name = "CampusesScreen Light", showBackground = true)
@Composable
private fun CampusesScreenPreview() {
    UthPreview { CampusesScreen(onBack = {}) }
}

@Preview(name = "CampusesScreen Dark", showBackground = true)
@Composable
private fun CampusesScreenDarkPreview() {
    UthPreview(darkTheme = true) { CampusesScreen(onBack = {}) }
}

package com.giathinh.uthconnect.feature.profile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.UthDimens
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.ui.theme.UthConnectTheme

/** Profile UI only reads the authenticated StudentProfile passed down from the shell. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    student: StudentProfile,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(UthDimens.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = UthDimens.pageMaxWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StudentAvatar(fullName = student.fullName)
                Spacer(Modifier.height(UthDimens.regular))
                Text(
                    student.fullName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("profile_full_name"),
                )
                Spacer(Modifier.height(UthDimens.small))
                Text(
                    stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(UthDimens.small))
                Text(
                    stringResource(R.string.profile_demo_badge),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(UthDimens.large))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(UthDimens.corner),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.fillMaxWidth().padding(UthDimens.regular)) {
                        ProfileDetail(stringResource(R.string.profile_full_name), student.fullName)
                        HorizontalDivider()
                        ProfileDetail(stringResource(R.string.profile_student_id), student.studentId)
                        HorizontalDivider()
                        ProfileDetail(stringResource(R.string.profile_email), student.email)
                        HorizontalDivider()
                        ProfileDetail(stringResource(R.string.profile_class), student.className)
                        HorizontalDivider()
                        ProfileDetail(stringResource(R.string.profile_major), student.major)
                        HorizontalDivider()
                        ProfileDetail(stringResource(R.string.profile_faculty), student.faculty)
                    }
                }
                Spacer(Modifier.height(UthDimens.large))
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().height(UthDimens.xLarge * 1.75f)
                        .testTag("logout_button"),
                    shape = RoundedCornerShape(UthDimens.fieldCorner),
                ) {
                    Icon(Icons.Default.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(UthDimens.regular))
                    Text(stringResource(R.string.logout_button))
                }
            }
        }
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = UthDimens.medium),
        verticalArrangement = Arrangement.spacedBy(UthDimens.xSmall),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}

internal val PreviewStudent = StudentProfile(
    studentId = "2023040123", email = "demo@ut.edu.vn", fullName = "Nguyễn Minh Anh",
    className = "CN23A", major = "Công nghệ thông tin", faculty = "Khoa Công nghệ thông tin",
)

@Preview(name = "Profile Light", showBackground = true)
@Composable
private fun ProfileLightPreview() {
    UthConnectTheme(darkTheme = false) {
        ProfileScreen(student = PreviewStudent, onBack = {}, onSignOut = {})
    }
}

@Preview(name = "Profile Dark", showBackground = true, fontScale = 1.3f)
@Composable
private fun ProfileDarkPreview() {
    UthConnectTheme(darkTheme = true) {
        ProfileScreen(student = PreviewStudent, onBack = {}, onSignOut = {})
    }
}

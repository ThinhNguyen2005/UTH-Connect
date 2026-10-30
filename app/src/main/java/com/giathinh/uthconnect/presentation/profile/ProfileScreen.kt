package com.giathinh.uthconnect.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.StudentProfile

/** Avatar chữ viết tắt có nội dung mô tả; chưa cần dịch vụ ảnh để đúng phạm vi. */
@Composable
private fun StudentAvatar(student: StudentProfile, modifier: Modifier = Modifier) {
    val initials = student.displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .takeLast(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    Box(
        modifier = modifier.size(UthDimens.Size.AvatarLarge)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials.ifEmpty { "SV" }, color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .padding(horizontal = UthDimens.Spacing.Xl)) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        Text(stringResource(R.string.auth_profile_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        when {
            uiState.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.failure == ProfileFailure.LoadFailed -> UthEmptyState(
                stringResource(R.string.auth_profile_error),
                stringResource(R.string.auth_profile_try_again),
            )
            uiState.student == null -> UthEmptyState(
                stringResource(R.string.auth_profile_empty),
                stringResource(R.string.auth_profile_empty_body),
            )
            else -> ProfileContent(
                student = uiState.student,
                loggingOut = uiState.isLoggingOut,
                failure = uiState.failure,
                onLogout = { onAction(ProfileAction.Logout) },
            )
        }
    }
}

@Composable
private fun ProfileContent(
    student: StudentProfile,
    loggingOut: Boolean,
    failure: ProfileFailure?,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        StudentAvatar(student)
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Text(student.displayName, style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center, modifier = Modifier.testTag("profile_name"))
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(stringResource(R.string.auth_demo_profile), style = MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(UthDimens.Spacing.Xl))
    UthCard(modifier = Modifier.fillMaxWidth()) {
        ProfileRow(stringResource(R.string.auth_profile_name), student.displayName)
        ProfileRow(stringResource(R.string.auth_profile_student_id), student.studentId)
        ProfileRow(stringResource(R.string.auth_profile_email), student.email)
        ProfileRow(stringResource(R.string.auth_profile_class), student.className)
        ProfileRow(stringResource(R.string.auth_profile_major), student.major)
        ProfileRow(stringResource(R.string.auth_profile_faculty), student.faculty)
    }
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    if (failure == ProfileFailure.LogoutFailed) {
        Text(stringResource(R.string.auth_logout_failed), color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
    }
    OutlinedButton(onClick = onLogout, enabled = !loggingOut,
        modifier = Modifier.fillMaxWidth().height(UthDimens.Size.ButtonHeight).testTag("logout_button")) {
        Text(stringResource(R.string.auth_logout))
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = UthDimens.Spacing.Sm),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private val DemoStudent = StudentProfile("2023040123", "demo@ut.edu.vn", "Nguyễn Minh Anh",
    "CN23A", "Công nghệ thông tin", "Khoa Công nghệ thông tin")

@Composable
private fun ProfilePreviewState(state: ProfileUiState, dark: Boolean = false) =
    UthPreview(darkTheme = dark) { ProfileScreen(state, onAction = {}) }

@Preview(name = "Profile content light", showBackground = true)
@Composable private fun ProfileContentLight() = ProfilePreviewState(ProfileUiState(false, DemoStudent))
@Preview(name = "Profile content dark", showBackground = true)
@Composable private fun ProfileContentDark() = ProfilePreviewState(ProfileUiState(false, DemoStudent), true)
@Preview(name = "Profile loading light", showBackground = true)
@Composable private fun ProfileLoadingLight() = ProfilePreviewState(ProfileUiState())
@Preview(name = "Profile loading dark", showBackground = true)
@Composable private fun ProfileLoadingDark() = ProfilePreviewState(ProfileUiState(), true)
@Preview(name = "Profile empty light", showBackground = true)
@Composable private fun ProfileEmptyLight() = ProfilePreviewState(ProfileUiState(false))
@Preview(name = "Profile empty dark", showBackground = true)
@Composable private fun ProfileEmptyDark() = ProfilePreviewState(ProfileUiState(false), true)
@Preview(name = "Profile error light", showBackground = true)
@Composable private fun ProfileErrorLight() = ProfilePreviewState(ProfileUiState(false, failure = ProfileFailure.LoadFailed))
@Preview(name = "Profile error dark", showBackground = true)
@Composable private fun ProfileErrorDark() = ProfilePreviewState(ProfileUiState(false, failure = ProfileFailure.LoadFailed), true)

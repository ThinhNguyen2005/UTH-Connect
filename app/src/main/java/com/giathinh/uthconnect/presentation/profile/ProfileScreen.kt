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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthEmptyState
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthSectionHeader
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.model.UserRole
import com.giathinh.uthconnect.domain.validation.ContactFieldError
import com.giathinh.uthconnect.domain.validation.PasswordError

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenTickets: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Xl),
    ) {
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        when (uiState.page) {
            ProfilePage.ChangePassword -> PasswordChangeContent(uiState, onAction)

            ProfilePage.EditContact -> ContactEditContent(uiState, onAction)

            ProfilePage.Overview -> {
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    uiState.failure == ProfileFailure.LoadFailed -> UthEmptyState(
                        stringResource(R.string.auth_profile_error),
                        stringResource(R.string.auth_profile_try_again),
                        actionLabel = stringResource(R.string.auth_profile_retry),
                        onAction = { onAction(ProfileAction.RetryLoad) },
                    )

                    uiState.student == null -> UthEmptyState(
                        stringResource(R.string.auth_profile_empty),
                        stringResource(R.string.auth_profile_empty_body),
                    )

                    else -> ProfileOverview(uiState, onAction, onOpenTickets)
                }
            }
        }
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
    }
    if (uiState.confirmLogout) {
        AlertDialog(
            onDismissRequest = { onAction(ProfileAction.CancelLogout) },
            title = { Text(stringResource(R.string.auth_logout_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.auth_logout_confirm_body))
                    if (uiState.failure == ProfileFailure.LogoutFailed) {
                        Spacer(Modifier.height(UthDimens.Spacing.Sm))
                        Text(
                            stringResource(R.string.auth_logout_failed),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.testTag("logout_retry_error"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(
                            if (uiState.failure == ProfileFailure.LogoutFailed) {
                                ProfileAction.RetryLogout
                            } else {
                                ProfileAction.ConfirmLogout
                            },
                        )
                    },
                    enabled = !uiState.isLoggingOut,
                    modifier = Modifier.testTag("logout_retry_or_confirm"),
                ) {
                    Text(
                        stringResource(
                            if (uiState.failure == ProfileFailure.LogoutFailed) {
                                R.string.auth_logout_retry
                            } else {
                                R.string.auth_logout
                            },
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ProfileAction.CancelLogout) }, enabled = !uiState.isLoggingOut) {
                    Text(stringResource(R.string.auth_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileOverview(
    state: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onOpenTickets: () -> Unit,
) {
    val student = state.student ?: return
    UserProfileHeader(student)
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    CompactTicketsRow(
        onClick = onOpenTickets,
        modifier = Modifier.testTag("profile_open_tickets"),
    )
    PersonalInfoSection(student)
    SchoolInfoSection(student)
    ContactInfoSection(student)
    ProfileActions(state, onAction)
}

@Composable
private fun UserProfileHeader(
    student: StudentProfile,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudentAvatar(student)
        Spacer(Modifier.width(UthDimens.Spacing.Lg))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
        ) {
            Text(
                text = student.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("profile_name"),
            )
            val studentDesc = listOf(student.studentId, student.className)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (studentDesc.isNotBlank()) {
                Text(
                    text = studentDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (student.major.isNotBlank()) {
                Text(
                    text = student.major,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ProfileBadges(student)
        }
    }
}

@Composable
private fun ProfileBadges(student: StudentProfile) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (student.uthVerified) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("profile_verified_badge"),
            ) {
                Text(
                    stringResource(R.string.auth_verified_demo_badge),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Xs,
                    ),
                )
            }
        } else {
            Text(
                stringResource(R.string.auth_unverified_badge),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.testTag("profile_unverified_badge"),
            )
        }
        if (student.role == UserRole.ORGANIZER) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.testTag("profile_organizer_badge"),
            ) {
                Text(
                    stringResource(R.string.auth_organizer_demo_badge),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(
                        horizontal = UthDimens.Spacing.Sm,
                        vertical = UthDimens.Spacing.Xs,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CompactTicketsRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UthCard(
        modifier = modifier.fillMaxWidth().heightIn(min = UthDimens.Size.MinTouchTarget),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(UthDimens.Size.UtilityIconContainer)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile_tickets),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tab_tickets),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.profile_tickets_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PersonalInfoSection(student: StudentProfile) {
    UthSectionHeader(stringResource(R.string.profile_section_personal))
    UthCard(modifier = Modifier.fillMaxWidth()) {
        ProfileRow(stringResource(R.string.auth_profile_name), student.displayName)
        ProfileRow(stringResource(R.string.auth_profile_email), student.email)
    }
}

@Composable
private fun SchoolInfoSection(student: StudentProfile, showNote: Boolean = false) {
    UthSectionHeader(stringResource(R.string.profile_section_school))
    UthCard(modifier = Modifier.fillMaxWidth()) {
        ProfileRow(stringResource(R.string.auth_profile_student_id), student.studentId)
        ProfileRow(stringResource(R.string.auth_profile_class), student.className)
        ProfileRow(stringResource(R.string.auth_profile_major), student.major)
        ProfileRow(stringResource(R.string.auth_profile_faculty), student.faculty)
        if (showNote) {
            Spacer(Modifier.height(UthDimens.Spacing.Xs))
            Text(
                stringResource(R.string.profile_school_info_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContactInfoSection(student: StudentProfile) {
    UthSectionHeader(stringResource(R.string.profile_section_contact))
    UthCard(modifier = Modifier.fillMaxWidth()) {
        ProfileRow(
            stringResource(R.string.auth_contact_phone),
            if (student.phoneNumber.isBlank()) stringResource(R.string.auth_contact_not_set) else student.phoneNumber,
        )
        ProfileRow(
            stringResource(R.string.auth_contact_email),
            if (student.personalEmail.isBlank()) stringResource(R.string.auth_contact_not_set) else student.personalEmail,
        )
        ProfileRow(
            stringResource(R.string.auth_contact_address),
            if (student.currentAddress.isBlank()) stringResource(R.string.auth_contact_not_set) else student.currentAddress,
        )
    }
}

@Composable
private fun ProfileActions(state: ProfileUiState, onAction: (ProfileAction) -> Unit) {
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    OutlinedButton(
        onClick = { onAction(ProfileAction.OpenContact) },
        modifier = Modifier.fillMaxWidth().testTag("profile_edit_contact"),
    ) { Text(stringResource(R.string.auth_contact_edit_title)) }
    Spacer(Modifier.height(UthDimens.Spacing.Sm))
    OutlinedButton(
        onClick = { onAction(ProfileAction.OpenPassword) },
        modifier = Modifier.fillMaxWidth().testTag("profile_change_password"),
    ) { Text(stringResource(R.string.auth_password_change_title)) }
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    DisplayFeedback(state)
    OutlinedButton(
        onClick = { onAction(ProfileAction.RequestLogout) },
        enabled = !state.isLoggingOut,
        modifier = Modifier.fillMaxWidth().height(UthDimens.Size.ButtonHeight).testTag("logout_button"),
    ) {
        Text(stringResource(R.string.auth_logout))
    }
}

@Composable
private fun DisplayFeedback(state: ProfileUiState) {
    val noticeRes = when (state.notice) {
        ProfileNotice.PasswordChanged -> R.string.auth_password_changed
        ProfileNotice.ContactSaved -> R.string.auth_contact_saved
        ProfileNotice.BiometricEnabled -> R.string.auth_biometric_enabled
        ProfileNotice.BiometricDisabled -> R.string.auth_biometric_disabled
        null -> null
    }
    noticeRes?.let {
        Text(
            stringResource(it),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag("profile_notice"),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
    }
    val errorRes = when (state.failure) {
        ProfileFailure.LogoutFailed -> R.string.auth_logout_failed
        ProfileFailure.SaveFailed -> R.string.auth_save_failed
        ProfileFailure.BiometricFailed -> R.string.auth_biometric_error
        ProfileFailure.LoadFailed -> R.string.auth_profile_error
        null -> null
    }
    errorRes?.let {
        Text(
            stringResource(it),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.testTag("profile_error"),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
    }
}

@Composable
private fun PasswordChangeContent(state: ProfileUiState, onAction: (ProfileAction) -> Unit) {
    val form = state.passwordForm
    Text(stringResource(R.string.auth_password_change_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(UthDimens.Spacing.Sm))
    Text(stringResource(R.string.auth_password_policy), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    UthCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = form.current,
            onValueChange = { onAction(ProfileAction.OldPasswordChanged(it)) },
            label = { Text(stringResource(R.string.auth_password_current)) },
            modifier = Modifier.fillMaxWidth().testTag("password_current"),
            enabled = !state.isSaving,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = form.errors.oldError != null || form.incorrectCurrent,
            supportingText = {
                if (form.incorrectCurrent) {
                    Text(stringResource(R.string.auth_password_wrong_current))
                } else {
                    form.errors.oldError?.let { Text(stringResource(passwordErrorRes(it))) }
                }
            },
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        OutlinedTextField(
            value = form.updated,
            onValueChange = { onAction(ProfileAction.NewPasswordChanged(it)) },
            label = { Text(stringResource(R.string.auth_password_new)) },
            modifier = Modifier.fillMaxWidth().testTag("password_new"),
            enabled = !state.isSaving,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = form.errors.newError != null,
            supportingText = { form.errors.newError?.let { Text(stringResource(passwordErrorRes(it))) } },
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        OutlinedTextField(
            value = form.confirmed,
            onValueChange = { onAction(ProfileAction.ConfirmPasswordChanged(it)) },
            label = { Text(stringResource(R.string.auth_password_confirm)) },
            modifier = Modifier.fillMaxWidth().testTag("password_confirm"),
            enabled = !state.isSaving,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = form.errors.confirmationError != null,
            supportingText = { form.errors.confirmationError?.let { Text(stringResource(passwordErrorRes(it))) } },
        )
    }
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    DisplayFeedback(state)
    UthPrimaryButton(
        text = stringResource(R.string.auth_save),
        onClick = { onAction(ProfileAction.SubmitPassword) },
        enabled = !state.isSaving,
        loading = state.isSaving,
        modifier = Modifier.fillMaxWidth().testTag("password_save"),
    )
    Spacer(Modifier.height(UthDimens.Spacing.Sm))
    OutlinedButton(
        onClick = { onAction(ProfileAction.Back) },
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.auth_back))
    }
}

private fun passwordErrorRes(error: PasswordError): Int = when (error) {
    PasswordError.Required -> R.string.auth_required
    PasswordError.Weak -> R.string.auth_password_weak
    PasswordError.SameAsOld -> R.string.auth_password_same
    PasswordError.ConfirmationMismatch -> R.string.auth_password_mismatch
}

@Composable
private fun ContactEditContent(state: ProfileUiState, onAction: (ProfileAction) -> Unit) {
    val form = state.contactForm
    val student = state.student
    Text(stringResource(R.string.auth_contact_edit_title), style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(UthDimens.Spacing.Sm))
    if (student != null) {
        PersonalInfoSection(student)
        SchoolInfoSection(student, showNote = true)
    }
    UthSectionHeader(stringResource(R.string.profile_section_contact))
    ContactEditFields(form = form, isSaving = state.isSaving, onAction = onAction)
    Spacer(Modifier.height(UthDimens.Spacing.Lg))
    DisplayFeedback(state)
    UthPrimaryButton(
        text = stringResource(R.string.auth_save),
        onClick = { onAction(ProfileAction.SubmitContact) },
        enabled = !state.isSaving,
        loading = state.isSaving,
        modifier = Modifier.fillMaxWidth().testTag("contact_save"),
    )
    Spacer(Modifier.height(UthDimens.Spacing.Sm))
    OutlinedButton(
        onClick = { onAction(ProfileAction.Back) },
        enabled = !state.isSaving,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.auth_back))
    }
}

@Composable
private fun ContactEditFields(
    form: ContactForm,
    isSaving: Boolean,
    onAction: (ProfileAction) -> Unit,
) {
    UthCard(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = form.phone,
            onValueChange = { onAction(ProfileAction.PhoneChanged(it)) },
            label = { Text(stringResource(R.string.auth_contact_phone)) },
            modifier = Modifier.fillMaxWidth().testTag("contact_phone"),
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = form.errors.phoneError != null,
            supportingText = { form.errors.phoneError?.let { Text(stringResource(contactErrorRes(it))) } },
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        OutlinedTextField(
            value = form.email,
            onValueChange = { onAction(ProfileAction.EmailChanged(it)) },
            label = { Text(stringResource(R.string.auth_contact_email)) },
            modifier = Modifier.fillMaxWidth().testTag("contact_email"),
            enabled = !isSaving,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = form.errors.emailError != null,
            supportingText = { form.errors.emailError?.let { Text(stringResource(contactErrorRes(it))) } },
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        OutlinedTextField(
            value = form.address,
            onValueChange = { onAction(ProfileAction.AddressChanged(it)) },
            label = { Text(stringResource(R.string.auth_contact_address)) },
            modifier = Modifier.fillMaxWidth().testTag("contact_address"),
            enabled = !isSaving,
            minLines = 2,
            maxLines = 3,
            isError = form.errors.addressError != null,
            supportingText = { form.errors.addressError?.let { Text(stringResource(contactErrorRes(it))) } },
        )
    }
}

private fun contactErrorRes(error: ContactFieldError): Int = when (error) {
    ContactFieldError.InvalidPhone -> R.string.auth_contact_invalid_phone
    ContactFieldError.InvalidEmail -> R.string.auth_contact_invalid_email
    ContactFieldError.AddressTooLong -> R.string.auth_contact_invalid_address
}

@Composable
private fun StudentAvatar(student: StudentProfile, modifier: Modifier = Modifier) {
    val initials = student.displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .takeLast(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    Box(
        modifier = modifier
            .size(UthDimens.Size.AvatarLarge)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.ifEmpty { "SV" },
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = UthDimens.Spacing.Sm),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

private val PreviewStudent = StudentProfile(
    "2023040123",
    "student@ut.edu.vn",
    "Nguyễn Minh Anh",
    "CN23A",
    "Công nghệ thông tin",
    "Khoa Công nghệ thông tin",
)

@Composable
private fun PreviewProfile(state: ProfileUiState, dark: Boolean = false) = UthPreview(darkTheme = dark) { ProfileScreen(state, onAction = {}) }

@Preview(name = "Profile content light", showBackground = true)
@Composable
private fun ProfileContentLight() = PreviewProfile(ProfileUiState(false, PreviewStudent))

@Preview(name = "Profile content dark", showBackground = true)
@Composable
private fun ProfileContentDark() = PreviewProfile(ProfileUiState(false, PreviewStudent), true)

@Preview(name = "Profile organizer light", showBackground = true)
@Composable
private fun ProfileOrganizerLight() = PreviewProfile(ProfileUiState(false, PreviewStudent.copy(role = UserRole.ORGANIZER, uthVerified = true)))

@Preview(name = "Profile password light", showBackground = true)
@Composable
private fun ProfilePasswordLight() = PreviewProfile(ProfileUiState(false, PreviewStudent, page = ProfilePage.ChangePassword))

@Preview(name = "Profile password dark", showBackground = true)
@Composable
private fun ProfilePasswordDark() = PreviewProfile(ProfileUiState(false, PreviewStudent, page = ProfilePage.ChangePassword), true)

@Preview(name = "Profile contact light", showBackground = true)
@Composable
private fun ProfileContactLight() = PreviewProfile(ProfileUiState(false, PreviewStudent, page = ProfilePage.EditContact))

@Preview(name = "Profile contact dark", showBackground = true)
@Composable
private fun ProfileContactDark() = PreviewProfile(ProfileUiState(false, PreviewStudent, page = ProfilePage.EditContact), true)

@Preview(name = "Profile empty light", showBackground = true)
@Composable
private fun ProfileEmptyLight() = PreviewProfile(ProfileUiState(false))

@Preview(name = "Profile logout confirmation", showBackground = true)
@Composable
private fun ProfileLogoutDialog() = PreviewProfile(ProfileUiState(false, PreviewStudent, confirmLogout = true))

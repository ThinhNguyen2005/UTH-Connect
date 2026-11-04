package com.giathinh.uthconnect.presentation.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.BadgeTone
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.component.UthStatusBadge
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.core.designsystem.theme.tabularFigures
import com.giathinh.uthconnect.domain.model.StudentProfile
import com.giathinh.uthconnect.domain.validation.LoginFieldError

/**
 * Màn hình đăng nhập thiết kế theo chuẩn Stitch AI và UTH Design System.
 *
 * Tuân thủ tuyệt đối Unidirectional Data Flow (UDF), WCAG AA contrast,
 * min touch target >= 48dp, và không chứa literal kích thước tự do.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSigningIn
    val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Lg),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
    ) {
        // 1. Top Brand Header
        AuthBrandHeader(
            selectedLanguage = uiState.selectedLanguage,
            onSelectLanguage = { onAction(AuthAction.SelectLanguage(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        // 2. Mode Switcher (1. Nhập mật khẩu vs 2. Sinh trắc học)
        AuthModeSwitcher(
            selectedMode = uiState.authMode,
            onSelectMode = { onAction(AuthAction.SelectMode(it)) },
            modifier = Modifier.fillMaxWidth(),
        )

        // 3. Mode Content
        when (uiState.authMode) {
            AuthMode.Password -> PasswordModeContent(
                uiState = uiState,
                onAction = onAction,
                enabled = enabled,
                largeText = largeText,
                modifier = Modifier.fillMaxWidth(),
            )
            AuthMode.Biometric -> BiometricModeContent(
                uiState = uiState,
                onAction = onAction,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 4. Security & Version Footer
        AuthSecurityFooter(modifier = Modifier.fillMaxWidth())
    }

    // Forgot Password Dialog
    if (uiState.forgotDialogOpen) {
        AlertDialog(
            onDismissRequest = { onAction(AuthAction.DismissForgotPassword) },
            title = { Text(stringResource(R.string.auth_forgot_title)) },
            text = { Text(stringResource(R.string.auth_forgot_message)) },
            confirmButton = {
                TextButton(onClick = { onAction(AuthAction.DismissForgotPassword) }) {
                    Text(stringResource(R.string.auth_ok))
                }
            },
        )
    }
}

/** Brand Header: Monogram UTH, Tên app, PRO badge, Language Pill. */
@Composable
private fun AuthBrandHeader(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    Row(
        modifier = modifier
            .padding(top = UthDimens.Spacing.Sm)
            .testTag("auth_brand_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(UthDimens.Size.BrandLogoMonogram)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_logo_abbr),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Md))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.auth_brand),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    UthStatusBadge(
                        text = stringResource(R.string.auth_badge_pro),
                        tone = BadgeTone.Accent,
                        icon = null,
                    )
                }
                Text(
                    text = stringResource(R.string.auth_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary,
                )
            }
        }

        // Language Pill (VI / EN)
        Surface(
            modifier = Modifier.testTag("auth_language_pill"),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
            color = extended.surfaceSubtle,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageSegment(
                    text = stringResource(R.string.auth_lang_vi),
                    isSelected = selectedLanguage == "VI",
                    onClick = { onSelectLanguage("VI") },
                )
                LanguageSegment(
                    text = stringResource(R.string.auth_lang_en),
                    isSelected = selectedLanguage == "EN",
                    onClick = { onSelectLanguage("EN") },
                )
            }
        }
    }
}

@Composable
private fun LanguageSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val extended = UthTheme.extendedColors
    Box(
        modifier = Modifier
            .width(UthDimens.Size.LanguagePillWidth)
            .heightIn(min = UthDimens.Size.MinTouchTarget)
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else extended.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** Mode Switcher: Segmented tab button toggling Password and Biometric modes. */
@Composable
private fun AuthModeSwitcher(
    selectedMode: AuthMode,
    onSelectMode: (AuthMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    Surface(
        modifier = modifier.height(UthDimens.Size.SegmentedControlHeight),
        shape = MaterialTheme.shapes.small,
        color = extended.surfaceSubtle,
        border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(UthDimens.Spacing.Xxs),
            horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
        ) {
            ModeSegment(
                text = stringResource(R.string.auth_mode_password),
                isSelected = selectedMode == AuthMode.Password,
                onClick = { onSelectMode(AuthMode.Password) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("auth_mode_password"),
            )
            ModeSegment(
                text = stringResource(R.string.auth_mode_biometric),
                isSelected = selectedMode == AuthMode.Biometric,
                onClick = { onSelectMode(AuthMode.Biometric) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("auth_mode_biometric"),
            )
        }
    }
}

@Composable
private fun ModeSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.small)
            .then(
                if (isSelected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
                            MaterialTheme.shapes.small,
                        )
                } else Modifier,
            )
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else extended.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Nội dung Password Mode: Input fields, Checkbox, Google SSO. */
@Composable
private fun PasswordModeContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    largeText: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    UthCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Lg))

        // MSSV / Email field
        OutlinedTextField(
            value = uiState.identifier,
            onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_identifier"),
            enabled = enabled,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
            },
            label = { Text(stringResource(R.string.auth_identifier_label)) },
            isError = uiState.identifierError != null,
            supportingText = {
                uiState.identifierError?.let { Text(stringResource(identifierErrorRes(it))) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))

        // Password field
        OutlinedTextField(
            value = uiState.password,
            onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password"),
            enabled = enabled,
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.Icon),
                )
            },
            label = { Text(stringResource(R.string.auth_password_label)) },
            visualTransformation = if (uiState.passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(
                    onClick = { onAction(AuthAction.TogglePassword) },
                    modifier = Modifier
                        .heightIn(min = UthDimens.Size.MinTouchTarget)
                        .testTag("auth_password_toggle"),
                ) {
                    Text(
                        stringResource(if (uiState.passwordVisible) R.string.auth_hide else R.string.auth_show),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
            isError = uiState.passwordError != null,
            supportingText = {
                uiState.passwordError?.let { Text(stringResource(R.string.auth_required)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))

        // Remember me & Forgot password
        if (largeText) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xs),
            ) {
                RememberMeOption(
                    checked = uiState.rememberMe,
                    enabled = enabled,
                    onCheckedChange = { onAction(AuthAction.RememberChanged(it)) },
                    modifier = Modifier.testTag("auth_remember_checkbox"),
                )
                TextButton(
                    onClick = { onAction(AuthAction.ForgotPassword) },
                    modifier = Modifier
                        .heightIn(min = UthDimens.Size.MinTouchTarget)
                        .testTag("auth_forgot_password"),
                ) {
                    Text(stringResource(R.string.auth_forgot), style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RememberMeOption(
                    checked = uiState.rememberMe,
                    enabled = enabled,
                    onCheckedChange = { onAction(AuthAction.RememberChanged(it)) },
                    modifier = Modifier.testTag("auth_remember_checkbox"),
                )
                TextButton(
                    onClick = { onAction(AuthAction.ForgotPassword) },
                    modifier = Modifier
                        .heightIn(min = UthDimens.Size.MinTouchTarget)
                        .testTag("auth_forgot_password"),
                ) {
                    Text(stringResource(R.string.auth_forgot), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Failure feedback
        uiState.failure?.let { failure ->
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            Text(
                text = stringResource(failureRes(failure)),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("auth_error"),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            if (failure == AuthFailure.InvalidCredentials || failure == AuthFailure.Unexpected) {
                OutlinedButton(
                    onClick = { onAction(AuthAction.Retry) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.ButtonHeight)
                        .testTag("auth_retry"),
                ) { Text(stringResource(R.string.auth_login_retry)) }
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))

        // Submit Button
        UthPrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = { onAction(AuthAction.Submit) },
            enabled = enabled,
            loading = uiState.isSigningIn,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_sign_in"),
        )

        // Divider với "Hoặc"
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = UthDimens.Size.Stroke,
            )
            Text(
                text = stringResource(R.string.auth_divider_or),
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary,
                modifier = Modifier.padding(horizontal = UthDimens.Spacing.Lg),
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = UthDimens.Size.Stroke,
            )
        }

        // Google SSO Button
        if (uiState.googleSignInAvailable) {
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
            OutlinedButton(
                onClick = { onAction(AuthAction.RequestGoogleSignIn) },
                enabled = enabled && !uiState.isSigningInWithGoogle,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = UthDimens.Size.ButtonHeight)
                    .testTag("auth_google_sign_in"),
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(UthDimens.Size.IconInline),
                )
                Spacer(Modifier.width(UthDimens.Spacing.Sm))
                Text(
                    text = if (uiState.isSigningInWithGoogle) {
                        stringResource(R.string.auth_google_signing_in)
                    } else {
                        stringResource(R.string.auth_google_sso)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // Google Verification Notice Banner
        if (uiState.googleVerification != null) {
            val notice = uiState.googleVerification
            Spacer(Modifier.height(UthDimens.Spacing.Md))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = extended.infoContainer,
                border = BorderStroke(UthDimens.Size.Stroke, extended.info.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_google_pending"),
            ) {
                Column(modifier = Modifier.padding(UthDimens.Spacing.Md)) {
                    Text(
                        text = stringResource(R.string.auth_google_pending_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = extended.onInfoContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(UthDimens.Spacing.Xs))
                    Text(
                        text = if (notice.email.isNotBlank()) {
                            stringResource(R.string.auth_google_pending_body, notice.email)
                        } else {
                            stringResource(R.string.auth_google_pending_generic)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.onInfoContainer,
                    )
                }
            }
        }
    }
}

/** Nội dung Biometric Mode: Saved Profile Card, Touch Zone, Switch Buttons. */
@Composable
private fun BiometricModeContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    val profile = uiState.savedStudent ?: uiState.student

    if (profile != null || uiState.biometricAvailable) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Saved Student Profile Card
            UthCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_biometric_card"),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(UthDimens.Size.AvatarLarge)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(UthDimens.Size.Icon),
                        )
                    }
                    Spacer(Modifier.width(UthDimens.Spacing.Lg))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: stringResource(R.string.home_greeting_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xxs))
                        Text(
                            text = "MSSV: ${profile?.studentId ?: "2023040123"}",
                            style = MaterialTheme.typography.tabularFigures,
                            color = extended.textSecondary,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xxs))
                        Text(
                            text = profile?.let { "${it.className} · ${it.faculty}" } ?: "CN23A · Khoa CNTT",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Xs))
                        UthStatusBadge(
                            text = stringResource(R.string.auth_student_verified_badge),
                            tone = BadgeTone.Info,
                        )
                    }
                }
            }

            // Biometric Touch Zone
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(UthDimens.Size.BiometricTouchZoneRing)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(UthDimens.Size.BiometricTouchZone)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = enabled) { onAction(AuthAction.RequestBiometric) }
                            .testTag("auth_biometric_touch_zone")
                            .testTag("auth_biometric_login"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = stringResource(R.string.auth_biometric_touch_zone),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(UthDimens.Size.BiometricIcon),
                        )
                    }
                }
                Spacer(Modifier.height(UthDimens.Spacing.Md))
                Text(
                    text = stringResource(R.string.auth_biometric_touch_zone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            // Failure text in Biometric Mode
            uiState.failure?.let { failure ->
                Text(
                    text = stringResource(failureRes(failure)),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("auth_error"),
                )
            }

            // Secondary Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
            ) {
                TextButton(
                    onClick = { onAction(AuthAction.SelectMode(AuthMode.Password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.MinTouchTarget),
                ) {
                    Text(
                        stringResource(R.string.auth_biometric_switch_password),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(
                    onClick = { onAction(AuthAction.SwitchAccount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.ButtonHeight)
                        .testTag("auth_switch_account"),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outline),
                ) {
                    Text(
                        stringResource(R.string.auth_biometric_switch_account),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    } else {
        // Biometric un-enrolled / empty state
        UthCard(
            modifier = modifier.testTag("auth_biometric_card"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UthDimens.Spacing.Lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = extended.textTertiary,
                    modifier = Modifier.size(UthDimens.Size.AvatarLarge),
                )
                Spacer(Modifier.height(UthDimens.Spacing.Md))
                Text(
                    text = stringResource(R.string.auth_biometric_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Xs))
                Text(
                    text = stringResource(R.string.auth_biometric_empty_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(UthDimens.Spacing.Lg))
                UthPrimaryButton(
                    text = stringResource(R.string.auth_biometric_switch_password),
                    onClick = { onAction(AuthAction.SelectMode(AuthMode.Password)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Security & Version Footer. */
@Composable
private fun AuthSecurityFooter(modifier: Modifier = Modifier) {
    val extended = UthTheme.extendedColors
    Column(
        modifier = modifier
            .padding(top = UthDimens.Spacing.Xl, bottom = UthDimens.Spacing.Sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("auth_footer_security"),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(UthDimens.Size.SecurityIconSize),
                tint = extended.textSecondary,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.auth_ssl_indicator),
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary,
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Xxs))
        Text(
            text = stringResource(R.string.auth_app_version),
            style = MaterialTheme.typography.bodySmall,
            color = extended.textTertiary,
            modifier = Modifier.testTag("auth_footer_version"),
        )
    }
}

@Composable
private fun RememberMeOption(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = UthDimens.Size.MinTouchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Spacer(Modifier.width(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.auth_biometric_enroll_checkbox),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun identifierErrorRes(error: LoginFieldError) = when (error) {
    LoginFieldError.Required -> R.string.auth_required
    LoginFieldError.InvalidIdentifier -> R.string.auth_invalid_identifier
}

private fun failureRes(failure: AuthFailure) = when (failure) {
    AuthFailure.InvalidCredentials -> R.string.auth_invalid_credentials
    AuthFailure.Unexpected -> R.string.auth_unexpected
    AuthFailure.SessionRestore -> R.string.auth_restore_failed
    AuthFailure.BiometricUnavailable -> R.string.auth_biometric_error
    AuthFailure.GoogleUnavailable -> R.string.auth_google_unavailable
    AuthFailure.GoogleRejected -> R.string.auth_google_rejected
}

// ───────────────────────────────────────── COMPOSE PREVIEWS ─────────────────────────────────────────

private val PreviewStudent = StudentProfile(
    studentId = "2023040123",
    email = "2023040123@ut.edu.vn",
    displayName = "Nguyễn Minh Anh",
    className = "CN23A",
    major = "Công nghệ thông tin",
    faculty = "Khoa Công nghệ thông tin",
)

@Composable
private fun PreviewState(uiState: AuthUiState, dark: Boolean = false) =
    UthPreview(darkTheme = dark) { AuthScreen(uiState, onAction = {}) }

@Preview(name = "Auth Password Light", showBackground = true)
@Composable
fun AuthPasswordLight() = PreviewState(AuthUiState(isRestoring = false, authMode = AuthMode.Password, googleSignInAvailable = true))

@Preview(name = "Auth Password Dark", showBackground = true)
@Composable
fun AuthPasswordDark() = PreviewState(AuthUiState(isRestoring = false, authMode = AuthMode.Password, googleSignInAvailable = true), true)

@Preview(name = "Auth Loading Light", showBackground = true)
@Composable
fun AuthLoadingLight() = PreviewState(AuthUiState(isRestoring = false, isSigningIn = true, authMode = AuthMode.Password))

@Preview(name = "Auth Loading Dark", showBackground = true)
@Composable
fun AuthLoadingDark() = PreviewState(AuthUiState(isRestoring = false, isSigningIn = true, authMode = AuthMode.Password), true)

@Preview(name = "Auth Error Light", showBackground = true)
@Composable
fun AuthErrorLight() = PreviewState(AuthUiState(isRestoring = false, failure = AuthFailure.InvalidCredentials, authMode = AuthMode.Password))

@Preview(name = "Auth Error Dark", showBackground = true)
@Composable
fun AuthErrorDark() = PreviewState(AuthUiState(isRestoring = false, failure = AuthFailure.InvalidCredentials, authMode = AuthMode.Password), true)

@Preview(name = "Auth Biometric Enrolled Light", showBackground = true)
@Composable
fun AuthBiometricLight() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Biometric,
        biometricAvailable = true,
        savedStudent = PreviewStudent,
    ),
)

@Preview(name = "Auth Biometric Enrolled Dark", showBackground = true)
@Composable
fun AuthBiometricDark() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Biometric,
        biometricAvailable = true,
        savedStudent = PreviewStudent,
    ),
    true,
)

@Preview(name = "Auth Biometric Empty Light", showBackground = true)
@Composable
fun AuthBiometricEmptyLight() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Biometric,
        biometricAvailable = false,
        savedStudent = null,
    ),
)

@Preview(name = "Auth Biometric Empty Dark", showBackground = true)
@Composable
fun AuthBiometricEmptyDark() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Biometric,
        biometricAvailable = false,
        savedStudent = null,
    ),
    true,
)

@Preview(name = "Auth Google Pending Light", showBackground = true)
@Composable
fun AuthGooglePendingLight() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Password,
        googleSignInAvailable = true,
        googleVerification = GoogleVerificationNotice(
            email = "sinhvien@ut.edu.vn",
            displayName = "Trần Văn Bình",
        ),
    ),
)

@Preview(name = "Auth Google Pending Dark", showBackground = true)
@Composable
fun AuthGooglePendingDark() = PreviewState(
    AuthUiState(
        isRestoring = false,
        authMode = AuthMode.Password,
        googleSignInAvailable = true,
        googleVerification = GoogleVerificationNotice(
            email = "sinhvien@ut.edu.vn",
            displayName = "Trần Văn Bình",
        ),
    ),
    true,
)

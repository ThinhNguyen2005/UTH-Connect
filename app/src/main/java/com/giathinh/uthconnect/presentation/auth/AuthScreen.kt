package com.giathinh.uthconnect.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
 * Màn hình đăng nhập thiết kế theo chuẩn Modern Minimalist và UTH Design System.
 *
 * Hỗ trợ luồng Identifier-first giảm tải nhận thức, giao diện "Hello Again!"
 * tinh giản phân cấp thị giác, tuân thủ WCAG AA, min touch target >= 48dp.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSigningIn && !uiState.isSigningInWithGoogle
    val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale
    val extended = UthTheme.extendedColors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Lớp nền cong mỹ thuật nhẹ nhàng ở góc trên phải (Decorative Soft Ambient Shape)
        val ambientColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ambientColor,
                radius = size.width * 0.45f,
                center = Offset(x = size.width * 1.05f, y = size.height * -0.05f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Lg),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
        ) {
            // 1. Top Brand Header với Language Pill
            AuthBrandHeader(
                selectedLanguage = uiState.selectedLanguage,
                onSelectLanguage = { onAction(AuthAction.SelectLanguage(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Mode Switcher (1. Mật khẩu vs 2. Sinh trắc học)
            AuthModeSwitcher(
                selectedMode = uiState.authMode,
                onSelectMode = { onAction(AuthAction.SelectMode(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            // 3. Main Content
            when (uiState.authMode) {
                AuthMode.Password -> {
                    AnimatedContent(
                        targetState = uiState.screenStep,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "auth_screen_step_transition",
                    ) { step ->
                        when (step) {
                            AuthScreenStep.Password -> ModernPasswordContent(
                                uiState = uiState,
                                onAction = onAction,
                                enabled = enabled,
                                largeText = largeText,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuthScreenStep.Identifier -> ModernIdentifierContent(
                                uiState = uiState,
                                onAction = onAction,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuthScreenStep.FirstTimeSetup -> ModernFirstTimeSetupContent(
                                uiState = uiState,
                                onAction = onAction,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuthScreenStep.ResetPassword -> ModernResetPasswordContent(
                                uiState = uiState,
                                onAction = onAction,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuthScreenStep.VerifyEmail -> ModernVerifyEmailContent(
                                uiState = uiState,
                                onAction = onAction,
                                enabled = enabled,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                AuthMode.Biometric -> {
                    BiometricModeContent(
                        uiState = uiState,
                        onAction = onAction,
                        enabled = enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 4. Security & Version Footer
            AuthSecurityFooter(modifier = Modifier.fillMaxWidth())
        }
    }

    // Forgot Password Dialog
    if (uiState.forgotDialogOpen) {
        val extended = UthTheme.extendedColors
        AlertDialog(
            onDismissRequest = { onAction(AuthAction.DismissForgotPassword) },
            title = { Text(stringResource(R.string.auth_forgot_title)) },
            text = {
                Column {
                    if (uiState.forgotSuccessNotice) {
                        Text(
                            text = stringResource(R.string.auth_forgot_success),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.onSuccessContainer,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.auth_forgot_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = extended.textSecondary,
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Md))
                        OutlinedTextField(
                            value = uiState.forgotEmail,
                            onValueChange = { onAction(AuthAction.ForgotEmailChanged(it)) },
                            label = { Text(stringResource(R.string.auth_forgot_email_label)) },
                            singleLine = true,
                            enabled = enabled && !uiState.isSendingForgot,
                            shape = UthDimens.Radius.LgShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_forgot_email_input"),
                        )
                        uiState.forgotError?.let { err ->
                            Spacer(Modifier.height(UthDimens.Spacing.Xs))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (uiState.forgotSuccessNotice) {
                    TextButton(onClick = { onAction(AuthAction.DismissForgotPassword) }) {
                        Text(stringResource(R.string.auth_ok))
                    }
                } else {
                    Button(
                        onClick = { onAction(AuthAction.SubmitForgotPassword) },
                        enabled = enabled && !uiState.isSendingForgot && uiState.forgotEmail.isNotBlank(),
                        shape = UthDimens.Radius.LgShape,
                        modifier = Modifier.testTag("auth_forgot_submit"),
                    ) {
                        if (uiState.isSendingForgot) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(UthDimens.Size.IconInline),
                                strokeWidth = UthDimens.Size.ProgressInlineStroke,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.auth_forgot_submit))
                        }
                    }
                }
            },
            dismissButton = {
                if (!uiState.forgotSuccessNotice) {
                    TextButton(onClick = { onAction(AuthAction.DismissForgotPassword) }) {
                        Text(stringResource(R.string.auth_back_to_sign_in))
                    }
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
        modifier = modifier.testTag("auth_brand_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(UthDimens.Size.BrandLogoMonogram)
                    .clip(UthDimens.Radius.MdShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "UTH",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Md))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.auth_brand),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Xs))
                    UthStatusBadge(
                        text = stringResource(R.string.auth_badge_pro),
                        tone = BadgeTone.Accent,
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
            shape = MaterialTheme.shapes.small,
            color = extended.surfaceSubtle,
            border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.testTag("auth_language_pill"),
        ) {
            Row(
                modifier = Modifier.padding(UthDimens.Spacing.Xxs),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
            ) {
                LanguageOption(
                    text = stringResource(R.string.auth_lang_vi),
                    isSelected = selectedLanguage == "VI",
                    onClick = { onSelectLanguage("VI") },
                )
                LanguageOption(
                    text = stringResource(R.string.auth_lang_en),
                    isSelected = selectedLanguage == "EN",
                    onClick = { onSelectLanguage("EN") },
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
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

/**
 * Giao diện Hello Again! (Password Step) thiết kế theo đúng hình mẫu:
 * Tiêu đề thân thiện, 2 ô nhập liệu bo góc mềm mại, checkbox Remember me,
 * Forgot Password, nút Login chuẩn UTH Navy và Google SSO.
 */
@Composable
private fun ModernPasswordContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    largeText: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    val textFieldShape = UthDimens.Radius.LgShape

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        // Welcome Header
        Text(
            text = stringResource(R.string.auth_title_hello_again),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.auth_subtitle_welcome_back),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        // Success notice after new student setup
        AnimatedVisibility(visible = uiState.setupSuccessNotice) {
            Surface(
                shape = UthDimens.Radius.MdShape,
                color = extended.successContainer,
                border = BorderStroke(UthDimens.Size.Stroke, extended.success.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = UthDimens.Spacing.Lg),
            ) {
                Row(
                    modifier = Modifier.padding(UthDimens.Spacing.Md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.auth_new_student_success),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.onSuccessContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Card Container bọc form
        UthCard(modifier = Modifier.fillMaxWidth()) {
            // Field 1: MSSV / Email
            OutlinedTextField(
                value = uiState.identifier,
                onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_identifier"),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = extended.surfaceSubtle.copy(alpha = 0.5f),
                    unfocusedContainerColor = extended.surfaceSubtle.copy(alpha = 0.25f),
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = extended.textSecondary,
                        modifier = Modifier.size(UthDimens.Size.Icon),
                    )
                },
                label = { Text(stringResource(R.string.auth_identifier_label)) },
                isError = uiState.identifierError != null,
                supportingText = {
                    uiState.identifierError?.let { Text(stringResource(identifierErrorRes(it))) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            // Field 2: Password
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password"),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = extended.surfaceSubtle.copy(alpha = 0.5f),
                    unfocusedContainerColor = extended.surfaceSubtle.copy(alpha = 0.25f),
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = extended.textSecondary,
                        modifier = Modifier.size(UthDimens.Size.Icon),
                    )
                },
                label = { Text(stringResource(R.string.auth_password_label)) },
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { onAction(AuthAction.TogglePassword) },
                        modifier = Modifier
                            .size(UthDimens.Size.MinTouchTarget)
                            .testTag("auth_password_toggle"),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (uiState.passwordVisible) R.drawable.ic_auth_eye else R.drawable.ic_auth_eye_off,
                            ),
                            contentDescription = stringResource(
                                if (uiState.passwordVisible) R.string.auth_hide else R.string.auth_show,
                            ),
                            tint = extended.textSecondary,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                    }
                },
                isError = uiState.passwordError != null,
                supportingText = {
                    uiState.passwordError?.let { Text(stringResource(R.string.auth_required)) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.Submit) }),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            // Remember me & Forgot Password Row
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
                        Text(
                            text = stringResource(R.string.auth_forgot),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
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
                        Text(
                            text = stringResource(R.string.auth_forgot),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Failure feedback & Retry
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
                        shape = UthDimens.Radius.LgShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = UthDimens.Size.ButtonHeight)
                            .testTag("auth_retry"),
                    ) { Text(stringResource(R.string.auth_login_retry)) }
                }
            }

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            // Primary Login Button (Full width, rounded 16dp)
            ModernPrimaryButton(
                text = stringResource(R.string.auth_sign_in),
                onClick = { onAction(AuthAction.Submit) },
                enabled = enabled,
                loading = uiState.isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_sign_in"),
            )

            // Divider "or continue with"
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
                    text = stringResource(R.string.auth_divider_or_continue_with),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary,
                    modifier = Modifier.padding(horizontal = UthDimens.Spacing.Md),
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = UthDimens.Size.Stroke,
                )
            }

            // Google SSO Button (thẻ bo tròn với icon Google chính hãng)
            if (uiState.googleSignInAvailable) {
                Spacer(Modifier.height(UthDimens.Spacing.Lg))
                OutlinedButton(
                    onClick = { onAction(AuthAction.RequestGoogleSignIn) },
                    enabled = enabled && !uiState.isSigningInWithGoogle,
                    shape = UthDimens.Radius.LgShape,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.ButtonHeight)
                        .testTag("auth_google_sign_in"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                    Text(
                        text = if (uiState.isSigningInWithGoogle) {
                            stringResource(R.string.auth_google_signing_in)
                        } else {
                            stringResource(R.string.auth_google_sign_in)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Google Verification Notice Banner
            if (uiState.googleVerification != null) {
                val notice = uiState.googleVerification
                Spacer(Modifier.height(UthDimens.Spacing.Md))
                Surface(
                    shape = UthDimens.Radius.MdShape,
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
                        Spacer(Modifier.height(UthDimens.Spacing.Md))
                        OutlinedTextField(
                            value = uiState.verificationStudentId,
                            onValueChange = { onAction(AuthAction.VerificationStudentIdChanged(it)) },
                            label = { Text(stringResource(R.string.auth_verification_input_label)) },
                            singleLine = true,
                            enabled = enabled && !uiState.isSubmittingVerification,
                            shape = UthDimens.Radius.MdShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_verification_input"),
                        )
                        Spacer(Modifier.height(UthDimens.Spacing.Sm))
                        Button(
                            onClick = { onAction(AuthAction.SubmitVerificationRequest) },
                            enabled = enabled && !uiState.isSubmittingVerification && uiState.verificationStudentId.isNotBlank(),
                            shape = UthDimens.Radius.MdShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_verification_submit"),
                        ) {
                            if (uiState.isSubmittingVerification) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(UthDimens.Size.IconInline),
                                    strokeWidth = UthDimens.Size.ProgressInlineStroke,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text(stringResource(R.string.auth_verification_submit))
                            }
                        }
                        uiState.verificationNotice?.let { msg ->
                            Spacer(Modifier.height(UthDimens.Spacing.Xs))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.onInfoContainer,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        // Footer: "Don't have an account? Sign up"
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_dont_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.auth_sign_up),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = enabled) {
                        onAction(AuthAction.SelectStep(AuthScreenStep.Identifier))
                    }
                    .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs),
            )
        }
    }
}

/**
 * Bước 1 (Identifier Step): Giảm tải nhận thức tối đa.
 * Chỉ hỏi MSSV/Email trường hoặc Đăng nhập nhanh Google. Bấm Tiếp tục để nhận diện tài khoản.
 */
@Composable
private fun ModernIdentifierContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    val textFieldShape = UthDimens.Radius.LgShape

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_title_identifier),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.auth_subtitle_identifier),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        UthCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.identifier,
                onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_identifier"),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = extended.textSecondary,
                        modifier = Modifier.size(UthDimens.Size.Icon),
                    )
                },
                label = { Text(stringResource(R.string.auth_identifier_label)) },
                isError = uiState.identifierError != null,
                supportingText = {
                    uiState.identifierError?.let { Text(stringResource(identifierErrorRes(it))) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.ContinueIdentifier) }),
            )

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            ModernPrimaryButton(
                text = stringResource(R.string.auth_continue),
                onClick = { onAction(AuthAction.ContinueIdentifier) },
                enabled = enabled,
                loading = uiState.isSigningIn,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.googleSignInAvailable) {
                Spacer(Modifier.height(UthDimens.Spacing.Lg))
                OutlinedButton(
                    onClick = { onAction(AuthAction.RequestGoogleSignIn) },
                    enabled = enabled && !uiState.isSigningInWithGoogle,
                    shape = textFieldShape,
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = UthDimens.Size.ButtonHeight)
                        .testTag("auth_google_sign_in"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(UthDimens.Size.IconInline),
                    )
                    Spacer(Modifier.width(UthDimens.Spacing.Sm))
                    Text(
                        text = stringResource(R.string.auth_google_sign_in),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_already_have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.auth_back_to_sign_in),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = enabled) {
                        onAction(AuthAction.SelectStep(AuthScreenStep.Password))
                    }
                    .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs),
            )
        }
    }
}

/**
 * Bước 3: Hoàn thiện thông tin sinh viên lần đầu đăng nhập (First-time setup).
 */
@Composable
private fun ModernFirstTimeSetupContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    val textFieldShape = UthDimens.Radius.LgShape

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_new_student_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.auth_new_student_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        UthCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.newStudentName,
                onValueChange = { onAction(AuthAction.NewStudentNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_new_student_name)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            OutlinedTextField(
                value = uiState.newStudentFaculty,
                onValueChange = { onAction(AuthAction.NewStudentFacultyChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_new_student_faculty)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_new_student_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = { onAction(AuthAction.ConfirmPasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_new_student_confirm_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.SubmitFirstTimeSetup) }),
            )

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            ModernPrimaryButton(
                text = stringResource(R.string.auth_new_student_submit),
                onClick = { onAction(AuthAction.SubmitFirstTimeSetup) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        TextButton(
            onClick = { onAction(AuthAction.SelectStep(AuthScreenStep.Password)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.auth_back_to_sign_in),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Màn hình Đặt lại Mật khẩu Mới (Reset Password Step).
 */
@Composable
private fun ModernResetPasswordContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors
    val textFieldShape = UthDimens.Radius.LgShape

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.auth_reset_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xs))
        Text(
            text = stringResource(R.string.auth_reset_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        UthCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = uiState.resetNewPassword,
                onValueChange = { onAction(AuthAction.ResetNewPasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_reset_new_password"),
                enabled = enabled && !uiState.isResettingPassword,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_reset_new_password)) },
                visualTransformation = if (uiState.resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { onAction(AuthAction.ToggleResetPasswordVisibility) },
                        modifier = Modifier.size(UthDimens.Size.MinTouchTarget),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (uiState.resetPasswordVisible) R.drawable.ic_auth_eye else R.drawable.ic_auth_eye_off,
                            ),
                            contentDescription = null,
                            tint = extended.textSecondary,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))

            OutlinedTextField(
                value = uiState.resetConfirmPassword,
                onValueChange = { onAction(AuthAction.ResetConfirmPasswordChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_reset_confirm_password"),
                enabled = enabled && !uiState.isResettingPassword,
                singleLine = true,
                shape = textFieldShape,
                label = { Text(stringResource(R.string.auth_reset_confirm_password)) },
                visualTransformation = if (uiState.resetPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAction(AuthAction.SubmitResetPassword) }),
            )

            uiState.resetPasswordError?.let { err ->
                Spacer(Modifier.height(UthDimens.Spacing.Sm))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(UthDimens.Spacing.Lg))

            ModernPrimaryButton(
                text = stringResource(R.string.auth_reset_submit),
                onClick = { onAction(AuthAction.SubmitResetPassword) },
                enabled = enabled && !uiState.isResettingPassword,
                loading = uiState.isResettingPassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_reset_submit_btn"),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        TextButton(
            onClick = { onAction(AuthAction.SelectStep(AuthScreenStep.Password)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.auth_back_to_sign_in),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/**
 * Màn hình Chờ xác nhận Email (Verify Email Step).
 */
@Composable
private fun ModernVerifyEmailContent(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extended = UthTheme.extendedColors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(UthDimens.Size.AvatarLarge)
                .clip(CircleShape)
                .background(extended.infoContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = extended.onInfoContainer,
                modifier = Modifier.size(UthDimens.Size.Icon),
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Text(
            text = stringResource(R.string.auth_verify_email_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.auth_verify_email_subtitle, uiState.registeredEmail.ifEmpty { "hòm thư sinh viên" }),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        UthCard(modifier = Modifier.fillMaxWidth()) {
            val cooldown = uiState.verifyEmailCooldownSeconds
            Button(
                onClick = { onAction(AuthAction.ResendVerificationEmail) },
                enabled = enabled && cooldown == 0,
                shape = UthDimens.Radius.LgShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_resend_email"),
            ) {
                Text(
                    text = if (cooldown > 0) {
                        stringResource(R.string.auth_verify_email_resend_cooldown, cooldown)
                    } else {
                        stringResource(R.string.auth_verify_email_resend)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        TextButton(
            onClick = { onAction(AuthAction.SelectStep(AuthScreenStep.Password)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.auth_back_to_sign_in),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Biometric Mode Content */
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

            uiState.failure?.let { failure ->
                Text(
                    text = stringResource(failureRes(failure)),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("auth_error"),
                )
            }

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
                    shape = UthDimens.Radius.LgShape,
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
        UthCard(modifier = modifier.testTag("auth_biometric_card")) {
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
                ModernPrimaryButton(
                    text = stringResource(R.string.auth_biometric_switch_password),
                    onClick = { onAction(AuthAction.SelectMode(AuthMode.Password)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Nút chính hiện đại bo góc 16dp chuẩn visual hierarchy */
@Composable
private fun ModernPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val loadingDescription = stringResource(R.string.common_loading)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .heightIn(min = UthDimens.Size.ButtonHeight)
            .semantics {
                if (loading) stateDescription = loadingDescription
            },
        shape = UthDimens.Radius.LgShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(UthDimens.Size.ProgressInline)
                    .clearAndSetSemantics { },
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = UthDimens.Size.ProgressInlineStroke,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
            text = stringResource(R.string.auth_remember),
            style = MaterialTheme.typography.bodySmall,
        )
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
    AuthFailure.RateLimited -> R.string.auth_forgot_rate_limit
    AuthFailure.NetworkError -> R.string.auth_network_error
    AuthFailure.TokenExpired -> R.string.auth_reset_token_expired
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

@Preview(name = "Light Mode - Password (Hello Again)", showBackground = true)
@Composable
private fun PreviewAuthLightPassword() {
    UthPreview(darkTheme = false) {
        AuthScreen(
            uiState = AuthUiState(
                isRestoring = false,
                googleSignInAvailable = true,
                screenStep = AuthScreenStep.Password,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Dark Mode - Password (Hello Again)", showBackground = true)
@Composable
private fun PreviewAuthDarkPassword() {
    UthPreview(darkTheme = true) {
        AuthScreen(
            uiState = AuthUiState(
                isRestoring = false,
                googleSignInAvailable = true,
                screenStep = AuthScreenStep.Password,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Light Mode - Identifier Step", showBackground = true)
@Composable
private fun PreviewAuthIdentifierStep() {
    UthPreview(darkTheme = false) {
        AuthScreen(
            uiState = AuthUiState(
                isRestoring = false,
                googleSignInAvailable = true,
                screenStep = AuthScreenStep.Identifier,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Light Mode - Biometric Enrolled", showBackground = true)
@Composable
private fun PreviewAuthLightBiometric() {
    UthPreview(darkTheme = false) {
        AuthScreen(
            uiState = AuthUiState(
                isRestoring = false,
                authMode = AuthMode.Biometric,
                biometricAvailable = true,
                savedStudent = PreviewStudent,
            ),
            onAction = {},
        )
    }
}

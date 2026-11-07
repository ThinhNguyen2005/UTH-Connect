package com.giathinh.uthconnect.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.alpha
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
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import com.giathinh.uthconnect.domain.validation.LoginFieldError

private val retryableAuthFailures = setOf(
    AuthFailure.InvalidCredentials,
    AuthFailure.ProfileUnavailable,
    AuthFailure.Unexpected,
    AuthFailure.NetworkError,
)

/**
 * Màn hình đăng nhập thiết kế theo chuẩn Modern Minimalist & UTH Design System.
 *
 * Tối ưu hóa phân cấp thị giác (Visual Hierarchy), loại bỏ các khung viền hộp lồng nhau,
 * xử lý triệt để Status Bar & Navigation Bar Insets, hỗ trợ luồng Identifier-first thông minh.
 */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSigningIn && !uiState.isSigningInWithGoogle
    val largeText = LocalDensity.current.fontScale >= UthDimens.Layout.LargeTextScale

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Lớp nền cong mỹ thuật nhẹ nhàng ở góc trên phải (Decorative Soft Ambient Shape)
        val ambientColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = UthDimens.Opacity.AuthAmbientShape)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = ambientColor,
                radius = size.width * UthDimens.Layout.AuthAmbientRadiusFraction,
                center = Offset(
                    x = size.width * UthDimens.Layout.AuthAmbientCenterFractionX,
                    y = size.height * UthDimens.Layout.AuthAmbientCenterFractionY,
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UthDimens.Spacing.Xl, vertical = UthDimens.Spacing.Md),
            verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg),
        ) {
            // 1. Top Bar: Logo thương hiệu UTH & Nút chuyển ngôn ngữ
            AuthTopBar(
                selectedLanguage = uiState.selectedLanguage,
                onSelectLanguage = { onAction(AuthAction.SelectLanguage(it)) },
                modifier = Modifier.fillMaxWidth(),
            )

            // 2. Nội dung chính theo từng bước (Không dùng Card lồng khung gây nặng nề)
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

            Spacer(Modifier.weight(1f, fill = false))

            // 3. Footer bảo mật & phiên bản (Thu nhỏ tinh tế, không chiếm diện tích)
            AuthSecurityFooter(modifier = Modifier.fillMaxWidth())
        }
    }

    // Forgot Password Dialog (Khôi phục mật khẩu sinh viên)
    if (uiState.forgotDialogOpen) {
        val extended = UthTheme.extendedColors
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSendingForgot) onAction(AuthAction.DismissForgotPassword)
            },
            title = {
                Text(
                    text = stringResource(R.string.auth_forgot_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
                ) {
                    Text(
                        text = stringResource(R.string.auth_forgot_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (uiState.forgotSuccessNotice) {
                        Surface(
                            shape = UthDimens.Radius.MdShape,
                            color = extended.successContainer.copy(alpha = UthDimens.Opacity.AuthSuccessBannerBorder),
                            border = BorderStroke(UthDimens.Size.Stroke, extended.success.copy(alpha = UthDimens.Opacity.AuthSuccessBannerBorder)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_forgot_success_banner"),
                        ) {
                            Row(
                                modifier = Modifier.padding(UthDimens.Spacing.Md),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Sm),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = extended.success,
                                    modifier = Modifier.size(UthDimens.Size.IconCompact),
                                )
                                Text(
                                    text = stringResource(R.string.auth_forgot_success),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.onSuccessContainer,
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = uiState.forgotEmail,
                            onValueChange = { onAction(AuthAction.ForgotEmailChanged(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_forgot_email"),
                            enabled = !uiState.isSendingForgot,
                            singleLine = true,
                            label = { Text(stringResource(R.string.auth_forgot_email_label)) },
                            placeholder = { Text("mssv@ut.edu.vn") },
                            shape = UthDimens.Radius.LgShape,
                            isError = uiState.forgotError != null,
                            supportingText = {
                                uiState.forgotError?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Send,
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { onAction(AuthAction.SubmitForgotPassword) },
                            ),
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(AuthAction.DismissForgotPassword) },
                    enabled = !uiState.isSendingForgot,
                    modifier = Modifier.testTag("auth_forgot_cancel"),
                ) {
                    Text(stringResource(R.string.auth_cancel))
                }
            },
            confirmButton = {
                if (uiState.forgotSuccessNotice) {
                    Button(
                        onClick = { onAction(AuthAction.DismissForgotPassword) },
                        shape = UthDimens.Radius.MdShape,
                        modifier = Modifier.testTag("auth_forgot_done"),
                    ) {
                        Text(stringResource(R.string.auth_ok))
                    }
                } else {
                    Button(
                        onClick = { onAction(AuthAction.SubmitForgotPassword) },
                        enabled = !uiState.isSendingForgot && uiState.forgotEmail.isNotBlank(),
                        shape = UthDimens.Radius.MdShape,
                        modifier = Modifier
                            .testTag("auth_forgot_send"),
                    ) {
                        if (uiState.isSendingForgot) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(UthDimens.Size.ProgressInline),
                                strokeWidth = UthDimens.Size.ProgressInlineStroke,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(stringResource(R.string.auth_forgot_submit))
                        }
                    }
                }
            },
        )
    }
}

/** Top Bar: Logo UTH Connect tinh gọn và Language Switch Pill. */
@Composable
private fun AuthTopBar(
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
                    .size(UthDimens.Size.Avatar)
                    .clip(UthDimens.Radius.MdShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.auth_brand_monogram),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(UthDimens.Spacing.Sm))
            Column {
                Text(
                    text = stringResource(R.string.auth_brand),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // Language Pill (VI / EN)
        Surface(
            shape = MaterialTheme.shapes.small,
            color = extended.surfaceSubtle,
            border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.SubtleDivider)),
            modifier = Modifier.testTag("auth_language_pill"),
        ) {
            Row(
                modifier = Modifier.padding(UthDimens.Spacing.Xxs),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Xxs),
            ) {
                LanguageOption(
                    text = stringResource(R.string.auth_lang_vi),
                    isSelected = selectedLanguage == LANGUAGE_CODE_VIETNAMESE,
                    onClick = { onSelectLanguage(LANGUAGE_CODE_VIETNAMESE) },
                )
                LanguageOption(
                    text = stringResource(R.string.auth_lang_en),
                    isSelected = selectedLanguage == LANGUAGE_CODE_ENGLISH,
                    onClick = { onSelectLanguage(LANGUAGE_CODE_ENGLISH) },
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
            .height(UthDimens.Size.LanguagePillHeight)
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else extended.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/**
 * Giao diện Hello Again! (Password Step) chuẩn phong cách Minimalism:
 * Không lồng card thô cứng, khoảng cách thoáng đãng, các input bo góc 16dp mềm mại.
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
        Spacer(Modifier.height(UthDimens.Spacing.Sm))

        // Welcome Header
        Text(
            text = stringResource(R.string.auth_title_hello_again),
            style = MaterialTheme.typography.headlineLarge,
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
                border = BorderStroke(UthDimens.Size.Stroke, extended.success.copy(alpha = UthDimens.Opacity.AuthSuccessBannerBorder)),
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

        // Field 1: MSSV / Email sinh viên (Single line, no bloated label)
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
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthInputUnfocusedBorder),
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

        // Field 2: Mật khẩu với Eye toggle
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
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthInputUnfocusedBorder),
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
            visualTransformation = if (uiState.passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
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
                        color = MaterialTheme.colorScheme.primary,
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
                        color = MaterialTheme.colorScheme.primary,
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
            if (failure in retryableAuthFailures) {
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

        // Nút Đăng nhập chính (Full width, bo tròn 16dp)
        ModernPrimaryButton(
            text = stringResource(R.string.auth_sign_in),
            onClick = { onAction(AuthAction.Submit) },
            enabled = enabled,
            loading = uiState.isSigningIn,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_sign_in"),
        )

        // Phân cách "hoặc tiếp tục với"
        Spacer(Modifier.height(UthDimens.Spacing.Lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthDivider),
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthDivider),
                thickness = UthDimens.Size.Stroke,
            )
        }

        // Nút Google SSO tinh gọn
        if (uiState.googleSignInAvailable) {
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
            OutlinedButton(
                onClick = { onAction(AuthAction.RequestGoogleSignIn) },
                enabled = enabled && !uiState.isSigningInWithGoogle,
                shape = UthDimens.Radius.LgShape,
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = extended.surfaceSubtle,
                ),
                border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthOutlineButtonBorder)),
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
                    fontWeight = FontWeight.SemiBold,
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
                border = BorderStroke(UthDimens.Size.Stroke, extended.info.copy(alpha = UthDimens.Opacity.AuthInfoBannerBorder)),
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

        // Footer link: Chưa có tài khoản? Đăng ký
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
                        onAction(AuthAction.SelectStep(AuthScreenStep.FirstTimeSetup))
                    }
                    .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Xxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_verify_email_prompt),
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.auth_verify_email_action),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = enabled) {
                        onAction(AuthAction.SelectStep(AuthScreenStep.VerifyEmail))
                    }
                    .padding(horizontal = UthDimens.Spacing.Xs, vertical = UthDimens.Spacing.Xxs)
                    .testTag("auth_goto_verify_email"),
            )
        }

        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        TextButton(
            onClick = { onAction(AuthAction.SwitchAccount) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(min = UthDimens.Size.MinTouchTarget)
                .testTag("auth_switch_account"),
        ) {
            Text(
                text = stringResource(R.string.auth_change_identifier),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Bước 1 (Identifier Step): Giảm tải nhận thức tối đa.
 * Chỉ yêu cầu nhập MSSV/Email trường hoặc Đăng nhập nhanh Google.
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
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.auth_title_identifier),
            style = MaterialTheme.typography.headlineLarge,
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
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthInputUnfocusedBorder),
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
                colors = ButtonDefaults.outlinedButtonColors(containerColor = extended.surfaceSubtle),
                border = BorderStroke(UthDimens.Size.Stroke, MaterialTheme.colorScheme.outlineVariant.copy(alpha = UthDimens.Opacity.AuthOutlineButtonBorder)),
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
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
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

/** Bước 3: Hoàn thiện thông tin tân sinh viên lần đầu đăng nhập. */
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
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.auth_new_student_title),
            style = MaterialTheme.typography.headlineLarge,
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

        OutlinedTextField(
            value = uiState.identifier,
            onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
            modifier = Modifier.fillMaxWidth().testTag("auth_setup_identifier"),
            enabled = enabled,
            singleLine = true,
            shape = textFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
            label = { Text(stringResource(R.string.auth_identifier_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(UthDimens.Spacing.Sm))

        OutlinedTextField(
            value = uiState.newStudentName,
            onValueChange = { onAction(AuthAction.NewStudentNameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            shape = textFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
 * Bước 4: Đặt lại mật khẩu (Reset Password Step).
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
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(
            text = stringResource(R.string.auth_reset_title),
            style = MaterialTheme.typography.headlineLarge,
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

        OutlinedTextField(
            value = uiState.resetNewPassword,
            onValueChange = { onAction(AuthAction.ResetNewPasswordChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_reset_new_password"),
            enabled = enabled && !uiState.isResettingPassword,
            singleLine = true,
            shape = textFieldShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = extended.surfaceSubtle,
                unfocusedContainerColor = extended.surfaceSubtle,
            ),
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
 * Bước 5: Chờ xác nhận Email (Verify Email Step).
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
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
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
            text = stringResource(
                R.string.auth_verify_email_subtitle,
                uiState.registeredEmail.ifEmpty { stringResource(R.string.auth_verify_email_default_mailbox) },
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(UthDimens.Spacing.Xl))

        val cooldown = uiState.verifyEmailCooldownSeconds
        Button(
            onClick = { onAction(AuthAction.ResendVerificationEmail) },
            enabled = enabled && cooldown == 0,
            shape = UthDimens.Radius.LgShape,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = UthDimens.Size.ButtonHeight)
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Security & Version Footer (Gọn gàng, độ mờ nhẹ, không gây phân tán). */
@Composable
private fun AuthSecurityFooter(modifier: Modifier = Modifier) {
    val extended = UthTheme.extendedColors
    Column(
        modifier = modifier
            .padding(top = UthDimens.Spacing.Md, bottom = UthDimens.Spacing.Xs)
            .alpha(UthDimens.Opacity.AuthFooter),
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
                tint = extended.textTertiary,
            )
            Spacer(Modifier.width(UthDimens.Spacing.Xs))
            Text(
                text = stringResource(R.string.auth_ssl_indicator),
                style = MaterialTheme.typography.labelSmall,
                color = extended.textTertiary,
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Xxs))
        Text(
            text = stringResource(R.string.auth_app_version),
            style = MaterialTheme.typography.labelSmall,
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
    AuthFailure.ProfileUnavailable -> R.string.auth_profile_unavailable
    AuthFailure.Unexpected -> R.string.auth_unexpected
    AuthFailure.SessionRestore -> R.string.auth_restore_failed
    AuthFailure.GoogleUnavailable -> R.string.auth_google_unavailable
    AuthFailure.GoogleRejected -> R.string.auth_google_rejected
    AuthFailure.RateLimited -> R.string.auth_forgot_rate_limit
    AuthFailure.NetworkError -> R.string.auth_network_error
    AuthFailure.TokenExpired -> R.string.auth_reset_token_expired
    AuthFailure.VerificationRequired -> R.string.auth_verify_email
}

// ───────────────────────────────────────── COMPOSE PREVIEWS ─────────────────────────────────────────

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

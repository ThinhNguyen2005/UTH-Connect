package com.giathinh.uthconnect.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.UthDimens
import com.giathinh.uthconnect.domain.validation.InputError
import com.giathinh.uthconnect.feature.auth.AuthUiState
import com.giathinh.uthconnect.feature.auth.SignInError
import com.giathinh.uthconnect.ui.theme.UthConnectTheme

/** Pure UDF screen. No Repository, Context, navigation, or ViewModel in this file. */
@Composable
fun LoginScreen(
    state: AuthUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onPasswordVisibilityClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onForgotPasswordDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inputEnabled = !state.isSigningIn
    Box(
        modifier = modifier.fillMaxSize().imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = UthDimens.pageMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(UthDimens.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(UthDimens.avatar)
                    .clip(RoundedCornerShape(UthDimens.corner))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(UthDimens.avatar / 2),
                )
            }
            Spacer(Modifier.height(UthDimens.regular))
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(UthDimens.xLarge))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UthDimens.corner),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = UthDimens.xSmall),
            ) {
                Column(Modifier.padding(UthDimens.large)) {
                    Text(
                        text = stringResource(R.string.login_welcome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(UthDimens.small))
                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(UthDimens.large))

                    OutlinedTextField(
                        value = state.identifier,
                        onValueChange = onIdentifierChange,
                        modifier = Modifier.fillMaxWidth().testTag("login_identifier"),
                        label = { Text(stringResource(R.string.login_id_label)) },
                        placeholder = { Text(stringResource(R.string.login_id_hint)) },
                        singleLine = true,
                        enabled = inputEnabled,
                        isError = state.identifierError != null,
                        supportingText = state.identifierError?.let { error ->
                            { Text(inputErrorText(error, password = false)) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(UthDimens.fieldCorner),
                    )
                    Spacer(Modifier.height(UthDimens.medium))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth().testTag("login_password"),
                        label = { Text(stringResource(R.string.password_label)) },
                        singleLine = true,
                        enabled = inputEnabled,
                        isError = state.passwordError != null,
                        visualTransformation = if (state.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        supportingText = state.passwordError?.let { error ->
                            { Text(inputErrorText(error, password = true)) }
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = onPasswordVisibilityClick, enabled = inputEnabled) {
                                Icon(
                                    imageVector = if (state.isPasswordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = stringResource(
                                        if (state.isPasswordVisible) R.string.password_hide
                                        else R.string.password_show,
                                    ),
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSignInClick() }),
                        shape = RoundedCornerShape(UthDimens.fieldCorner),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = state.rememberMe,
                                onCheckedChange = onRememberMeChange,
                                enabled = inputEnabled,
                                modifier = Modifier.testTag("remember_me"),
                            )
                            Text(
                                stringResource(R.string.remember_me),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = onForgotPasswordClick, enabled = inputEnabled) {
                            Text(stringResource(R.string.forgot_password))
                        }
                    }

                    state.signInError?.let { error ->
                        Text(
                            text = stringResource(
                                if (error == SignInError.INVALID_CREDENTIALS) {
                                    R.string.credentials_invalid
                                } else {
                                    R.string.unexpected_error
                                },
                            ),
                            modifier = Modifier.padding(bottom = UthDimens.small),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth().height(UthDimens.xLarge * 1.75f)
                            .testTag("login_submit"),
                        enabled = inputEnabled,
                        shape = RoundedCornerShape(UthDimens.fieldCorner),
                    ) {
                        if (state.isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(UthDimens.large),
                                strokeWidth = UthDimens.xSmall / 2,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(UthDimens.regular))
                        }
                        Text(
                            if (state.isSigningIn) stringResource(R.string.login_loading)
                            else stringResource(R.string.login_button),
                        )
                    }
                }
            }

            Spacer(Modifier.height(UthDimens.regular))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(UthDimens.fieldCorner),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Column(Modifier.padding(UthDimens.regular)) {
                    Text(
                        stringResource(R.string.demo_account_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(UthDimens.small))
                    Text(stringResource(R.string.demo_account_info), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.demo_password_info), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(UthDimens.small))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(UthDimens.small))
                    Text(
                        stringResource(R.string.demo_disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (state.showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = onForgotPasswordDismiss,
            title = { Text(stringResource(R.string.forgot_title)) },
            text = { Text(stringResource(R.string.forgot_message)) },
            confirmButton = {
                TextButton(onClick = onForgotPasswordDismiss) {
                    Text(stringResource(R.string.understood))
                }
            },
        )
    }
}

@Composable
private fun inputErrorText(error: InputError, password: Boolean): String = when {
    error == InputError.INVALID_IDENTIFIER -> stringResource(R.string.id_invalid)
    password -> stringResource(R.string.password_required)
    else -> stringResource(R.string.field_required)
}

@Preview(name = "Login Light", showBackground = true)
@Composable
private fun PreviewLoginLight() {
    UthConnectTheme(darkTheme = false) { PreviewLogin(AuthUiState(isRestoring = false)) }
}

@Preview(name = "Login Dark", showBackground = true)
@Composable
private fun PreviewLoginDark() {
    UthConnectTheme(darkTheme = true) { PreviewLogin(AuthUiState(isRestoring = false)) }
}

@Preview(name = "Login Error", showBackground = true, fontScale = 1.3f)
@Composable
private fun PreviewLoginError() {
    UthConnectTheme {
        PreviewLogin(
            AuthUiState(
                isRestoring = false,
                identifier = "abc",
                identifierError = InputError.INVALID_IDENTIFIER,
                passwordError = InputError.REQUIRED,
            ),
        )
    }
}

@Preview(name = "Login Loading", showBackground = true)
@Composable
private fun PreviewLoginLoading() {
    UthConnectTheme { PreviewLogin(AuthUiState(isRestoring = false, isSigningIn = true)) }
}

@Composable
private fun PreviewLogin(state: AuthUiState) {
    LoginScreen(
        state = state,
        onIdentifierChange = {},
        onPasswordChange = {},
        onRememberMeChange = {},
        onPasswordVisibilityClick = {},
        onForgotPasswordClick = {},
        onForgotPasswordDismiss = {},
        onSignInClick = {},
    )
}

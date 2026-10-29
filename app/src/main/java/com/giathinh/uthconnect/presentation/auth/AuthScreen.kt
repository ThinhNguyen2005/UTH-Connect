package com.giathinh.uthconnect.presentation.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.validation.LoginFieldError

/** UI chỉ lấy state và phát Action, không biết cách lưu phiên hoặc xác thực. */
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    onAction: (AuthAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSigningIn
    Column(
        modifier = modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.auth_brand), style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(UthDimens.Spacing.Sm))
        Text(stringResource(R.string.auth_subtitle), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(UthDimens.Spacing.Xl))
        UthCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
            OutlinedTextField(
                value = uiState.identifier,
                onValueChange = { onAction(AuthAction.IdentifierChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("auth_identifier"),
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.auth_identifier_label)) },
                isError = uiState.identifierError != null,
                supportingText = {
                    uiState.identifierError?.let { Text(stringResource(identifierErrorRes(it))) }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(UthDimens.Spacing.Sm))
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { onAction(AuthAction.PasswordChanged(it)) },
                modifier = Modifier.fillMaxWidth().testTag("auth_password"),
                enabled = enabled,
                singleLine = true,
                label = { Text(stringResource(R.string.auth_password_label)) },
                visualTransformation = if (uiState.passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { onAction(AuthAction.TogglePassword) }) {
                        Text(stringResource(if (uiState.passwordVisible) R.string.auth_hide else R.string.auth_show))
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
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.rememberMe,
                    onCheckedChange = { onAction(AuthAction.RememberChanged(it)) },
                    enabled = enabled)
                Text(stringResource(R.string.auth_remember), modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { onAction(AuthAction.ForgotPassword) }) {
                    Text(stringResource(R.string.auth_forgot))
                }
            }
            uiState.failure?.let { failure ->
                Text(
                    text = stringResource(failureRes(failure)),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("auth_error"),
                )
                Spacer(Modifier.height(UthDimens.Spacing.Sm))
            }
            Spacer(Modifier.height(UthDimens.Spacing.Lg))
            UthPrimaryButton(
                text = stringResource(R.string.auth_sign_in),
                onClick = { onAction(AuthAction.Submit) },
                enabled = enabled,
                loading = uiState.isSigningIn,
                modifier = Modifier.fillMaxWidth().testTag("auth_sign_in"),
            )
        }
        Spacer(Modifier.height(UthDimens.Spacing.Md))
        Text(stringResource(R.string.auth_demo_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
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

private fun identifierErrorRes(error: LoginFieldError) = when (error) {
    LoginFieldError.Required -> R.string.auth_required
    LoginFieldError.InvalidIdentifier -> R.string.auth_invalid_identifier
}

private fun failureRes(failure: AuthFailure) = when (failure) {
    AuthFailure.InvalidCredentials -> R.string.auth_invalid_credentials
    AuthFailure.Unexpected -> R.string.auth_unexpected
    AuthFailure.SessionRestore -> R.string.auth_restore_failed
}

@Composable
private fun PreviewState(uiState: AuthUiState, dark: Boolean = false) =
    UthPreview(darkTheme = dark) { AuthScreen(uiState, onAction = {}) }

@Preview(name = "Auth empty light", showBackground = true)
@Composable private fun AuthEmptyLight() = PreviewState(AuthUiState(isRestoring = false))
@Preview(name = "Auth empty dark", showBackground = true)
@Composable private fun AuthEmptyDark() = PreviewState(AuthUiState(isRestoring = false), true)
@Preview(name = "Auth loading light", showBackground = true)
@Composable private fun AuthLoadingLight() = PreviewState(AuthUiState(isRestoring = false, isSigningIn = true))
@Preview(name = "Auth loading dark", showBackground = true)
@Composable private fun AuthLoadingDark() = PreviewState(AuthUiState(isRestoring = false, isSigningIn = true), true)
@Preview(name = "Auth error light", showBackground = true)
@Composable private fun AuthErrorLight() = PreviewState(AuthUiState(isRestoring = false, failure = AuthFailure.InvalidCredentials))
@Preview(name = "Auth error dark", showBackground = true)
@Composable private fun AuthErrorDark() = PreviewState(AuthUiState(isRestoring = false, failure = AuthFailure.InvalidCredentials), true)
@Preview(name = "Auth content light", showBackground = true)
@Composable private fun AuthContentLight() = PreviewState(AuthUiState(isRestoring = false, identifier = "2023040123"))
@Preview(name = "Auth content dark", showBackground = true)
@Composable private fun AuthContentDark() = PreviewState(AuthUiState(isRestoring = false, identifier = "2023040123"), true)

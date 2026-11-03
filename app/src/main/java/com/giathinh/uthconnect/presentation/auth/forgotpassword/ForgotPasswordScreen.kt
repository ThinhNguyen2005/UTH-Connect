package com.giathinh.uthconnect.presentation.auth.forgotpassword

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.repository.AuthError

@Composable
fun ForgotPasswordScreen(state: ForgotPasswordUiState, onAction: (ForgotPasswordAction) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg)) {
        Text(stringResource(R.string.auth_forgot_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.auth_forgot_message))
        OutlinedTextField(state.email, { onAction(ForgotPasswordAction.EmailChanged(it)) },
            modifier = Modifier.fillMaxWidth().testTag("forgot_email"), singleLine = true,
            enabled = !state.isSending, label = { Text(stringResource(R.string.auth3_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        if (state.sent) Text(stringResource(R.string.auth_reset_sent), modifier = Modifier.testTag("forgot_sent"))
        state.error?.let { Text(stringResource(when(it) {
            AuthError.DomainNotAllowed -> R.string.auth3_domain
            AuthError.Network -> R.string.auth_network
            AuthError.TooManyRequests -> R.string.auth_too_many
            AuthError.Configuration -> R.string.auth_configuration
            else -> R.string.auth_unexpected
        }), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("forgot_error")) }
        UthPrimaryButton(stringResource(R.string.auth_reset_send), { onAction(ForgotPasswordAction.Send) },
            modifier = Modifier.fillMaxWidth().testTag("forgot_send"), loading = state.isSending, enabled = !state.sent)
        TextButton(onBack, enabled = !state.isSending) { Text(stringResource(R.string.auth_back)) }
    }
}
@Preview(showBackground = true) @Composable private fun ForgotPreview() = UthPreview { ForgotPasswordScreen(ForgotPasswordUiState(), {}, {}) }
@Preview(showBackground = true) @Composable private fun ForgotLoading() = UthPreview { ForgotPasswordScreen(ForgotPasswordUiState(isSending = true), {}, {}) }
@Preview(showBackground = true) @Composable private fun ForgotError() = UthPreview { ForgotPasswordScreen(ForgotPasswordUiState(error = AuthError.Network), {}, {}) }
@Preview(showBackground = true) @Composable private fun ForgotDarkError() = UthPreview(darkTheme = true) { ForgotPasswordScreen(ForgotPasswordUiState(error = AuthError.DomainNotAllowed), {}, {}) }

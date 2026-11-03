package com.giathinh.uthconnect.presentation.auth.verifyemail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.domain.repository.AuthError

@Composable
fun VerifyEmailScreen(state: VerifyEmailUiState, remember: Boolean, onAction: (VerifyEmailAction) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(UthDimens.Spacing.Xl),
        verticalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Lg)) {
        Text(stringResource(R.string.auth3_verify_title), style = MaterialTheme.typography.headlineMedium)
        Text(state.email, modifier = Modifier.testTag("verify_email"))
        Text(stringResource(R.string.auth3_verify_body))
        state.error?.let {
            Text(stringResource(when(it) {
                AuthError.EmailNotVerified, AuthError.VerificationRequired -> R.string.auth3_not_verified
                AuthError.DomainNotAllowed -> R.string.auth3_domain
                AuthError.Network -> R.string.auth_network
                AuthError.TooManyRequests -> R.string.auth_too_many
                AuthError.Configuration -> R.string.auth_configuration
                AuthError.ApprovalRequired -> R.string.auth_approval
                AuthError.PermissionDenied -> R.string.auth_permission
                AuthError.Disabled -> R.string.auth_disabled
                AuthError.InvalidSession -> R.string.auth_invalid_session
                else -> R.string.auth_unexpected
            }), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("verify_error"))
        }
        if (state.emailSent) Text(stringResource(R.string.auth3_verification_sent))
        UthPrimaryButton(stringResource(R.string.auth3_check_verified), { onAction(VerifyEmailAction.Check(remember)) },
            modifier = Modifier.fillMaxWidth().testTag("verify_check"), enabled = !state.isSending,
            loading = state.isChecking)
        OutlinedButton({ onAction(VerifyEmailAction.Resend) }, modifier = Modifier.fillMaxWidth().testTag("verify_resend"),
            enabled = state.cooldownSeconds == 0 && !state.isChecking && !state.isSending) {
            Text(if (state.cooldownSeconds > 0) stringResource(R.string.auth3_cooldown, state.cooldownSeconds)
                else stringResource(R.string.auth3_resend))
        }
        OutlinedButton({ onAction(VerifyEmailAction.SignOut) }, modifier = Modifier.fillMaxWidth().testTag("verify_logout"),
            enabled = !state.isChecking && !state.isSending) { Text(stringResource(R.string.auth_logout)) }
    }
}

@Preview(showBackground = true) @Composable private fun VerifyPreview() = UthPreview { VerifyEmailScreen(VerifyEmailUiState(email = "student@ut.edu.vn"), false, {}) }
@Preview(showBackground = true) @Composable private fun VerifyLoading() = UthPreview { VerifyEmailScreen(VerifyEmailUiState(isChecking = true), false, {}) }
@Preview(showBackground = true) @Composable private fun VerifyError() = UthPreview { VerifyEmailScreen(VerifyEmailUiState(error = AuthError.Network), false, {}) }
@Preview(showBackground = true) @Composable private fun VerifyDarkError() = UthPreview(darkTheme = true) { VerifyEmailScreen(VerifyEmailUiState(error = AuthError.EmailNotVerified), false, {}) }

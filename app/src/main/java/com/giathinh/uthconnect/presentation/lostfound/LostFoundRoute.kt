package com.giathinh.uthconnect.presentation.lostfound

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Entry-point route for Lost & Found feature, managing state subscription and navigation callbacks.
 */
@Composable
fun LostFoundRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LostFoundViewModel = hiltViewModel(),
    onCallContact: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LostFoundScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        onCallClick = { phone ->
            if (onCallContact != null) {
                onCallContact(phone)
            } else {
                dialPhoneNumberSafely(
                    context = context,
                    phoneNumber = phone,
                    onDialerUnavailable = {
                        viewModel.onAction(LostFoundAction.ShowDialerUnavailable)
                    },
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * Triggers a phone call intent with the given contact number, handling devices without a dialer.
 *
 * @param context Android context for launching the dialer intent.
 * @param phoneNumber Phone number string to dial.
 * @param onDialerUnavailable Callback invoked when no dialer application is present.
 */
fun dialPhoneNumberSafely(
    context: Context,
    phoneNumber: String,
    onDialerUnavailable: () -> Unit,
) {
    val cleanNumber = phoneNumber.replace(" ", "").trim()
    if (cleanNumber.isNotBlank()) {
        launchDialer(context, cleanNumber, onError = onDialerUnavailable)
    }
}

internal fun launchDialer(
    context: Context,
    phone: String,
    intentFactory: (String) -> Intent = { Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")) },
    onError: () -> Unit,
) {
    try {
        val dialIntent = intentFactory(phone)
        context.startActivity(dialIntent)
    } catch (_: ActivityNotFoundException) {
        onError()
    } catch (_: SecurityException) {
        onError()
    }
}

package com.giathinh.uthconnect.presentation.lostfound

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R

@Composable
fun LostFoundRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LostFoundViewModel = hiltViewModel(),
    onCallContact: ((String) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LostFoundScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        onCallContact = { phone ->
            if (onCallContact != null) {
                onCallContact(phone)
            } else {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(dialIntent)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, R.string.lostfound_no_dialer_error, Toast.LENGTH_SHORT).show()
                } catch (_: SecurityException) {
                    Toast.makeText(context, R.string.lostfound_no_dialer_error, Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = modifier,
    )
}

package com.giathinh.uthconnect.presentation.housing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R
import kotlinx.coroutines.flow.collect

/**
 * Điểm nối duy nhất của module với Hilt (docs/CODING_STANDARDS.md mục 3.2):
 * thu UiState, hứng event một lần thành snackbar và gọi [HousingScreen] stateless.
 */
@Composable
fun HousingRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onListingClick: (String) -> Unit = {},
    onCreatePost: () -> Unit = {},
    viewModel: HousingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    // Đăng tin / đánh dấu hết phòng ở màn con xong quay về phải thấy feed mới.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(HousingAction.Refresh) }
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is HousingEvent.CallPhone -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${event.phoneNumber}"))
                    context.startActivity(intent)
                }

                HousingEvent.ReportAccepted ->
                    snackbars.showSnackbar(context.getString(R.string.housing_report_accepted))

                HousingEvent.ReportRejected ->
                    snackbars.showSnackbar(context.getString(R.string.housing_report_rejected))

                HousingEvent.RefreshFailed ->
                    snackbars.showSnackbar(context.getString(R.string.housing_refresh_error))
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        HousingScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            onListingClick = onListingClick,
            onCreatePost = onCreatePost,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

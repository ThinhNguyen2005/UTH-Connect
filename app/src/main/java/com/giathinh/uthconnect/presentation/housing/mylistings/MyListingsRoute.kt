package com.giathinh.uthconnect.presentation.housing.mylistings

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
 * Điểm nối duy nhất của màn với Hilt (docs/CODING_STANDARDS.md mục 3.2):
 * thu UiState, hứng event một lần thành điều hướng / snackbar và gọi [MyListingsScreen] stateless.
 */
@Composable
fun MyListingsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onListingClick: (String) -> Unit = {},
    viewModel: MyListingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    // Admin duyệt / từ chối ở web xong quay về phải thấy trạng thái mới.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(MyListingsAction.Refresh) }
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is MyListingsEvent.OpenListing -> onListingClick(event.listingId)

                MyListingsEvent.RefreshFailed ->
                    snackbars.showSnackbar(context.getString(R.string.housing_refresh_error))
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        MyListingsScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

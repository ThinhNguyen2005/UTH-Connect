package com.giathinh.uthconnect.presentation.housing.detail

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R

@Composable
fun HousingDetailRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HousingDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                is HousingDetailEvent.CallPhone -> {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${event.phoneNumber}"))
                    context.startActivity(intent)
                }

                is HousingDetailEvent.OpenZalo -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    context.startActivity(intent)
                }

                is HousingDetailEvent.OpenMap -> {
                    // Có tọa độ thật thì dẫn đường theo tọa độ; tin chưa gắn vị trí (0,0)
                    // thì tìm theo địa chỉ — không mở bản đồ ở điểm placeholder ngoài khơi.
                    val destination = if (event.latitude != 0.0 || event.longitude != 0.0) {
                        "${event.latitude},${event.longitude}"
                    } else {
                        event.addressLabel
                    }
                    val url = "https://www.google.com/maps/dir/?api=1&destination=" +
                        Uri.encode(destination)
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }

                HousingDetailEvent.MarkFulfilledSuccess -> {
                    snackbars.showSnackbar(context.getString(R.string.housing_detail_fulfilled_success))
                }

                HousingDetailEvent.Deleted -> onNavigateBack()

                HousingDetailEvent.ActionFailed -> {
                    snackbars.showSnackbar(context.getString(R.string.housing_load_error_title))
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HousingDetailScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

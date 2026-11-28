package com.giathinh.uthconnect.presentation.incidents

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.core.util.ImageCompressor
import kotlinx.coroutines.launch

@Composable
fun IncidentsRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncidentsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_INCIDENT_IMAGES),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val remaining = (MAX_INCIDENT_IMAGES - uiState.form.imagePaths.size).coerceAtLeast(0)
        uris.take(remaining).forEach { uri ->
            scope.launch {
                when (val compressed = ImageCompressor.compress(context.contentResolver, uri)) {
                    is ImageCompressor.Result.Success ->
                        viewModel.onAction(IncidentsAction.UploadCompressedImage(compressed.bytes))

                    is ImageCompressor.Result.Failure ->
                        viewModel.onAction(IncidentsAction.ImageCompressionFailed())
                }
            }
        }
    }

    IncidentsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onPickPhotos = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

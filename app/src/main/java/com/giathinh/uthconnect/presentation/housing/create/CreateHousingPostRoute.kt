package com.giathinh.uthconnect.presentation.housing.create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.presentation.common.messageText

@Composable
fun CreateHousingPostRoute(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateHousingPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    // messageText cần Composition nên dịch sẵn trong composable, collect chỉ tra bảng.
    val errorMessages = ServerErrorCode.entries.associateWith { it.messageText() }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
    ) { uris ->
        uris.forEach { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAction(CreateHousingPostAction.AddPhoto(uri.toString()))
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            when (event) {
                CreateHousingPostEvent.PostCreated -> {
                    snackbars.showSnackbar(context.getString(R.string.housing_create_success))
                    onPostCreated()
                }

                is CreateHousingPostEvent.SubmitFailed -> {
                    snackbars.showSnackbar(
                        errorMessages[event.code] ?: context.getString(R.string.housing_load_error_title),
                    )
                }

                CreateHousingPostEvent.DraftSaved -> {
                    snackbars.showSnackbar(context.getString(R.string.housing_create_draft_saved))
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CreateHousingPostScreen(
            uiState = uiState,
            onAction = viewModel::onAction,
            onPickPhotos = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onNavigateBack = onNavigateBack,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

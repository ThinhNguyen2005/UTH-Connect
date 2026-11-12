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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.model.ServerErrorCode
import com.giathinh.uthconnect.presentation.common.messageText
import com.giathinh.uthconnect.presentation.housing.PICKED_ADDRESS_KEY
import com.giathinh.uthconnect.presentation.housing.PICKED_LATITUDE_KEY
import com.giathinh.uthconnect.presentation.housing.PICKED_LONGITUDE_KEY

@Composable
fun CreateHousingPostRoute(
    onNavigateBack: () -> Unit,
    onPostCreated: () -> Unit,
    onPickLocation: () -> Unit,
    savedStateHandle: SavedStateHandle,
    modifier: Modifier = Modifier,
    viewModel: CreateHousingPostViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbars = remember { SnackbarHostState() }
    // messageText cần Composition nên dịch sẵn trong composable, collect chỉ tra bảng.
    val errorMessages = ServerErrorCode.entries.associateWith { it.messageText() }
    val unknownErrorText = ServerErrorCode.Unknown.messageText()

    // Picker bản đồ ghi tọa độ vào savedStateHandle của màn này rồi pop về. Đọc một lần,
    // nạp vào form, reset khóa để lần mở picker sau không nạp lại kết quả cũ.
    val pickedLatitude by savedStateHandle.getStateFlow(PICKED_LATITUDE_KEY, 0.0).collectAsStateWithLifecycle()
    LaunchedEffect(pickedLatitude) {
        if (pickedLatitude != 0.0) {
            val longitude = savedStateHandle.get<Double>(PICKED_LONGITUDE_KEY) ?: 0.0
            val address = savedStateHandle.get<String>(PICKED_ADDRESS_KEY).orEmpty()
            viewModel.onAction(CreateHousingPostAction.ChangeLocation(pickedLatitude, longitude, address))
            savedStateHandle[PICKED_LATITUDE_KEY] = 0.0
        }
    }
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
                    // Unknown giữ chuỗi gốc server để còn dấu vết debug thay vì câu chung.
                    val text = if (event.code == ServerErrorCode.Unknown) {
                        event.detail?.takeIf { it.isNotBlank() } ?: unknownErrorText
                    } else {
                        errorMessages[event.code] ?: context.getString(R.string.housing_load_error_title)
                    }
                    snackbars.showSnackbar(text)
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
            onPickLocation = onPickLocation,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(hostState = snackbars, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

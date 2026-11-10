package com.giathinh.uthconnect.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.domain.usecase.CheckInResult
import kotlinx.serialization.Serializable

@Serializable data object ScannerDestination

fun NavGraphBuilder.scannerGraph(navController: NavHostController) {
    composable<ScannerDestination> { ScannerRoute(onBack = { navController.popBackStack() }) }
}

@Composable
fun ScannerRoute(onBack: () -> Unit, viewModel: ScannerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onAction(ScannerAction.CameraPermissionChanged(granted))
    }
    LaunchedEffect(Unit) {
        viewModel.onAction(
            ScannerAction.CameraPermissionChanged(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
            ),
        )
    }
    val snackbar = remember { SnackbarHostState() }
    val success = stringResource(R.string.scanner_success)
    val duplicate = stringResource(R.string.scanner_duplicate)
    val notRegistered = stringResource(R.string.scanner_not_registered)
    val invalid = stringResource(R.string.scanner_invalid)
    val forbidden = stringResource(R.string.scanner_forbidden)
    val error = stringResource(R.string.scanner_error)
    LaunchedEffect(viewModel, success, duplicate, notRegistered, invalid, forbidden, error) {
        viewModel.events.collect { result ->
            val message = when (result) {
                CheckInResult.SUCCESS -> success
                CheckInResult.ALREADY_CHECKED_IN -> duplicate
                CheckInResult.NOT_REGISTERED -> notRegistered
                CheckInResult.INVALID_CODE -> invalid
                CheckInResult.FORBIDDEN -> forbidden
                CheckInResult.ERROR -> error
            }
            snackbar.showSnackbar(message)
        }
    }
    Box(Modifier.fillMaxSize()) {
        ScannerScreen(
            state = state,
            onAction = viewModel::onAction,
            onBack = onBack,
            onRequestCamera = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

package com.giathinh.uthconnect.presentation.housing.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import java.io.File
import java.util.Locale

/** Chờ bản đồ đứng yên rồi mới dịch địa chỉ — kéo tới đâu geocode tới đó sẽ giật và thừa. */
private const val GEOCODE_DEBOUNCE_MS = 500L

/** Chu kỳ giả cho LocationRequest — chỉ dùng để hỏi SettingsClient, không nhận update thật. */
private const val LOCATION_REQUEST_INTERVAL_MS = 1_000L

/**
 * Route ôm phần nền tảng: quyền vị trí, GPS, Geocoder, cấu hình OSMDroid và điều hướng kết quả.
 * Screen chỉ nhận UiState và bắn action (docs/CODING_STANDARDS.md: Screen không Context).
 * Bản đồ OpenStreetMap không cần API key / billing nên không còn nhánh thiếu key.
 */
@Composable
fun HousingLocationPickerRoute(
    onNavigateBack: () -> Unit,
    onLocationConfirmed: (latitude: Double, longitude: Double, address: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HousingLocationPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val settingsClient = remember { LocationServices.getSettingsClient(context) }

    // OSMDroid bắt buộc user-agent theo package và cache tile nội bộ (không xin quyền storage).
    remember(context) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
    }

    fun fetchCurrentLocation() {
        scope.launch {
            val location = runCatching {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    CancellationTokenSource().token,
                ).await()
            }.getOrNull()
            if (location != null) {
                viewModel.onAction(
                    HousingLocationPickerAction.CurrentLocationResult(location.latitude, location.longitude),
                )
            } else {
                viewModel.onAction(HousingLocationPickerAction.CurrentLocationFailed)
            }
        }
    }

    // Quyền runtime không bật công tắc vị trí hệ thống; hỏi SettingsClient để
    // hiện hộp thoại "Bật vị trí?" thay vì bắt người dùng tự vào Cài đặt.
    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchCurrentLocation()
        } else {
            viewModel.onAction(HousingLocationPickerAction.CurrentLocationFailed)
        }
    }

    fun fetchWithLocationSettings() {
        scope.launch {
            val request = LocationSettingsRequest.Builder()
                .addLocationRequest(
                    LocationRequest.Builder(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        LOCATION_REQUEST_INTERVAL_MS,
                    ).build(),
                )
                .setAlwaysShow(true)
                .build()
            val resolvable = runCatching { settingsClient.checkLocationSettings(request).await() }
                .exceptionOrNull() as? ResolvableApiException
            if (resolvable != null) {
                resolutionLauncher.launch(IntentSenderRequest.Builder(resolvable.resolution).build())
            } else {
                fetchCurrentLocation()
            }
        }
    }

    // Android 12+ cho chọn "vị trí gần đúng": xin cả FINE lẫn COARSE, chỉ cần một trong hai được cấp.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) {
            fetchWithLocationSettings()
        } else {
            viewModel.onAction(HousingLocationPickerAction.CurrentLocationFailed)
        }
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    val handleAction: (HousingLocationPickerAction) -> Unit = { action ->
        viewModel.onAction(action)
        if (action is HousingLocationPickerAction.UseCurrentLocation) {
            if (hasLocationPermission()) {
                fetchWithLocationSettings()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }

    LaunchedEffect(uiState.latitude, uiState.longitude, uiState.hasPickedLocation) {
        if (!uiState.hasPickedLocation) return@LaunchedEffect
        delay(GEOCODE_DEBOUNCE_MS)
        val address = withContext(Dispatchers.IO) {
            runCatching {
                // Bản đồng bộ chạy trên IO; API 33+ có bản async nhưng minSdk 31
                // nên dùng bản này cho mọi máy, kết quả vẫn là địa chỉ tương đối.
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.forLanguageTag("vi"))
                    .getFromLocation(uiState.latitude, uiState.longitude, 1)
                    ?.firstOrNull()
                    ?.getAddressLine(0)
            }.getOrNull()
        }
        if (address.isNullOrBlank()) {
            viewModel.onAction(HousingLocationPickerAction.GeocodeFailed)
        } else {
            viewModel.onAction(HousingLocationPickerAction.AddressGeocoded(address))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HousingLocationPickerEvent.LocationConfirmed ->
                    onLocationConfirmed(event.latitude, event.longitude, event.address)
            }
        }
    }

    HousingLocationPickerScreen(
        uiState = uiState,
        onAction = handleAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

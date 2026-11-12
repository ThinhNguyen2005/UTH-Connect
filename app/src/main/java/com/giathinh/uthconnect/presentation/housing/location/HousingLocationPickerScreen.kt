package com.giathinh.uthconnect.presentation.housing.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.giathinh.uthconnect.R
import com.giathinh.uthconnect.core.designsystem.component.UthCard
import com.giathinh.uthconnect.core.designsystem.component.UthPrimaryButton
import com.giathinh.uthconnect.core.designsystem.theme.UthDimens
import com.giathinh.uthconnect.core.designsystem.theme.UthPreview
import com.giathinh.uthconnect.core.designsystem.theme.UthTheme
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/** Mức zoom mở màn: đủ thấy từng dãy nhà quanh cơ sở để chấm pin chính xác. */
private const val PIN_ZOOM = 16.0

/**
 * Chọn vị trí phòng: ghim cố định giữa màn hình, kéo bản đồ bên dưới ghim
 * (mượt và dễ trúng hơn kéo marker), nút GPS và ô địa chỉ Geocoder điền sẵn
 * vẫn sửa tay được. Screen không chạm Context — mọi thứ nền tảng ở Route.
 */
@Composable
fun HousingLocationPickerScreen(
    uiState: HousingLocationPickerUiState,
    onAction: (HousingLocationPickerAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { PickerTopBar(onNavigateBack = onNavigateBack) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            var mapView by remember { mutableStateOf<MapView?>(null) }
            val currentOnAction by rememberUpdatedState(onAction)

            // GPS trả kết quả → nhảy tâm bản đồ tới đó một lần; kéo tay không đi qua nhánh này.
            LaunchedEffect(uiState.gpsFixCount) {
                if (uiState.gpsFixCount > 0) {
                    mapView?.controller?.setCenter(GeoPoint(uiState.latitude, uiState.longitude))
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(PIN_ZOOM)
                        controller.setCenter(GeoPoint(uiState.latitude, uiState.longitude))
                        addMapListener(
                            object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    val center = event?.source?.mapCenter ?: return false
                                    currentOnAction(
                                        HousingLocationPickerAction.PinMoved(center.latitude, center.longitude),
                                    )
                                    return false
                                }

                                override fun onZoom(event: ZoomEvent?): Boolean = false
                            },
                        )
                        mapView = this
                    }
                },
                onRelease = { it.onDetach() },
            )
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = stringResource(R.string.housing_location_pin_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = -UthDimens.Size.UtilityFeatureIcon / 2)
                    .size(UthDimens.Size.UtilityFeatureIcon),
            )
            PickerBottomPanel(
                uiState = uiState,
                onAction = onAction,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun PickerTopBar(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(
                start = UthDimens.Spacing.Sm,
                end = UthDimens.Spacing.Lg,
                top = UthDimens.Spacing.Sm,
                bottom = UthDimens.Spacing.Sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = stringResource(R.string.housing_location_picker_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun PickerBottomPanel(
    uiState: HousingLocationPickerUiState,
    onAction: (HousingLocationPickerAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(UthDimens.Spacing.Lg),
    ) {
        UthCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.housing_location_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
            OutlinedTextField(
                value = uiState.address,
                onValueChange = { onAction(HousingLocationPickerAction.ChangeAddress(it)) },
                label = { Text(stringResource(R.string.housing_location_address_label)) },
                supportingText = if (uiState.isGeocoding) {
                    { Text(stringResource(R.string.housing_location_geocoding)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                singleLine = false,
                maxLines = 2,
            )
            if (uiState.failure != null) {
                Spacer(modifier = Modifier.height(UthDimens.Spacing.Sm))
                Text(
                    text = stringResource(
                        when (uiState.failure) {
                            HousingLocationPickerFailure.LocationUnavailable -> R.string.housing_location_failed
                            HousingLocationPickerFailure.GeocodeFailed -> R.string.housing_location_geocode_failed
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(UthDimens.Spacing.Md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(UthDimens.Spacing.Md),
            ) {
                OutlinedButton(
                    onClick = { onAction(HousingLocationPickerAction.UseCurrentLocation) },
                    enabled = !uiState.isLocating,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = UthDimens.Size.ButtonHeight),
                    shape = MaterialTheme.shapes.small,
                ) {
                    if (uiState.isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(UthDimens.Size.ProgressInline),
                            strokeWidth = UthDimens.Size.ProgressInlineStroke,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(UthDimens.Size.IconInline),
                        )
                    }
                    Spacer(modifier = Modifier.width(UthDimens.Spacing.Sm))
                    Text(stringResource(R.string.housing_location_current))
                }
                UthPrimaryButton(
                    text = stringResource(R.string.housing_location_confirm),
                    onClick = { onAction(HousingLocationPickerAction.Confirm) },
                    enabled = uiState.hasPickedLocation && !uiState.isLocating,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PickerBottomPanelPreview() {
    UthPreview {
        PickerBottomPanel(
            uiState = HousingLocationPickerUiState(
                hasPickedLocation = true,
                address = "138 Võ Oanh, Phường Thạnh Mỹ Tây, TP. Hồ Chí Minh",
            ),
            onAction = {},
        )
    }
}

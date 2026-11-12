package com.giathinh.uthconnect.presentation.housing

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.giathinh.uthconnect.presentation.housing.create.CreateHousingPostRoute
import com.giathinh.uthconnect.presentation.housing.detail.HousingDetailRoute
import com.giathinh.uthconnect.presentation.housing.location.HousingLocationPickerRoute
import kotlinx.serialization.Serializable

/** Màn con của tab Tiện ích; bottom bar tự ẩn theo destination hiện tại. */
@Serializable
data object HousingDestination

@Serializable
data class HousingDetailDestination(val listingId: String)

@Serializable
data object CreateHousingPostDestination

@Serializable
data object HousingLocationPickerDestination

/**
 * Khóa kết quả picker bản đồ ghi vào `savedStateHandle` của màn đăng tin
 * (`previousBackStackEntry`). Dùng primitive để sống sót qua process death.
 */
const val PICKED_LATITUDE_KEY = "picked_latitude"
const val PICKED_LONGITUDE_KEY = "picked_longitude"
const val PICKED_ADDRESS_KEY = "picked_address"

fun NavGraphBuilder.housingGraph(navController: NavHostController) {
    composable<HousingDestination> {
        HousingRoute(
            onNavigateBack = { navController.popBackStack() },
            onListingClick = { listingId ->
                navController.navigate(HousingDetailDestination(listingId = listingId))
            },
            onCreatePost = {
                navController.navigate(CreateHousingPostDestination)
            },
        )
    }

    composable<HousingDetailDestination> {
        HousingDetailRoute(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<CreateHousingPostDestination> { entry ->
        CreateHousingPostRoute(
            onNavigateBack = { navController.popBackStack() },
            onPostCreated = { navController.popBackStack() },
            onPickLocation = { navController.navigate(HousingLocationPickerDestination) },
            savedStateHandle = entry.savedStateHandle,
        )
    }

    composable<HousingLocationPickerDestination> {
        HousingLocationPickerRoute(
            onNavigateBack = { navController.popBackStack() },
            onLocationConfirmed = { latitude, longitude, address ->
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    this[PICKED_LATITUDE_KEY] = latitude
                    this[PICKED_LONGITUDE_KEY] = longitude
                    this[PICKED_ADDRESS_KEY] = address
                }
                navController.popBackStack()
            },
        )
    }
}

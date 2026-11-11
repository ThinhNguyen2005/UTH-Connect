package com.giathinh.uthconnect.presentation.housing

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.giathinh.uthconnect.presentation.housing.create.CreateHousingPostRoute
import com.giathinh.uthconnect.presentation.housing.detail.HousingDetailRoute
import kotlinx.serialization.Serializable

/** Màn con của tab Tiện ích; bottom bar tự ẩn theo destination hiện tại. */
@Serializable
data object HousingDestination

@Serializable
data class HousingDetailDestination(val listingId: String)

@Serializable
data object CreateHousingPostDestination

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

    composable<CreateHousingPostDestination> {
        CreateHousingPostRoute(
            onNavigateBack = { navController.popBackStack() },
            onPostCreated = { navController.popBackStack() },
        )
    }
}

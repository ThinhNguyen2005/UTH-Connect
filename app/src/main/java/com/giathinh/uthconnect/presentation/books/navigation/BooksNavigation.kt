package com.giathinh.uthconnect.presentation.books.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.giathinh.uthconnect.presentation.books.BooksRoute
import kotlinx.serialization.Serializable

@Serializable object BooksGraph

@Serializable object BooksDestination

fun NavGraphBuilder.booksGraph(navController: NavController) {
    navigation<BooksGraph>(startDestination = BooksDestination) {
        composable<BooksDestination> {
            BooksRoute(onBack = { navController.popBackStack() })
        }
    }
}

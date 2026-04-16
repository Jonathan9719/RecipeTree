package org.maxwelltech.recipetree

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.maxwelltech.recipetree.ui.screens.RecipeListScreen

// Type-safe route definitions
sealed interface Route {

    @Serializable
    data object RecipeList : Route

    @Serializable
    data class RecipeDetail(val recipeId: String) : Route

    @Serializable
    data class RecipeEdit(val recipeId: String? = null) : Route

    @Serializable
    data object CookbookList : Route

    @Serializable
    data class CookbookDetail(val cookbookId: String) : Route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.RecipeList
    ) {
        composable<Route.RecipeList> {
            RecipeListScreen(
                userId = "temp_user", // will come from auth later
                navController = navController
            )
        }

        composable<Route.RecipeDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RecipeDetail>()
            // RecipeDetailScreen(recipeId = route.recipeId, navController = navController)
        }

        composable<Route.RecipeEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RecipeEdit>()
            // RecipeEditScreen(recipeId = route.recipeId, navController = navController)
        }

        composable<Route.CookbookList> {
            // CookbookListScreen(navController)
        }

        composable<Route.CookbookDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CookbookDetail>()
            // CookbookDetailScreen(cookbookId = route.cookbookId, navController = navController)
        }
    }
}
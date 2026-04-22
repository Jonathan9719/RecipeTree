package org.maxwelltech.recipetree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.maxwelltech.recipetree.ui.screens.AddRecipesToCookbookScreen
import org.maxwelltech.recipetree.ui.screens.CookbookDetailScreen
import org.maxwelltech.recipetree.ui.screens.CookbookEditScreen
import org.maxwelltech.recipetree.ui.screens.CookbookListScreen
import org.maxwelltech.recipetree.ui.screens.LoginScreen
import org.maxwelltech.recipetree.ui.screens.RecipeDetailScreen
import org.maxwelltech.recipetree.ui.screens.RecipeEditScreen
import org.maxwelltech.recipetree.ui.screens.RecipeListScreen
import org.maxwelltech.recipetree.ui.screens.SignUpScreen
import org.maxwelltech.recipetree.viewmodel.AuthViewModel

sealed interface Route {

    @Serializable
    data object Login : Route

    @Serializable
    data object SignUp : Route

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

    @Serializable
    data class CookbookEdit(val cookbookId: String? = null) : Route

    @Serializable
    data class AddRecipesToCookbook(val cookbookId: String) : Route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = remember { AuthViewModel(AppContainer.authRepository) }
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Determine start destination based on auth state
    val startDestination = if (currentUser != null) Route.RecipeList else Route.Login

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Route.Login> {
            LoginScreen(navController = navController, viewModel = authViewModel)
        }

        composable<Route.SignUp> {
            SignUpScreen(navController = navController, viewModel = authViewModel)
        }

        composable<Route.RecipeList> {
            val user = currentUser
            LaunchedEffect(user) {
                if (user == null) {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.RecipeList) { inclusive = true }
                    }
                }
            }
            if (user != null) {
                RecipeListScreen(
                    userId = user.id,
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }

        composable<Route.RecipeDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RecipeDetail>()
            val user = currentUser
            if (user != null) {
                RecipeDetailScreen(
                    recipeId = route.recipeId,
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.RecipeEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.RecipeEdit>()
            val user = currentUser
            if (user != null) {
                RecipeEditScreen(
                    recipeId = route.recipeId,
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.CookbookList> {
            val user = currentUser
            LaunchedEffect(user) {
                if (user == null) {
                    navController.navigate(Route.Login) {
                        popUpTo(Route.CookbookList) { inclusive = true }
                    }
                }
            }
            if (user != null) {
                CookbookListScreen(
                    userId = user.id,
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }

        composable<Route.CookbookDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CookbookDetail>()
            val user = currentUser
            if (user != null) {
                CookbookDetailScreen(
                    cookbookId = route.cookbookId,
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.CookbookEdit> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.CookbookEdit>()
            val user = currentUser
            if (user != null) {
                CookbookEditScreen(
                    cookbookId = route.cookbookId,
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.AddRecipesToCookbook> { backStackEntry ->
            val route = backStackEntry.toRoute<Route.AddRecipesToCookbook>()
            val user = currentUser
            if (user != null) {
                AddRecipesToCookbookScreen(
                    cookbookId = route.cookbookId,
                    userId = user.id,
                    navController = navController
                )
            }
        }
    }
}
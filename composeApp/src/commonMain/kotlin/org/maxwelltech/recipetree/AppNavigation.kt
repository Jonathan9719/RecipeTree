package org.maxwelltech.recipetree

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.maxwelltech.recipetree.ui.components.AppBottomBar
import org.maxwelltech.recipetree.ui.components.RootTab
import org.maxwelltech.recipetree.ui.screens.AddRecipesToCookbookScreen
import org.maxwelltech.recipetree.ui.screens.CookbookDetailScreen
import org.maxwelltech.recipetree.ui.screens.CookbookEditScreen
import org.maxwelltech.recipetree.ui.screens.CookbookListScreen
import org.maxwelltech.recipetree.ui.screens.JoinCookbookScreen
import org.maxwelltech.recipetree.ui.screens.LoginScreen
import org.maxwelltech.recipetree.ui.screens.ProfileScreen
import org.maxwelltech.recipetree.ui.screens.SettingsScreen
import org.maxwelltech.recipetree.ui.screens.RecipeDetailScreen
import org.maxwelltech.recipetree.ui.screens.RecipeEditScreen
import org.maxwelltech.recipetree.ui.screens.RecipeListScreen
import org.maxwelltech.recipetree.ui.screens.SignUpScreen
import org.maxwelltech.recipetree.ui.screens.WelcomeScreen
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

    @Serializable
    data object JoinCookbook : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Welcome : Route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = remember { AuthViewModel(AppContainer.authRepository) }
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    // Determine start destination based on auth + welcome state.
    // currentUser is null until the FirebaseAuthRepository's async init
    // finishes its Firestore profile fetch, so on a cold start the
    // startDestination resolves to Login briefly — that flashes for a tick
    // before the auth-state listener emits the cached user. Acceptable.
    val startDestination = when {
        currentUser == null -> Route.Login
        currentUser?.seenWelcome != true -> Route.Welcome
        else -> Route.RecipeList
    }

    // Track the current destination so the bottom bar can compute selection
    // state and hide itself on pre-auth + Welcome routes.
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = tabForRoute(currentBackStackEntry?.destination)

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                AppBottomBar(
                    navController = navController,
                    currentTab = currentTab,
                    currentUser = currentUser
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
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
                } else if (user.seenWelcome != true) {
                    // Cached-auth sign-in resolves into RecipeList by the
                    // startDestination evaluation, but seenWelcome arrives a
                    // moment later via the Firestore profile fetch. Catch
                    // the late false here and redirect.
                    navController.navigate(Route.Welcome) {
                        popUpTo(Route.RecipeList) { inclusive = true }
                    }
                }
            }
            if (user != null && user.seenWelcome == true) {
                RecipeListScreen(
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.Welcome> {
            WelcomeScreen(
                navController = navController,
                authViewModel = authViewModel
            )
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
                    navController = navController
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

        composable<Route.JoinCookbook> {
            val user = currentUser
            if (user != null) {
                JoinCookbookScreen(
                    userId = user.id,
                    navController = navController
                )
            }
        }

        composable<Route.Profile> {
            val user = currentUser
            LaunchedEffect(user) {
                // Profile is a tab root, so signing out from here (or having
                // the auth state otherwise clear) needs an explicit redirect
                // to Login — there's no LaunchedEffect-on-RecipeList to catch
                // it like cached-auth flows have.
                if (user == null) {
                    navController.navigate(Route.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            if (user != null) {
                ProfileScreen(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }

        composable<Route.Settings> {
            val user = currentUser
            if (user != null) {
                SettingsScreen(
                    userId = user.id,
                    navController = navController,
                    onAccountDeleted = {
                        // Firebase's authStateChanged will emit null shortly,
                        // but we route to Login immediately so the user can't
                        // back-navigate into the half-gone session — and clear
                        // the entire backstack so there's nothing to land on.
                        navController.navigate(Route.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
        }
    }
}

/**
 * Map a NavDestination to the bottom bar's RootTab, or null when the bar
 * should be hidden entirely (Login / SignUp / Welcome — pre-tab routes).
 *
 * Used twice: once to drive selection state inside AppBottomBar, and once
 * to drive the Scaffold's bottomBar visibility (the bar isn't rendered at
 * all when this returns null).
 */
private fun tabForRoute(destination: NavDestination?): RootTab? {
    val route = destination?.route ?: return null
    val typeName = route.substringBefore('/').substringAfterLast('.')
    return when (typeName) {
        "RecipeList", "RecipeDetail", "RecipeEdit" -> RootTab.Recipes
        "CookbookList", "CookbookDetail", "CookbookEdit",
        "AddRecipesToCookbook", "JoinCookbook" -> RootTab.Cookbooks
        "Profile", "Settings" -> RootTab.Profile
        else -> null  // Login / SignUp / Welcome -> bar hidden
    }
}
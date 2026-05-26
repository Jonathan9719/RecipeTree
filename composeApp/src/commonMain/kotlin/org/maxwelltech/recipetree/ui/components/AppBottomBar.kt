package org.maxwelltech.recipetree.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.ui.theme.Sage

/** The three top-level destinations the bottom bar swaps between. */
enum class RootTab { Recipes, Cookbooks, Profile }

/**
 * Bottom navigation bar with three tabs: Recipes, Cookbooks, Profile. Lives
 * inside the outer Scaffold in AppNavigation so it persists across the
 * tab-rooted destinations and any screens nested under them. Hidden on the
 * pre-auth + Welcome routes — see AppNavigation's tabForRoute() classifier.
 *
 * Each tab uses the canonical Compose Navigation tab pattern:
 *   popUpTo(graph.startDestination) { saveState = true } + restoreState +
 *   launchSingleTop. Per-tab back stacks are saved and restored as the
 *   user swaps tabs, so deep-navigating in one tab and returning later
 *   lands them on the screen they left.
 */
@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentTab: RootTab?,
    currentUser: User?
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null) },
            label = { Text("Recipes") },
            selected = currentTab == RootTab.Recipes,
            onClick = { navController.switchTab(Route.RecipeList) },
            colors = barItemColors()
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Outlined.LibraryBooks, contentDescription = null) },
            label = { Text("Cookbooks") },
            selected = currentTab == RootTab.Cookbooks,
            onClick = { navController.switchTab(Route.CookbookList) },
            colors = barItemColors()
        )
        NavigationBarItem(
            icon = {
                // Reuse the existing UserAvatar so the Profile slot carries
                // the user's own initial / avatar rather than a generic
                // person icon — keeps the "this is me" signal that the old
                // top-bar avatar IconButton provided.
                UserAvatar(
                    displayName = currentUser?.displayName.orEmpty(),
                    email = currentUser?.email.orEmpty(),
                    avatarUrl = currentUser?.avatarUrl,
                    size = 24.dp,
                    textStyle = MaterialTheme.typography.labelMedium
                )
            },
            label = { Text("Profile") },
            selected = currentTab == RootTab.Profile,
            onClick = { navController.switchTab(Route.Profile) },
            colors = barItemColors()
        )
    }
}

/**
 * Sage-tinted selection colors so the bar matches the rest of the app's
 * accent treatment (tag chips, Welcome dots, Post button, etc.) instead of
 * Material3's default primary tint.
 */
@Composable
private fun barItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = Sage,
    indicatorColor = Sage,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** Canonical tab-switch navigation: save state, restore state, single-top. */
private fun NavHostController.switchTab(destination: Any) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

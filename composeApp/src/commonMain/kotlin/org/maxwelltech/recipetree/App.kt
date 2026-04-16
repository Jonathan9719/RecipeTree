package org.maxwelltech.recipetree

import androidx.compose.runtime.Composable
import org.maxwelltech.recipetree.ui.theme.RecipeTreeTheme

@Composable
fun App() {
    RecipeTreeTheme {
        AppNavigation()
    }
}
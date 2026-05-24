package org.maxwelltech.recipetree

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import org.maxwelltech.recipetree.ui.theme.RecipeTreeTheme

@Composable
fun App() {
    // Coil 3 no longer auto-registers a network fetcher — we have to wire
    // one explicitly or AsyncImage falls through to its empty state on every
    // https:// model (Firebase Storage download URLs included). The ktor3
    // adapter picks up whichever ktor engine is on each platform's classpath
    // (OkHttp on Android, Darwin on iOS — see composeApp/build.gradle.kts).
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }

    RecipeTreeTheme {
        AppNavigation()
    }
}

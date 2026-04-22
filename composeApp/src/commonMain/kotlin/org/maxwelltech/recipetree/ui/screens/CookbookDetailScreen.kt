package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.CookbookVisibility
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.ui.components.RecipeCard
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.CookbookDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookDetailScreen(
    cookbookId: String,
    userId: String,
    navController: NavController,
    viewModel: CookbookDetailViewModel = remember {
        CookbookDetailViewModel(
            cookbookRepository = AppContainer.cookbookRepository,
            recipeRepository = AppContainer.recipeRepository
        )
    }
) {
    val cookbook by viewModel.cookbook.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(cookbookId) {
        viewModel.loadCookbook(cookbookId)
        viewModel.observeRecipes(cookbookId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && cookbook == null -> {
                    CircularProgressIndicator(
                        color = Sage,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null && cookbook == null -> {
                    Text(
                        text = error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                cookbook != null -> {
                    CookbookDetailContent(
                        cookbook = cookbook!!,
                        recipes = recipes,
                        userId = userId,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
private fun CookbookDetailContent(
    cookbook: Cookbook,
    recipes: List<Recipe>,
    userId: String,
    navController: NavController
) {
    val isOwner = cookbook.ownerId == userId

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Hero with overlaid top bar
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (cookbook.coverPhotoUrl != null) {
                    AsyncImage(
                        model = cookbook.coverPhotoUrl,
                        contentDescription = "Cookbook cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📖", fontSize = 48.sp)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text(
                                text = "← Back",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (isOwner) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ) {
                            TextButton(
                                onClick = {
                                    navController.navigate(
                                        Route.CookbookEdit(cookbookId = cookbook.id)
                                    )
                                }
                            ) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Sage
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header block
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = cookbook.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (!cookbook.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cookbook.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaPill(
                        text = visibilityLabel(cookbook.visibility),
                        containerColor = when (cookbook.visibility) {
                            CookbookVisibility.PUBLIC -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = when (cookbook.visibility) {
                            CookbookVisibility.PUBLIC -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        borderColor = SageLight
                    )

                    val memberCount = cookbook.memberIds.size
                    if (memberCount > 0) {
                        MetaPill(
                            text = if (memberCount == 1) "1 member" else "$memberCount members",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "RECIPES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Sage,
                    letterSpacing = 0.08.sp
                )
            }
        }

        // Recipes
        if (recipes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recipes in this cookbook yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recipes) { recipe ->
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    RecipeCard(
                        recipe = recipe,
                        cookbookNames = emptyList(),
                        authorName = if (recipe.ownerId == userId) "you" else "author",
                        onClick = {
                            navController.navigate(
                                Route.RecipeDetail(recipeId = recipe.id)
                            )
                        }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun MetaPill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun visibilityLabel(visibility: CookbookVisibility): String = when (visibility) {
    CookbookVisibility.PRIVATE -> "private"
    CookbookVisibility.UNLISTED -> "unlisted"
    CookbookVisibility.PUBLIC -> "public"
}

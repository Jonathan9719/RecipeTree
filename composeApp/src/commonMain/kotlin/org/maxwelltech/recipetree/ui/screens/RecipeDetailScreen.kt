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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.RecipeDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    userId: String,
    navController: NavController,
    viewModel: RecipeDetailViewModel = remember { RecipeDetailViewModel(AppContainer.recipeRepository) }
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(recipeId) {
        viewModel.loadRecipe(recipeId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = Sage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            error != null -> {
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
            recipe != null -> {
                RecipeDetailContent(
                    recipe = recipe!!,
                    userId = userId,
                    navController = navController,
                    onDelete = {
                        viewModel.deleteRecipe(recipe!!.id) {
                            navController.popBackStack()
                        }
                    }
                )
            }
            else -> {
                // Initial null state before load completes
                CircularProgressIndicator(
                    color = Sage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    userId: String,
    navController: NavController,
    onDelete: () -> Unit
) {
    val isOwner = recipe.ownerId == userId

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero photo
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (recipe.photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = recipe.photoUrls.first(),
                        contentDescription = "Recipe photo",
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
                        Text(text = "🌿", fontSize = 48.sp)
                    }
                }

                // Top bar overlaid on hero
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
                                        Route.RecipeEdit(recipeId = recipe.id)
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

        // Content
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title row
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Meta row — servings + tags + private badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Servings pill
                    MetaPill(
                        text = "${recipe.servings} servings",
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        borderColor = MaterialTheme.colorScheme.outline
                    )

                    // Tags
                    recipe.tags.take(3).forEach { tag ->
                        MetaPill(
                            text = tag,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            borderColor = SageLight
                        )
                    }

                    // Private badge
                    if (recipe.isPrivate) {
                        MetaPill(
                            text = "private",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            borderColor = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Ingredients section
                if (recipe.ingredients.isNotEmpty()) {
                    SectionHeader(title = "Ingredients")
                    Spacer(modifier = Modifier.height(8.dp))
                    recipe.ingredients.forEach { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Sage)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Steps section
                if (recipe.steps.isNotEmpty()) {
                    SectionHeader(title = "Steps")
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Steps as individual lazy items for performance
        items(recipe.steps.mapIndexed { index, step -> Pair(index, step) }) { (index, step) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = CircleShape,
                    color = Sage,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Sage,
        letterSpacing = 0.08.sp
    )
}
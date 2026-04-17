package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.data.model.Ingredient
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.RecipeEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: String? = null,
    userId: String,
    navController: NavController,
    viewModel: RecipeEditViewModel = remember { RecipeEditViewModel(AppContainer.recipeRepository) }
) {
    val recipe by viewModel.recipe.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    // Load existing recipe if editing
    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            viewModel.loadRecipe(recipeId)
        }
    }

    // Navigate back on successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (recipeId == null) "New Recipe" else "Edit Recipe",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveRecipe(userId) },
                        enabled = recipe.title.isNotBlank() && !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Sage),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Error
            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Basic Info section
            SectionCard(title = "Basic info") {
                OutlinedTextField(
                    value = recipe.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recipe.description ?: "",
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors()
                )
            }

            // Ingredients section
            SectionCard(title = "Ingredients") {
                recipe.ingredients.forEachIndexed { index, ingredient ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = ingredient.name,
                            onValueChange = { newName ->
                                val updated = recipe.ingredients.toMutableList()
                                updated[index] = ingredient.copy(name = newName)
                                viewModel.updateIngredients(updated)
                            },
                            label = { Text("e.g. 2 cups flour") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val updated = recipe.ingredients.toMutableList()
                                updated.removeAt(index)
                                viewModel.updateIngredients(updated)
                            }
                        ) {
                            Text(
                                text = "×",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                TextButton(
                    onClick = {
                        val updated = recipe.ingredients.toMutableList()
                        updated.add(Ingredient())
                        viewModel.updateIngredients(updated)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+ Add ingredient",
                        color = Sage,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Steps section
            SectionCard(title = "Steps") {
                recipe.steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .size(24.dp)
                                .then(
                                    Modifier.padding(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = step,
                            onValueChange = { newStep ->
                                val updated = recipe.steps.toMutableList()
                                updated[index] = newStep
                                viewModel.updateSteps(updated)
                            },
                            label = { Text("Describe this step") },
                            minLines = 2,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = textFieldColors()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val updated = recipe.steps.toMutableList()
                                updated.removeAt(index)
                                viewModel.updateSteps(updated)
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = "×",
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                TextButton(
                    onClick = {
                        val updated = recipe.steps.toMutableList()
                        updated.add("")
                        viewModel.updateSteps(updated)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+ Add step",
                        color = Sage,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Settings section
            SectionCard(title = "Settings") {
                // Tags
                OutlinedTextField(
                    value = recipe.tags.joinToString(", "),
                    onValueChange = { input ->
                        val tags = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        viewModel.updateTags(tags)
                    },
                    label = { Text("Tags (comma separated)") },
                    placeholder = { Text("dessert, baking, family") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Servings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Servings",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (recipe.servings > 1)
                                    viewModel.updateServings(recipe.servings - 1)
                            }
                        ) {
                            Text(
                                text = "−",
                                fontSize = 20.sp,
                                color = Sage
                            )
                        }
                        Text(
                            text = "${recipe.servings}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { viewModel.updateServings(recipe.servings + 1) }
                        ) {
                            Text(
                                text = "+",
                                fontSize = 20.sp,
                                color = Sage
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Private toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Private recipe",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Only visible to you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = recipe.isPrivate,
                        onCheckedChange = { viewModel.updateIsPrivate(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = Sage,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.08.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Sage,
    focusedLabelColor = Sage,
    cursorColor = Sage
)
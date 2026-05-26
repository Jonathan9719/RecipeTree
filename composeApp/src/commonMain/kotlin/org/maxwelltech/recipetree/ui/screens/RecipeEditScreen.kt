package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.Ingredient
import org.maxwelltech.recipetree.platform.rememberPhotoPicker
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.RecipeEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: String? = null,
    userId: String,
    navController: NavController,
    viewModel: RecipeEditViewModel = remember {
        RecipeEditViewModel(
            recipeRepository = AppContainer.recipeRepository,
            cookbookRepository = AppContainer.cookbookRepository,
            photoStorageRepository = AppContainer.photoStorageRepository,
            recipeImportRepository = AppContainer.recipeImportRepository
        )
    }
) {
    val recipe by viewModel.recipe.collectAsState()
    val availableCookbooks by viewModel.availableCookbooks.collectAsState()
    val selectedCookbookIds by viewModel.selectedCookbookIds.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    val photoError by viewModel.photoError.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val imageImportFailed by viewModel.imageImportFailed.collectAsState()

    val photoPicker = rememberPhotoPicker { bytes -> viewModel.uploadPhoto(bytes) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }

    // Auto-dismiss the import dialog when the VM signals success (isImporting
    // flips false with no error). Cancellation via button leaves importError
    // null too but we only close in that path explicitly via onDismiss.
    LaunchedEffect(isImporting, importError) {
        if (showImportDialog && !isImporting && importError == null && recipe.title.isNotBlank()) {
            showImportDialog = false
            importUrl = ""
        }
    }

    // Load existing recipe if editing
    LaunchedEffect(recipeId) {
        if (recipeId != null) {
            viewModel.loadRecipe(recipeId)
        }
    }

    LaunchedEffect(userId) {
        viewModel.loadAvailableCookbooks(userId)
    }

    // Navigate back on successful save
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (recipeId == null) "New Recipe" else "Edit Recipe",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Sage,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.saveRecipe(userId) },
                            enabled = recipe.title.isNotBlank() && !isSaving,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Save",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
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

            // Import section — only shown when creating a new recipe. Edit
            // mode loaded an existing recipe so the import path isn't useful
            // (it would overwrite the user's data).
            if (recipeId == null) {
                SectionCard(title = "Import") {
                    Text(
                        text = "Got a recipe link? Paste it and we'll fill in the title, ingredients, and steps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Import from web",
                            style = MaterialTheme.typography.labelLarge,
                            color = Sage
                        )
                    }
                }
            }

            // Photo section — single hero tile. Tap to add (or replace),
            // small "Remove" pill in the corner when a photo is set, spinner
            // overlay while an upload is in flight.
            SectionCard(title = "Photo") {
                PhotoTile(
                    photoUrl = recipe.photoUrls.firstOrNull(),
                    isUploading = isUploadingPhoto,
                    onTap = { photoPicker.launch() },
                    onRemove = { viewModel.removePhoto() }
                )
                if (photoError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = photoError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearPhotoError() }) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                if (imageImportFailed) {
                    // Non-blocking: the text imported fine; just the image
                    // re-upload failed. User can add their own via the picker.
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Couldn't import the photo — add your own from the tile above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearImageImportFailed() }) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
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

            // Cookbooks section — pick which cookbooks this recipe belongs to
            SectionCard(title = "Cookbooks") {
                if (availableCookbooks.isEmpty()) {
                    Text(
                        text = "Create a cookbook first to group recipes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    CookbookChips(
                        cookbooks = availableCookbooks,
                        selectedIds = selectedCookbookIds,
                        onToggle = { viewModel.toggleCookbook(it) }
                    )
                }
            }

            // Settings section
            SectionCard(title = "Settings") {
                // Tags — keep raw text locally so separators aren't eaten mid-typing
                var tagsInput by remember(recipe.id) {
                    mutableStateOf(recipe.tags.joinToString(", "))
                }
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { input ->
                        tagsInput = input
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

            if (recipeId != null) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isDeleting && !isSaving,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Delete recipe",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog && recipeId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete recipe?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteRecipe(recipeId) {
                            navController.popBackStack<Route.RecipeList>(inclusive = false)
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportDialog) {
        ImportRecipeDialog(
            url = importUrl,
            onUrlChange = { importUrl = it },
            isImporting = isImporting,
            error = importError,
            onImport = { viewModel.importFromUrl(importUrl) },
            onDismiss = {
                showImportDialog = false
                importUrl = ""
                viewModel.clearImportError()
            },
            onDismissError = { viewModel.clearImportError() }
        )
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

@Composable
private fun CookbookChips(
    cookbooks: List<Cookbook>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cookbooks, key = { it.id }) { cookbook ->
            val isSelected = cookbook.id in selectedIds
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Sage else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 0.5.dp,
                    color = if (isSelected) Sage else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.clickable { onToggle(cookbook.id) }
            ) {
                Text(
                    text = cookbook.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PhotoTile(
    photoUrl: String?,
    isUploading: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            // Tap to add OR tap to replace. Disabled during upload so a
            // double-tap can't race two transactions.
            .clickable(enabled = !isUploading, onClick = onTap)
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Recipe photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Remove pill in the top-right corner. Sits on a translucent
            // surface so it stays legible against any underlying photo.
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                TextButton(
                    onClick = onRemove,
                    enabled = !isUploading
                ) {
                    Text(
                        text = "Remove",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            // Empty state — a plus glyph and a label that doubles as
            // affordance hint. No icons pack imported in this project so
            // we use the same emoji/symbol pattern used elsewhere.
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "+", fontSize = 32.sp, color = Sage)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add photo",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Upload-in-flight overlay. Sits above any state — empty or loaded.
        if (isUploading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Sage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportRecipeDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    isImporting: Boolean,
    error: String?,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Paste a recipe link",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(
                            text = "https://www.allrecipes.com/...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Text(text = "🔗", style = MaterialTheme.typography.bodyLarge)
                    },
                    singleLine = true,
                    enabled = !isImporting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = RoundedCornerShape(10.dp),
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = url.isNotBlank() && !isImporting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Sage,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(text = "Import", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting
            ) {
                Text(text = "Cancel", style = MaterialTheme.typography.labelLarge)
            }
        }
    )
}
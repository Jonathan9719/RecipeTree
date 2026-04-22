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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.data.model.CookbookVisibility
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.viewmodel.CookbookEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookEditScreen(
    cookbookId: String? = null,
    userId: String,
    navController: NavController,
    viewModel: CookbookEditViewModel = remember {
        CookbookEditViewModel(AppContainer.cookbookRepository)
    }
) {
    val cookbook by viewModel.cookbook.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cookbookId) {
        if (cookbookId != null) {
            viewModel.loadCookbook(cookbookId)
        }
    }

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
                        text = if (cookbookId == null) "New Cookbook" else "Edit Cookbook",
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
                            onClick = { viewModel.saveCookbook(userId) },
                            enabled = cookbook.name.isNotBlank() && !isSaving,
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

            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "Basic info") {
                OutlinedTextField(
                    value = cookbook.name,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = cookbookTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cookbook.description ?: "",
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = cookbookTextFieldColors()
                )
            }

            SectionCard(title = "Visibility") {
                VisibilitySelector(
                    selected = cookbook.visibility,
                    onSelect = { viewModel.updateVisibility(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = visibilityDescription(cookbook.visibility),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (cookbookId != null) {
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
                            text = "Delete cookbook",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog && cookbookId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete cookbook?") },
            text = { Text("This removes the cookbook for all members. Recipes inside it are not deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCookbook(cookbookId) {
                            navController.popBackStack<Route.CookbookList>(inclusive = false)
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
}

@Composable
private fun VisibilitySelector(
    selected: CookbookVisibility,
    onSelect: (CookbookVisibility) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CookbookVisibility.entries.forEach { option ->
            VisibilityOption(
                label = option.label(),
                isSelected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VisibilityOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) Sage else MaterialTheme.colorScheme.surface
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary
             else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = fg),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun CookbookVisibility.label(): String = when (this) {
    CookbookVisibility.PRIVATE -> "Private"
    CookbookVisibility.UNLISTED -> "Unlisted"
    CookbookVisibility.PUBLIC -> "Public"
}

private fun visibilityDescription(visibility: CookbookVisibility): String = when (visibility) {
    CookbookVisibility.PRIVATE -> "Only members can see this cookbook."
    CookbookVisibility.UNLISTED -> "Anyone with the link can view, but it won't appear in search."
    CookbookVisibility.PUBLIC -> "Discoverable by anyone."
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
private fun cookbookTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Sage,
    focusedLabelColor = Sage,
    cursorColor = Sage
)

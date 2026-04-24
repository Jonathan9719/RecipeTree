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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.maxwelltech.recipetree.data.model.User
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
            recipeRepository = AppContainer.recipeRepository,
            userProfileRepository = AppContainer.userProfileRepository
        )
    }
) {
    val cookbook by viewModel.cookbook.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isProcessingMember by viewModel.isProcessingMember.collectAsState()
    val memberActionError by viewModel.memberActionError.collectAsState()

    LaunchedEffect(cookbookId) {
        viewModel.observeCookbook(cookbookId)
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
                        members = members,
                        userId = userId,
                        navController = navController,
                        isProcessingMember = isProcessingMember,
                        memberActionError = memberActionError,
                        onRemoveMember = { memberId ->
                            viewModel.removeMember(cookbookId = cookbook!!.id, userId = memberId)
                        },
                        onLeaveCookbook = {
                            viewModel.leaveCookbook(
                                cookbookId = cookbook!!.id,
                                userId = userId,
                                onSuccess = {
                                    navController.popBackStack<Route.CookbookList>(inclusive = false)
                                }
                            )
                        }
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
    members: List<User>,
    userId: String,
    navController: NavController,
    isProcessingMember: Boolean,
    memberActionError: String?,
    onRemoveMember: (String) -> Unit,
    onLeaveCookbook: () -> Unit
) {
    val isOwner = cookbook.ownerId == userId
    var memberToRemove by remember { mutableStateOf<User?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }

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
                    // Member count pill removed — the Members section below shows the full list.
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECIPES",
                        style = MaterialTheme.typography.labelMedium,
                        color = Sage,
                        letterSpacing = 0.08.sp
                    )
                    TextButton(
                        onClick = {
                            navController.navigate(
                                Route.AddRecipesToCookbook(cookbookId = cookbook.id)
                            )
                        }
                    ) {
                        Text(
                            text = "+ Add",
                            style = MaterialTheme.typography.labelLarge,
                            color = Sage
                        )
                    }
                }
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

        // Members section header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp)
            ) {
                Text(
                    text = "MEMBERS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Sage,
                    letterSpacing = 0.08.sp
                )
                if (memberActionError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = memberActionError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(members, key = { it.id }) { member ->
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                MemberRow(
                    member = member,
                    isOwner = member.id == cookbook.ownerId,
                    isSelf = member.id == userId,
                    canRemove = isOwner && member.id != cookbook.ownerId,
                    enabled = !isProcessingMember,
                    onRemoveClick = { memberToRemove = member }
                )
            }
        }

        // Leave cookbook button for non-owner viewers
        if (!isOwner && cookbook.memberIds.contains(userId)) {
            item {
                OutlinedButton(
                    onClick = { showLeaveDialog = true },
                    enabled = !isProcessingMember,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Leave cookbook",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }

    val toRemove = memberToRemove
    if (toRemove != null) {
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove member?") },
            text = {
                Text(
                    text = "${toRemove.displayName.ifBlank { "This member" }} will lose access to this cookbook. " +
                        "You can re-invite them later."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = toRemove.id
                        memberToRemove = null
                        onRemoveMember(id)
                    }
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave cookbook?") },
            text = { Text("You'll need a new invite code to rejoin.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLeaveDialog = false
                        onLeaveCookbook()
                    }
                ) {
                    Text(
                        text = "Leave",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberRow(
    member: User,
    isOwner: Boolean,
    isSelf: Boolean,
    canRemove: Boolean,
    enabled: Boolean,
    onRemoveClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(member = member)

            Spacer(modifier = Modifier.padding(horizontal = 6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            member.displayName.isNotBlank() -> member.displayName
                            member.email.isNotBlank() -> member.email
                            else -> "Unknown member"
                        } + if (isSelf) " (you)" else "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isOwner) {
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        MetaPill(
                            text = "owner",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            borderColor = SageLight
                        )
                    }
                }
                if (member.email.isNotBlank() && member.email != member.displayName) {
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canRemove) {
                TextButton(
                    onClick = onRemoveClick,
                    enabled = enabled
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberAvatar(member: User) {
    val initial = (member.displayName.firstOrNull() ?: member.email.firstOrNull() ?: '?')
        .uppercaseChar()
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!member.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = member.avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(36.dp)
                    .padding(0.dp)
            )
        } else {
            Text(
                text = initial.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
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

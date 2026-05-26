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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.data.model.Comment
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.ui.components.InteractiveStarRating
import org.maxwelltech.recipetree.ui.components.StarRating
import org.maxwelltech.recipetree.ui.components.UserAvatar
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.RecipeDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: String,
    userId: String,
    navController: NavController,
    viewModel: RecipeDetailViewModel = remember {
        RecipeDetailViewModel(
            recipeRepository = AppContainer.recipeRepository,
            ratingRepository = AppContainer.ratingRepository,
            commentRepository = AppContainer.commentRepository,
            userProfileRepository = AppContainer.userProfileRepository
        )
    }
) {
    val recipe by viewModel.recipe.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val myRating by viewModel.myRating.collectAsState()
    val isSubmittingRating by viewModel.isSubmittingRating.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val commentAuthors by viewModel.commentAuthors.collectAsState()
    val commentInput by viewModel.commentInput.collectAsState()
    val isSubmittingComment by viewModel.isSubmittingComment.collectAsState()
    val commentError by viewModel.commentError.collectAsState()

    // Lives outside the VM because it's pure UI state (dialog open/closed).
    // The selected Comment carries through the dialog so we know what to delete.
    var pendingDelete by remember { mutableStateOf<Comment?>(null) }

    LaunchedEffect(recipeId, userId) {
        viewModel.loadRecipe(recipeId, userId)
        viewModel.observeComments(recipeId)
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
                    myRating = myRating,
                    isSubmittingRating = isSubmittingRating,
                    onRate = { stars ->
                        viewModel.submitRating(recipe!!.id, userId, stars)
                    },
                    onDelete = {
                        viewModel.deleteRecipe(recipe!!.id) {
                            navController.popBackStack()
                        }
                    },
                    comments = comments,
                    commentAuthors = commentAuthors,
                    commentInput = commentInput,
                    isSubmittingComment = isSubmittingComment,
                    commentError = commentError,
                    onCommentInputChange = viewModel::updateCommentInput,
                    onPostComment = { viewModel.submitComment(recipe!!.id, userId) },
                    onRequestDeleteComment = { comment -> pendingDelete = comment },
                    onDismissCommentError = viewModel::clearCommentError
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

        // Delete-confirm overlay. Sits inside the Box so it renders on top
        // of any state above; cleared on dismiss or confirm.
        pendingDelete?.let { target ->
            DeleteCommentDialog(
                onConfirm = {
                    recipe?.let { r ->
                        viewModel.deleteComment(r.id, target.id)
                    }
                    pendingDelete = null
                },
                onDismiss = { pendingDelete = null }
            )
        }
    }
}

@Composable
private fun DeleteCommentDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete this comment?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "This can't be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}

@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    userId: String,
    navController: NavController,
    myRating: Int,
    isSubmittingRating: Boolean,
    onRate: (Int) -> Unit,
    onDelete: () -> Unit,
    comments: List<Comment>,
    commentAuthors: Map<String, User>,
    commentInput: String,
    isSubmittingComment: Boolean,
    commentError: String?,
    onCommentInputChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onRequestDeleteComment: (Comment) -> Unit,
    onDismissCommentError: () -> Unit
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

                // Ratings section
                RatingSection(
                    averageRating = recipe.averageRating,
                    ratingCount = recipe.ratingCount,
                    myRating = myRating,
                    isSubmittingRating = isSubmittingRating,
                    onRate = onRate
                )

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
                                text = composeIngredient(ingredient),
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

        // Comments section — header, input, and the list. Spread across separate
        // lazy items so individual comment rows can be lazily composed/disposed
        // alongside the steps above them.
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = if (comments.isEmpty()) "Comments" else "Comments (${comments.size})")
                Spacer(modifier = Modifier.height(12.dp))
                CommentInput(
                    value = commentInput,
                    onValueChange = onCommentInputChange,
                    isSubmitting = isSubmittingComment,
                    onPost = onPostComment
                )
                if (commentError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = commentError,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissCommentError) {
                            Text(
                                text = "Dismiss",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (comments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet. Be the first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentItem(
                    comment = comment,
                    author = commentAuthors[comment.authorId],
                    canDelete = comment.authorId == userId || recipe.ownerId == userId,
                    onDelete = { onRequestDeleteComment(comment) }
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

@Composable
private fun RatingSection(
    averageRating: Float,
    ratingCount: Int,
    myRating: Int,
    isSubmittingRating: Boolean,
    onRate: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Rating")
        Spacer(modifier = Modifier.height(8.dp))

        // "Your rating" row — tap-to-set. Disabled while a submit is in flight
        // so a double-tap doesn't fire two transactions; the optimistic update
        // in the VM means the new star count is already visible.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your rating",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(96.dp)
            )
            InteractiveStarRating(
                rating = myRating,
                onRatingChanged = onRate,
                enabled = !isSubmittingRating
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Average row — full stars rounded down + the exact decimal as text.
        // Honest about the underlying float without inventing a half-star glyph.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Average",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(96.dp)
            )
            if (ratingCount == 0) {
                Text(
                    text = "No ratings yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                StarRating(rating = averageRating)
                Text(
                    text = formatAverageRating(averageRating, ratingCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAverageRating(averageRating: Float, ratingCount: Int): String {
    // Manual one-decimal format — kotlinx-datetime / printf-style aren't great
    // on KMP and "%.1f".format() isn't available in commonMain. round-half-up.
    val tenths = ((averageRating * 10f) + 0.5f).toInt()
    val whole = tenths / 10
    val decimal = tenths % 10
    val label = if (ratingCount == 1) "1 rating" else "$ratingCount ratings"
    return "$whole.$decimal  ($label)"
}

/**
 * Render an ingredient as a single display string by composing the three
 * structured fields. Manual entries (everything in [name], amount/unit empty)
 * render the same as before this composer existed; imported entries with all
 * three fields populated render as "1 cup flour".
 */
private fun composeIngredient(i: org.maxwelltech.recipetree.data.model.Ingredient): String =
    buildString {
        if (i.amount.isNotBlank()) { append(i.amount); append(' ') }
        if (i.unit.isNotBlank())   { append(i.unit);   append(' ') }
        append(i.name)
    }.trim()

@Composable
private fun CommentInput(
    value: String,
    onValueChange: (String) -> Unit,
    isSubmitting: Boolean,
    onPost: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = "Add a comment…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            enabled = !isSubmitting,
            minLines = 2,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Sage,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(
                onClick = onPost,
                enabled = value.isNotBlank() && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Sage,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Post",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    author: User?,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        UserAvatar(
            displayName = author?.displayName.orEmpty(),
            email = author?.email.orEmpty(),
            avatarUrl = author?.avatarUrl,
            size = 32.dp,
            textStyle = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = author?.displayName?.ifBlank { "Unknown" } ?: "Unknown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = relativeTime(comment.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 20.sp
            )
        }
        if (canDelete) {
            TextButton(onClick = onDelete) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun relativeTime(createdAt: Long?): String {
    if (createdAt == null) return ""
    val now = Clock.System.now().toEpochMilliseconds()
    val diffMs = now - createdAt
    if (diffMs < 0) return "just now"
    val sec = diffMs / 1000
    val min = sec / 60
    val hour = min / 60
    val day = hour / 24
    return when {
        sec < 60 -> "just now"
        min < 60 -> "${min}m ago"
        hour < 24 -> "${hour}h ago"
        day < 7 -> "${day}d ago"
        day < 30 -> "${day / 7}w ago"
        day < 365 -> "${day / 30}mo ago"
        else -> "${day / 365}y ago"
    }
}
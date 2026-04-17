package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.maxwelltech.recipetree.data.model.Recipe

@Composable
fun RecipeCard(
    recipe: Recipe,
    cookbookNames: List<String>,
    authorName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
        Row(modifier = Modifier.heightIn(min = 90.dp)) {

            // Photo
            RecipeCardPhoto(
                photoUrl = recipe.photoUrls.firstOrNull()
            )

            // Content
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // Cookbook row
                CookbookLabel(cookbookNames = cookbookNames)

                // Title
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 15.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Author
                Text(
                    text = "by $authorName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Stars row
                RatingRow(
                    averageRating = recipe.averageRating,
                    ratingCount = recipe.ratingCount
                )

                // Tags row
                TagsRow(
                    tags = recipe.tags,
                    isPrivate = recipe.isPrivate
                )
            }
        }
    }
}

@Composable
private fun RecipeCardPhoto(photoUrl: String?) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Recipe photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight()
            )
        } else {
            // Placeholder leaf icon using text — replace with Icon when you add Icons dependency
            Text(
                text = "🌿",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun CookbookLabel(cookbookNames: List<String>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cookbookNames.isNotEmpty()) {
            Text(
                text = cookbookNames.first(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (cookbookNames.size > 1) {
                Text(
                    text = "+${cookbookNames.size - 1} more",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RatingRow(averageRating: Float, ratingCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StarRating(rating = averageRating)
        if (ratingCount > 0) {
            Text(
                text = "($ratingCount)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TagsRow(tags: List<String>, isPrivate: Boolean) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items(tags.take(3)) { tag ->
            RecipeTag(label = tag)
        }
        if (isPrivate) {
            item { PrivateBadge() }
        }
    }
}

@Composable
private fun RecipeTag(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PrivateBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = "private",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
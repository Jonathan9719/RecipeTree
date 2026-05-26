package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.CookbookVisibility
import org.maxwelltech.recipetree.ui.util.cookbookEmojiFor

@Composable
fun CookbookCard(
    cookbook: Cookbook,
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
        // Fixed row height + matching photo tile height — same pattern as
        // RecipeCard. fillMaxHeight inside an intrinsic-driven Row was
        // leaving the photo tile shorter than the actual row.
        Row(modifier = Modifier.height(COOKBOOK_CARD_HEIGHT)) {

            CookbookCardPhoto(
                photoUrl = cookbook.coverPhotoUrl,
                cookbookId = cookbook.id
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = cookbook.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!cookbook.description.isNullOrBlank()) {
                    Text(
                        text = cookbook.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VisibilityPill(visibility = cookbook.visibility)
                    MemberCountLabel(memberCount = cookbook.memberIds.size)
                }
            }
        }
    }
}

@Composable
private fun CookbookCardPhoto(photoUrl: String?, cookbookId: String) {
    Box(
        modifier = Modifier
            .size(width = 90.dp, height = COOKBOOK_CARD_HEIGHT)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Cookbook cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Deterministic emoji per cookbook — same id always picks the
            // same icon, and CookbookDetailScreen's hero uses the same
            // call so the card and detail always agree.
            Text(
                text = cookbookEmojiFor(cookbookId),
                fontSize = 32.sp
            )
        }
    }
}

private val COOKBOOK_CARD_HEIGHT = 100.dp

@Composable
private fun VisibilityPill(visibility: CookbookVisibility) {
    val label = when (visibility) {
        CookbookVisibility.PRIVATE -> "private"
        CookbookVisibility.UNLISTED -> "unlisted"
        CookbookVisibility.PUBLIC -> "public"
    }
    val (bg, fg) = when (visibility) {
        CookbookVisibility.PUBLIC -> MaterialTheme.colorScheme.primaryContainer to
            MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg
        )
    }
}

@Composable
private fun MemberCountLabel(memberCount: Int) {
    if (memberCount <= 1) return
    Text(
        text = "$memberCount members",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

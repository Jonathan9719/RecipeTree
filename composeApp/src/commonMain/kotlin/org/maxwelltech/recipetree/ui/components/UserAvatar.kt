package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Circular avatar for a user. Shows [avatarUrl] via AsyncImage when available,
 * otherwise a first-initial badge on [MaterialTheme.colorScheme.primaryContainer].
 *
 * Used in the cookbook members list, the top-bar profile button, the profile
 * screen itself — anywhere we render a person.
 */
@Composable
fun UserAvatar(
    displayName: String,
    email: String = "",
    avatarUrl: String? = null,
    size: Dp = 40.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier
) {
    val initial = (displayName.firstOrNull() ?: email.firstOrNull() ?: '?')
        .uppercaseChar()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = initial.toString(),
                style = textStyle,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

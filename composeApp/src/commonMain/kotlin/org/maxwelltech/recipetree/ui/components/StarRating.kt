package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import recipetree.composeapp.generated.resources.Res
import recipetree.composeapp.generated.resources.ic_star_filled
import recipetree.composeapp.generated.resources.ic_star_empty

@Composable
fun StarRating(
    rating: Float,
    maxStars: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        repeat(maxStars) { index ->
            Icon(
                painter = if (index < rating.toInt())
                    painterResource(Res.drawable.ic_star_filled)
                else
                    painterResource(Res.drawable.ic_star_empty),
                contentDescription = null,
                tint = if (index < rating.toInt())
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}
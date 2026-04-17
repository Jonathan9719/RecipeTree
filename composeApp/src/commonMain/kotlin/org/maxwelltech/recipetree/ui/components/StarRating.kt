package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maxwelltech.recipetree.ui.theme.HoneyDark

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
            Text(
                text = if (index < rating.toInt()) "★" else "☆",
                fontSize = 12.sp,
                color = if (index < rating.toInt())
                    HoneyDark
                else
                    MaterialTheme.colorScheme.outline
            )
        }
    }
}
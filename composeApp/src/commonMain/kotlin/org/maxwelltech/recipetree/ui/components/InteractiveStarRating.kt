package org.maxwelltech.recipetree.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.maxwelltech.recipetree.ui.theme.HoneyDark

/**
 * Tap-to-rate 5-star row. Sibling to [StarRating] which is display-only at 12sp;
 * this one is sized for thumb taps and reports an Int 1..5 back through [onRatingChanged].
 *
 * A rating of 0 means "not yet rated" and renders all empty stars — there's no
 * way to tap-set 0 from this row (tapping the first star sets 1). Clearing a
 * rating isn't supported by the design doc (one-rating-per-user, edit anytime).
 */
@Composable
fun InteractiveStarRating(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            val starValue = index + 1
            val isFilled = starValue <= rating
            Text(
                text = if (isFilled) "★" else "☆",
                fontSize = 28.sp,
                color = if (isFilled) HoneyDark else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clickable(enabled = enabled) { onRatingChanged(starValue) }
                    .padding(2.dp)
            )
        }
    }
}

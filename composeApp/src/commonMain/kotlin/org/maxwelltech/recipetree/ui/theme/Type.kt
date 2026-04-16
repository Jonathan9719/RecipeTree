package org.maxwelltech.recipetree.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import recipetree.composeapp.generated.resources.Res
import recipetree.composeapp.generated.resources.playfair_display_regular
import recipetree.composeapp.generated.resources.playfair_display_medium
import recipetree.composeapp.generated.resources.lato_regular
import recipetree.composeapp.generated.resources.lato_bold

@Composable
fun playfairFamily() = FontFamily(
    Font(Res.font.playfair_display_regular, FontWeight.Normal),
    Font(Res.font.playfair_display_medium, FontWeight.Medium),
)

@Composable
fun latoFamily() = FontFamily(
    Font(Res.font.lato_regular, FontWeight.Normal),
    Font(Res.font.lato_bold, FontWeight.Bold),
)

@Composable
fun RecipeTreeTypography(): Typography {
    val playfair = playfairFamily()
    val lato = latoFamily()
    return Typography(
        // Playfair for display/headings
        displayLarge = TextStyle(
            fontFamily = playfair,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        displayMedium = TextStyle(
            fontFamily = playfair,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 36.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = playfair,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 32.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = playfair,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 28.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = playfair,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        // Lato for body and UI
        titleLarge = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 26.sp
        ),
        titleMedium = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = lato,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    )
}
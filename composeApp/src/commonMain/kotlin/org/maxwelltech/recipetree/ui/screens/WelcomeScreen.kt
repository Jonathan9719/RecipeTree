package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.ui.theme.SageLight
import org.maxwelltech.recipetree.viewmodel.AuthViewModel
import recipetree.composeapp.generated.resources.Res
import recipetree.composeapp.generated.resources.recipe_tree_logo

/**
 * First-launch carousel. Shown once per user (gated by User.seenWelcome). On
 * Skip or Get started, the VM flips the flag and the user lands on
 * RecipeList. Skip is identical to Get started — it just lets the user out
 * sooner without making them swipe through every card.
 */
@Composable
fun WelcomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val pages = remember { welcomePages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val finish = {
        authViewModel.markWelcomeSeen()
        // popUpTo(0) clears the entire backstack so Back from RecipeList
        // can't land on Welcome again. Welcome is one-shot per user.
        navController.navigate(Route.RecipeList) {
            popUpTo(0) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Skip — always available, top right.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = finish) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                WelcomePageContent(page = pages[pageIndex])
            }

            // Dot indicator above the action button.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(width = if (selected) 24.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(if (selected) Sage else SageLight)
                    )
                }
            }

            // Primary action: Next on pages 0..n-2, Get started on the last.
            val isLast = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (isLast) {
                        finish()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Sage,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = if (isLast) "Get started" else "Next",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun WelcomePageContent(page: WelcomePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo on every page so the brand stays present even as the topic shifts.
        Image(
            painter = painterResource(Res.drawable.recipe_tree_logo),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Big topic emoji as the focal point of the page.
        Text(text = page.emoji, fontSize = 64.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.heading,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private data class WelcomePage(
    val emoji: String,
    val heading: String,
    val body: String
)

private fun welcomePages(): List<WelcomePage> = listOf(
    WelcomePage(
        emoji = "📖",
        heading = "Your recipes, your way",
        body = "Add recipes by hand, or paste a link from any recipe site and " +
            "we'll fill in the title, ingredients, and steps for you."
    ),
    WelcomePage(
        emoji = "👨‍👩‍👧",
        heading = "Cookbooks for sharing",
        body = "Make a cookbook for your family and share the invite code. " +
            "Everyone you invite can see every recipe inside — and add their own."
    ),
    WelcomePage(
        emoji = "✨",
        heading = "Better together",
        body = "Rate dishes, leave comments, and grow your family's recipe " +
            "collection together over time."
    )
)

package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.ui.components.CookbookCard
import org.maxwelltech.recipetree.viewmodel.AuthViewModel
import org.maxwelltech.recipetree.viewmodel.CookbookListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookListScreen(
    userId: String,
    navController: NavController,
    authViewModel: AuthViewModel,
    viewModel: CookbookListViewModel = remember {
        CookbookListViewModel(AppContainer.cookbookRepository)
    }
) {
    val cookbooks by viewModel.cookbooks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(userId) {
        viewModel.observeUserCookbooks(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Cookbooks",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    TextButton(onClick = {
                        navController.navigate(Route.RecipeList) {
                            popUpTo(Route.RecipeList) { inclusive = true }
                        }
                    }) {
                        Text(
                            text = "Recipes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { authViewModel.signOut() }) {
                        Text(
                            text = "Sign out",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            cookbooks.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No cookbooks yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(cookbooks) { cookbook ->
                        CookbookCard(
                            cookbook = cookbook,
                            onClick = {
                                navController.navigate(
                                    Route.CookbookDetail(cookbookId = cookbook.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

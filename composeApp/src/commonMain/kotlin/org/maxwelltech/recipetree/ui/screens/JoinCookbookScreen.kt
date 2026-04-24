package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.Route
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.viewmodel.JoinCookbookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinCookbookScreen(
    userId: String,
    navController: NavController,
    viewModel: JoinCookbookViewModel = remember {
        JoinCookbookViewModel(inviteRepository = AppContainer.inviteRepository)
    }
) {
    val code by viewModel.code.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val joinedCookbookId by viewModel.joinedCookbookId.collectAsState()

    LaunchedEffect(joinedCookbookId) {
        val id = joinedCookbookId
        if (id != null) {
            // Replace the join screen on the back stack — pressing Back from the
            // new detail screen should land on the cookbook list, not this form.
            navController.navigate(Route.CookbookDetail(cookbookId = id)) {
                popUpTo(Route.JoinCookbook) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Join Cookbook",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        TextButton(onClick = { navController.popBackStack() }) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Sage,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.submit(userId) },
                            enabled = !isSubmitting && code.isNotBlank(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Join",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Enter the invite code someone shared with you.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            OutlinedTextField(
                value = code,
                onValueChange = { viewModel.updateCode(it) },
                label = { Text("Invite code") },
                placeholder = { Text("ABCD-EFGH") },
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.submit(userId) }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Sage,
                    focusedLabelColor = Sage,
                    cursorColor = Sage
                )
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Codes look like ABCD-EFGH — dashes and spaces are optional.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

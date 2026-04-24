package org.maxwelltech.recipetree.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.ui.components.SettingRow
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = remember {
        SettingsViewModel(authRepository = AppContainer.authRepository)
    }
) {
    val isChangingPassword by viewModel.isChangingPassword.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val passwordChangeSuccess by viewModel.passwordChangeSuccess.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // When the VM flags a successful password change, close the dialog and
    // show the confirmation snackbar. LaunchedEffect keyed on the flag so
    // rotation / recomposition doesn't re-fire it.
    LaunchedEffect(passwordChangeSuccess) {
        if (passwordChangeSuccess) {
            showPasswordDialog = false
            snackbarHostState.showSnackbar(
                message = "Password updated",
                duration = SnackbarDuration.Short
            )
            viewModel.clearPasswordChangeSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(
                            text = "← Back",
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingRow(
                label = "Change password",
                onClick = { showPasswordDialog = true }
            )

            // More settings rows land here (theme, notifications, delete
            // account — the last of those is 3c).
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isSubmitting = isChangingPassword,
            error = passwordError,
            onDismiss = {
                showPasswordDialog = false
                viewModel.clearPasswordError()
            },
            onSubmit = { current, new ->
                viewModel.changePassword(currentPassword = current, newPassword = new)
            }
        )
    }
}

@Composable
private fun ChangePasswordDialog(
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    // Per-field show/hide so the user can reveal one without the other —
    // matches the usual iOS/Android pattern and sidesteps any "I peeked at
    // my current password but not my new one" awkwardness.
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current password") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = showCurrentPassword,
                            onToggle = { showCurrentPassword = !showCurrentPassword }
                        )
                    },
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Sage,
                        focusedLabelColor = Sage,
                        cursorColor = Sage
                    )
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New password") },
                    singleLine = true,
                    visualTransformation = if (showNewPassword) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = showNewPassword,
                            onToggle = { showNewPassword = !showNewPassword }
                        )
                    },
                    isError = error != null,
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
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "At least 6 characters. You'll stay signed in.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(0.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(currentPassword, newPassword) },
                enabled = !isSubmitting
                    && currentPassword.isNotBlank()
                    && newPassword.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Sage,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save", color = Sage)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit
) {
    TextButton(
        onClick = onToggle,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = if (visible) "Hide" else "Show",
            style = MaterialTheme.typography.labelMedium,
            color = Sage
        )
    }
}

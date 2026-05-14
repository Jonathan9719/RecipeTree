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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.maxwelltech.recipetree.AppContainer
import org.maxwelltech.recipetree.ui.components.SettingRow
import org.maxwelltech.recipetree.ui.theme.Sage
import org.maxwelltech.recipetree.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    navController: NavController,
    onAccountDeleted: () -> Unit,
    viewModel: SettingsViewModel = remember {
        SettingsViewModel(
            authRepository = AppContainer.authRepository,
            cookbookRepository = AppContainer.cookbookRepository,
            recipeRepository = AppContainer.recipeRepository,
            inviteRepository = AppContainer.inviteRepository
        )
    }
) {
    val isChangingPassword by viewModel.isChangingPassword.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val passwordChangeSuccess by viewModel.passwordChangeSuccess.collectAsState()

    val ownedCookbookCount by viewModel.ownedCookbookCount.collectAsState()
    val ownedRecipeCount by viewModel.ownedRecipeCount.collectAsState()
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()
    val deleteAccountError by viewModel.deleteAccountError.collectAsState()
    val deleteAccountSuccess by viewModel.deleteAccountSuccess.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadAccountSummary(userId)
    }

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

    LaunchedEffect(deleteAccountSuccess) {
        if (deleteAccountSuccess) {
            showDeleteAccountDialog = false
            // Hand off to the navigation owner — they decide how to clear the
            // backstack and land on Login. This VM doesn't reach into the nav
            // controller directly.
            onAccountDeleted()
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

            // Danger zone — the only destructive setting today. Tinted red so
            // it's visually distinct from the routine actions above.
            DangerRow(
                label = "Delete account",
                enabled = !isDeletingAccount,
                onClick = {
                    viewModel.clearDeleteAccountError()
                    showDeleteAccountDialog = true
                }
            )
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

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            ownedCookbookCount = ownedCookbookCount,
            ownedRecipeCount = ownedRecipeCount,
            isSubmitting = isDeletingAccount,
            error = deleteAccountError,
            onDismiss = {
                showDeleteAccountDialog = false
                viewModel.clearDeleteAccountError()
            },
            onConfirm = { currentPassword ->
                viewModel.deleteAccount(userId = userId, currentPassword = currentPassword)
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

/**
 * Same shape as [SettingRow] but tinted in error color and with no chevron —
 * for destructive actions like delete-account. Kept local because it's the
 * only destructive row today; if more land we can promote it to ui/components.
 */
@Composable
private fun DangerRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.error
        )
    ) {
        TextButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    ownedCookbookCount: Int,
    ownedRecipeCount: Int,
    isSubmitting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = buildWarning(
                        ownedCookbookCount = ownedCookbookCount,
                        ownedRecipeCount = ownedRecipeCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "This can't be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Confirm with current password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = showPassword,
                            onToggle = { showPassword = !showPassword }
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
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(currentPassword) },
                enabled = !isSubmitting && currentPassword.isNotBlank()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Delete account",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp)
                    )
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

private fun buildWarning(ownedCookbookCount: Int, ownedRecipeCount: Int): String {
    val cookbookPart = when (ownedCookbookCount) {
        0 -> null
        1 -> "1 cookbook you own (members will lose access)"
        else -> "$ownedCookbookCount cookbooks you own (members will lose access)"
    }
    val recipePart = when (ownedRecipeCount) {
        0 -> null
        1 -> "1 recipe"
        else -> "$ownedRecipeCount recipes"
    }
    val parts = listOfNotNull(cookbookPart, recipePart)
    return if (parts.isEmpty()) {
        "Your profile and sign-in will be permanently removed."
    } else {
        "We'll permanently delete your profile, sign-in, and ${parts.joinToString(" and ")}."
    }
}

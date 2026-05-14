package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.repository.AuthRepository
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.InviteRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val cookbookRepository: CookbookRepository,
    private val recipeRepository: RecipeRepository,
    private val inviteRepository: InviteRepository
) : ViewModel() {

    // ---- Change password -------------------------------------------------

    private val _isChangingPassword = MutableStateFlow(false)
    val isChangingPassword: StateFlow<Boolean> = _isChangingPassword.asStateFlow()

    /** Populated when a password change attempt fails so the dialog can render it inline. */
    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    /** Emitted once on a successful password change so the screen can close the dialog + show a confirmation. */
    private val _passwordChangeSuccess = MutableStateFlow(false)
    val passwordChangeSuccess: StateFlow<Boolean> = _passwordChangeSuccess.asStateFlow()

    // ---- Delete account --------------------------------------------------

    private val _ownedCookbookCount = MutableStateFlow(0)
    val ownedCookbookCount: StateFlow<Int> = _ownedCookbookCount.asStateFlow()

    private val _ownedRecipeCount = MutableStateFlow(0)
    val ownedRecipeCount: StateFlow<Int> = _ownedRecipeCount.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError: StateFlow<String?> = _deleteAccountError.asStateFlow()

    /** Flips to true after Firebase Auth user is gone; the screen routes to Login. */
    private val _deleteAccountSuccess = MutableStateFlow(false)
    val deleteAccountSuccess: StateFlow<Boolean> = _deleteAccountSuccess.asStateFlow()

    // ---- Change password actions -----------------------------------------

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank()) {
            _passwordError.value = "Enter your current password."
            return
        }
        if (newPassword.length < 6) {
            _passwordError.value = "New password must be at least 6 characters."
            return
        }
        if (newPassword == currentPassword) {
            _passwordError.value = "New password must be different from the current one."
            return
        }
        viewModelScope.launch {
            _isChangingPassword.value = true
            _passwordError.value = null
            try {
                authRepository.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
                _passwordChangeSuccess.value = true
            } catch (e: Exception) {
                _passwordError.value = friendlyError(e.message)
            } finally {
                _isChangingPassword.value = false
            }
        }
    }

    fun clearPasswordError() {
        _passwordError.value = null
    }

    fun clearPasswordChangeSuccess() {
        _passwordChangeSuccess.value = false
    }

    // ---- Delete account actions ------------------------------------------

    /**
     * Refresh the counts shown in the delete-account warning. Called when
     * Settings opens so the dialog has live numbers when the user taps the
     * delete row. Cheap — one .first() snapshot per flow.
     */
    fun loadAccountSummary(userId: String) {
        viewModelScope.launch {
            try {
                val cookbooks = cookbookRepository.observeUserCookbooks(userId).first()
                _ownedCookbookCount.value = cookbooks.count { it.ownerId == userId }
                val recipes = recipeRepository.observeUserRecipes(userId).first()
                _ownedRecipeCount.value = recipes.size
            } catch (_: Exception) {
                // Counts are advisory; if we can't fetch them the dialog
                // shows zeros and the cascade still runs correctly.
                _ownedCookbookCount.value = 0
                _ownedRecipeCount.value = 0
            }
        }
    }

    /**
     * Cascade-delete everything tied to [userId] then the Firebase Auth user.
     * Order matters: Auth user goes LAST so a network failure midway leaves
     * the user still signed in and able to retry.
     */
    fun deleteAccount(userId: String, currentPassword: String) {
        if (currentPassword.isBlank()) {
            _deleteAccountError.value = "Enter your current password to confirm."
            return
        }
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteAccountError.value = null
            try {
                // 1. Prove identity — Firebase requires this for the Auth
                //    delete() call below. Doing it up front means a wrong
                //    password fails fast without touching any data.
                authRepository.reauthenticate(currentPassword)

                // 2. Owned cookbooks first (other members lose access here).
                val cookbooks = cookbookRepository.observeUserCookbooks(userId).first()
                val owned = cookbooks.filter { it.ownerId == userId }
                val memberOnly = cookbooks.filter { it.ownerId != userId }
                owned.forEach { cookbookRepository.deleteCookbook(it.id) }

                // 3. Owned recipes.
                val recipes = recipeRepository.observeUserRecipes(userId).first()
                recipes.forEach { recipeRepository.deleteRecipe(it.id) }

                // 4. Self-leave the cookbooks where we're a member but not
                //    the owner — owners stay intact.
                memberOnly.forEach { cookbookRepository.removeMember(it.id, userId) }

                // 5. Invites we created — revoke so leaked codes can't keep
                //    adding members to cookbooks we no longer touch.
                val invites = inviteRepository.observeUserCreatedInvites(userId).first()
                invites.forEach { inviteRepository.revokeInvite(it.code) }

                // 6. Point of no return: users/{uid} doc + Firebase Auth.
                authRepository.deleteCurrentUser()

                _deleteAccountSuccess.value = true
            } catch (e: Exception) {
                _deleteAccountError.value = friendlyError(e.message)
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }

    /**
     * Firebase Auth surfaces low-level SDK messages ("ERROR_WRONG_PASSWORD",
     * "The password is invalid...") that aren't user-facing. Map the common
     * cases to something readable; fall through to the raw message for
     * anything unexpected so we don't swallow useful signal.
     */
    private fun friendlyError(raw: String?): String {
        if (raw == null) return "Something went wrong. Try again."
        val lower = raw.lowercase()
        return when {
            "wrong" in lower && "password" in lower -> "Current password is incorrect."
            "invalid" in lower && "credential" in lower -> "Current password is incorrect."
            "weak" in lower && "password" in lower -> "New password is too weak."
            "network" in lower -> "Network issue — check your connection."
            "too-many-requests" in lower || "too many" in lower ->
                "Too many attempts. Wait a bit and try again."
            else -> raw
        }
    }
}

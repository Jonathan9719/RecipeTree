package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.repository.AuthRepository

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isChangingPassword = MutableStateFlow(false)
    val isChangingPassword: StateFlow<Boolean> = _isChangingPassword.asStateFlow()

    /** Populated when a password change attempt fails so the dialog can render it inline. */
    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

    /** Emitted once on a successful password change so the screen can close the dialog + show a confirmation. */
    private val _passwordChangeSuccess = MutableStateFlow(false)
    val passwordChangeSuccess: StateFlow<Boolean> = _passwordChangeSuccess.asStateFlow()

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

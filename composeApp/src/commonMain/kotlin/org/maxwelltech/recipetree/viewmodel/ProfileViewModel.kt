package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.AuthRepository
import org.maxwelltech.recipetree.ui.util.friendlyMessage

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Mirror the repo's currentUser StateFlow. The repo itself patches
        // profile edits into that flow, so we don't need to re-apply them
        // locally — both this VM and any other reader (e.g. the top-bar
        // avatar on the list screens) see the update at the same time.
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _user.value = user
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _error.value = "Name can't be empty."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                authRepository.updateDisplayName(trimmed)
            } catch (e: Exception) {
                _error.value = friendlyMessage(e, "Couldn't update your name.")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

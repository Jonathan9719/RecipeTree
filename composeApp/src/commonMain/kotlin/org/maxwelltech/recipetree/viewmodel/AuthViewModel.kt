package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.AuthRepository

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    init {
        // Observe Firebase auth state — reacts automatically to login/logout
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _currentUser.value = user
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                authRepository.signIn(email, password)
                _authSuccess.value = true
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                authRepository.signUp(email, password, displayName)
                _authSuccess.value = true
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _error.value = null
            try {
                authRepository.signOut()
                _authSuccess.value = false
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                authRepository.sendPasswordResetEmail(email)
                onSuccess()
            } catch (e: Exception) {
                _error.value = friendlyError(e.message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Converts Firebase error messages into family-friendly language
    private fun friendlyError(message: String?): String {
        return when {
            message == null -> "Something went wrong. Please try again."
            message.contains("password") -> "Incorrect password. Please try again."
            message.contains("email") -> "Invalid email address."
            message.contains("no user") -> "No account found with that email."
            message.contains("already in use") -> "An account already exists with that email."
            message.contains("weak-password") -> "Password must be at least 6 characters."
            message.contains("network") -> "Network error. Check your connection."
            else -> "Something went wrong. Please try again."
        }
    }
}
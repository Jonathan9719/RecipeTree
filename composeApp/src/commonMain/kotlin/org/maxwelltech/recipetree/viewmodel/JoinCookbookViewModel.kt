package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.firebase.FirebaseInviteRepository
import org.maxwelltech.recipetree.data.repository.InviteRepository

class JoinCookbookViewModel(
    private val inviteRepository: InviteRepository
) : ViewModel() {

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** cookbookId of the cookbook the user just joined — UI uses this to navigate. */
    private val _joinedCookbookId = MutableStateFlow<String?>(null)
    val joinedCookbookId: StateFlow<String?> = _joinedCookbookId.asStateFlow()

    fun updateCode(input: String) {
        _code.value = input
        // Clear any prior error as soon as the user edits — otherwise a stale
        // "Invite not found" keeps shouting while they fix the typo.
        if (_error.value != null) _error.value = null
    }

    fun submit(userId: String) {
        val normalized = FirebaseInviteRepository.normalizeCode(_code.value)
        if (normalized.length < 8) {
            _error.value = "Code should be 8 characters (like ABCD-EFGH)."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            try {
                val cookbookId = inviteRepository.acceptInvite(
                    code = normalized,
                    userId = userId
                )
                _joinedCookbookId.value = cookbookId
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to join cookbook"
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}

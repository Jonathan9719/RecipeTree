package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.CookbookVisibility
import org.maxwelltech.recipetree.data.repository.CookbookRepository

class CookbookEditViewModel(
    private val cookbookRepository: CookbookRepository
) : ViewModel() {

    private val _cookbook = MutableStateFlow(Cookbook())
    val cookbook: StateFlow<Cookbook> = _cookbook.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    fun loadCookbook(cookbookId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                _cookbook.value = cookbookRepository.getCookbook(cookbookId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cookbook"
            }
        }
    }

    fun updateName(name: String) {
        _cookbook.value = _cookbook.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _cookbook.value = _cookbook.value.copy(description = description)
    }

    fun updateVisibility(visibility: CookbookVisibility) {
        _cookbook.value = _cookbook.value.copy(visibility = visibility)
    }

    fun saveCookbook(ownerId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val current = _cookbook.value
                val cookbookToSave = if (current.ownerId.isEmpty()) {
                    // New cookbook — owner becomes a member so the query picks it up
                    current.copy(
                        ownerId = ownerId,
                        memberIds = (current.memberIds + ownerId).distinct()
                    )
                } else {
                    current
                }
                cookbookRepository.saveCookbook(cookbookToSave)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save cookbook"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteCookbook(cookbookId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null
            try {
                cookbookRepository.deleteCookbook(cookbookId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete cookbook"
            } finally {
                _isDeleting.value = false
            }
        }
    }
}

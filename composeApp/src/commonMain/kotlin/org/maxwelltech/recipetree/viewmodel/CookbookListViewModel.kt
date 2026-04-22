package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.repository.CookbookRepository

class CookbookListViewModel(
    private val cookbookRepository: CookbookRepository
) : ViewModel() {

    private val _cookbooks = MutableStateFlow<List<Cookbook>>(emptyList())
    val cookbooks: StateFlow<List<Cookbook>> = _cookbooks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun observeUserCookbooks(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                cookbookRepository.observeUserCookbooks(userId).collect { cookbooks ->
                    _cookbooks.value = cookbooks
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cookbooks"
                _isLoading.value = false
            }
        }
    }
}

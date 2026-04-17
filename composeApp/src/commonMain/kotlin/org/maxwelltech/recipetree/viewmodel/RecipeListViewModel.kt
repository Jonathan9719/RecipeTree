package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun observeUserRecipes(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                recipeRepository.observeUserRecipes(userId).collect { recipes ->
                    _recipes.value = recipes
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipes"
                _isLoading.value = false
            }
        }
    }

    fun observeCookbookRecipes(cookbookId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                recipeRepository.observeCookbookRecipes(cookbookId).collect { recipes ->
                    _recipes.value = recipes
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to observe recipes"
                _isLoading.value = false
            }
        }
    }
}
package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                println("DEBUG: Loading recipe $recipeId")
                val result = recipeRepository.getRecipe(recipeId)
                println("DEBUG: Got recipe ${result.title}")
                _recipe.value = result
            } catch (e: Exception) {
                println("DEBUG: Error loading recipe ${e.message}")
                _error.value = e.message ?: "Failed to load recipe"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteRecipe(recipeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                recipeRepository.deleteRecipe(recipeId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete recipe"
            }
        }
    }
}
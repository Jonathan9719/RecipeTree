package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class CookbookDetailViewModel(
    private val cookbookRepository: CookbookRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _cookbook = MutableStateFlow<Cookbook?>(null)
    val cookbook: StateFlow<Cookbook?> = _cookbook.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadCookbook(cookbookId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _cookbook.value = cookbookRepository.getCookbook(cookbookId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cookbook"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun observeRecipes(cookbookId: String) {
        viewModelScope.launch {
            try {
                recipeRepository.observeCookbookRecipes(cookbookId).collect { recipes ->
                    _recipes.value = recipes
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipes"
            }
        }
    }
}

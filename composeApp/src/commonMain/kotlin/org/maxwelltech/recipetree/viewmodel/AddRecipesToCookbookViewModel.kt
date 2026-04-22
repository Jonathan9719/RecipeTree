package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class AddRecipesToCookbookViewModel(
    private val cookbookRepository: CookbookRepository,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _selectedRecipeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedRecipeIds: StateFlow<Set<String>> = _selectedRecipeIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // The recipes already in this cookbook when the screen opened — used to diff on save.
    private var initialRecipeIds: Set<String> = emptySet()

    fun load(userId: String, cookbookId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Take the first snapshot so we can seed the selection deterministically
                val snapshot = recipeRepository.observeUserRecipes(userId).first()
                _recipes.value = snapshot
                val already = snapshot
                    .filter { cookbookId in it.cookbookIds }
                    .map { it.id }
                    .toSet()
                initialRecipeIds = already
                _selectedRecipeIds.value = already
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleRecipe(recipeId: String) {
        val current = _selectedRecipeIds.value
        _selectedRecipeIds.value = if (recipeId in current) {
            current - recipeId
        } else {
            current + recipeId
        }
    }

    fun save(cookbookId: String, userId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val selected = _selectedRecipeIds.value
                val toAdd = selected - initialRecipeIds
                val toRemove = initialRecipeIds - selected
                toAdd.forEach { recipeId ->
                    cookbookRepository.addRecipeToCookbook(
                        recipeId = recipeId,
                        cookbookId = cookbookId,
                        addedById = userId
                    )
                }
                toRemove.forEach { recipeId ->
                    cookbookRepository.removeRecipeFromCookbook(
                        recipeId = recipeId,
                        cookbookId = cookbookId
                    )
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update cookbook"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

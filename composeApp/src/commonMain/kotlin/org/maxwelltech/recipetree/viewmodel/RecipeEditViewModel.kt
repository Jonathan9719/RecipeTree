package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Ingredient
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class RecipeEditViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow(Recipe())
    val recipe: StateFlow<Recipe> = _recipe.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // Load existing recipe for editing
    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                _recipe.value = recipeRepository.getRecipe(recipeId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipe"
            }
        }
    }

    // Field update functions — each updates only its field
    fun updateTitle(title: String) {
        _recipe.value = _recipe.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _recipe.value = _recipe.value.copy(description = description)
    }

    fun updateServings(servings: Int) {
        _recipe.value = _recipe.value.copy(servings = servings)
    }

    fun updateIngredients(ingredients: List<Ingredient>) {
        _recipe.value = _recipe.value.copy(ingredients = ingredients)
    }

    fun updateSteps(steps: List<String>) {
        _recipe.value = _recipe.value.copy(steps = steps)
    }

    fun updateTags(tags: List<String>) {
        _recipe.value = _recipe.value.copy(tags = tags)
    }

    fun updateIsPrivate(isPrivate: Boolean) {
        _recipe.value = _recipe.value.copy(isPrivate = isPrivate)
    }

    fun saveRecipe(ownerId: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                val recipeToSave = if (_recipe.value.ownerId.isEmpty()) {
                    _recipe.value.copy(ownerId = ownerId)
                } else {
                    _recipe.value
                }
                recipeRepository.saveRecipe(recipeToSave)
                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save recipe"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
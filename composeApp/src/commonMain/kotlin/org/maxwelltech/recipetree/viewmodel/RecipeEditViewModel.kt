package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.Ingredient
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class RecipeEditViewModel(
    private val recipeRepository: RecipeRepository,
    private val cookbookRepository: CookbookRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow(Recipe())
    val recipe: StateFlow<Recipe> = _recipe.asStateFlow()

    private val _availableCookbooks = MutableStateFlow<List<Cookbook>>(emptyList())
    val availableCookbooks: StateFlow<List<Cookbook>> = _availableCookbooks.asStateFlow()

    private val _selectedCookbookIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCookbookIds: StateFlow<Set<String>> = _selectedCookbookIds.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // Baseline for diffing cookbook membership on save.
    private var initialCookbookIds: Set<String> = emptySet()

    fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val loaded = recipeRepository.getRecipe(recipeId)
                _recipe.value = loaded
                val seeded = loaded.cookbookIds.toSet()
                initialCookbookIds = seeded
                _selectedCookbookIds.value = seeded
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipe"
            }
        }
    }

    fun loadAvailableCookbooks(userId: String) {
        viewModelScope.launch {
            try {
                _availableCookbooks.value = cookbookRepository
                    .observeUserCookbooks(userId)
                    .first()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cookbooks"
            }
        }
    }

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

    fun toggleCookbook(cookbookId: String) {
        val current = _selectedCookbookIds.value
        _selectedCookbookIds.value = if (cookbookId in current) {
            current - cookbookId
        } else {
            current + cookbookId
        }
    }

    fun deleteRecipe(recipeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null
            try {
                recipeRepository.deleteRecipe(recipeId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete recipe"
            } finally {
                _isDeleting.value = false
            }
        }
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
                val saved = recipeRepository.saveRecipe(recipeToSave)

                // Apply cookbook membership diff — recipe doc must exist first since
                // addRecipeToCookbook does an arrayUnion update on recipes/{id}.
                val selected = _selectedCookbookIds.value
                val toAdd = selected - initialCookbookIds
                val toRemove = initialCookbookIds - selected
                toAdd.forEach { cookbookId ->
                    cookbookRepository.addRecipeToCookbook(
                        recipeId = saved.id,
                        cookbookId = cookbookId,
                        addedById = ownerId
                    )
                }
                toRemove.forEach { cookbookId ->
                    cookbookRepository.removeRecipeFromCookbook(
                        recipeId = saved.id,
                        cookbookId = cookbookId
                    )
                }

                _saveSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to save recipe"
            } finally {
                _isSaving.value = false
            }
        }
    }
}

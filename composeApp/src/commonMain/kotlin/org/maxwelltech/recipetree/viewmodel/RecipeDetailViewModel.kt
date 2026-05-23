package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RatingRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val ratingRepository: RatingRepository
) : ViewModel() {

    private val _recipe = MutableStateFlow<Recipe?>(null)
    val recipe: StateFlow<Recipe?> = _recipe.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 0 = "not yet rated" sentinel. Persisted ratings are always 1..5.
    private val _myRating = MutableStateFlow(0)
    val myRating: StateFlow<Int> = _myRating.asStateFlow()

    private val _isSubmittingRating = MutableStateFlow(false)
    val isSubmittingRating: StateFlow<Boolean> = _isSubmittingRating.asStateFlow()

    fun loadRecipe(recipeId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                println("DEBUG: Loading recipe $recipeId")
                val result = recipeRepository.getRecipe(recipeId)
                println("DEBUG: Got recipe ${result.title}")
                _recipe.value = result
                // Independent of recipe load — if this fails we still want the recipe
                // visible. Rating absence is the common case (most recipes will be
                // unrated by any given viewer) so we don't surface its error.
                _myRating.value = try {
                    ratingRepository.getMyRating(recipeId, userId)?.rating ?: 0
                } catch (e: Exception) {
                    println("DEBUG: Error loading own rating ${e.message}")
                    0
                }
            } catch (e: Exception) {
                println("DEBUG: Error loading recipe ${e.message}")
                _error.value = e.message ?: "Failed to load recipe"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitRating(recipeId: String, userId: String, stars: Int) {
        if (stars !in 1..5) return
        // Optimistic update so the star fills the moment the user taps; rolled
        // back if the transaction throws. The recipe doc refetch below also
        // refreshes averageRating/ratingCount so the Average row updates.
        val previous = _myRating.value
        _myRating.value = stars
        _isSubmittingRating.value = true
        viewModelScope.launch {
            try {
                ratingRepository.submitRating(recipeId, userId, stars)
                _recipe.value = recipeRepository.getRecipe(recipeId)
            } catch (e: Exception) {
                _myRating.value = previous
                _error.value = e.message ?: "Failed to submit rating"
            } finally {
                _isSubmittingRating.value = false
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

package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Comment
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.CommentRepository
import org.maxwelltech.recipetree.data.repository.RatingRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.data.repository.UserProfileRepository

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository,
    private val ratingRepository: RatingRepository,
    private val commentRepository: CommentRepository,
    private val userProfileRepository: UserProfileRepository
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

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    /** Resolved author profiles keyed by userId. UI falls back to a placeholder when missing. */
    private val _commentAuthors = MutableStateFlow<Map<String, User>>(emptyMap())
    val commentAuthors: StateFlow<Map<String, User>> = _commentAuthors.asStateFlow()

    private val _commentInput = MutableStateFlow("")
    val commentInput: StateFlow<String> = _commentInput.asStateFlow()

    private val _isSubmittingComment = MutableStateFlow(false)
    val isSubmittingComment: StateFlow<Boolean> = _isSubmittingComment.asStateFlow()

    private val _commentError = MutableStateFlow<String?>(null)
    val commentError: StateFlow<String?> = _commentError.asStateFlow()

    // Same de-dupe pattern as CookbookDetailViewModel.lastResolvedMemberIds:
    // a comment list emission usually has the same authors as the previous one
    // (just one added/removed), so a naive re-fetch on every emission would
    // hammer Firestore for nothing.
    private var lastResolvedAuthorIds: Set<String> = emptySet()

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

    fun observeComments(recipeId: String) {
        viewModelScope.launch {
            try {
                commentRepository.observeComments(recipeId).collect { fetched ->
                    _comments.value = fetched
                    refreshAuthorsIfChanged(fetched.map { it.authorId })
                }
            } catch (e: Exception) {
                _commentError.value = e.message ?: "Failed to load comments"
            }
        }
    }

    private fun refreshAuthorsIfChanged(authorIds: List<String>) {
        val asSet = authorIds.toSet()
        if (asSet == lastResolvedAuthorIds) return
        lastResolvedAuthorIds = asSet
        if (asSet.isEmpty()) {
            _commentAuthors.value = emptyMap()
            return
        }
        viewModelScope.launch {
            try {
                _commentAuthors.value = userProfileRepository.getUsers(asSet)
            } catch (_: Exception) {
                // Graceful degradation: missing profiles render as "Unknown"
                // at the UI layer rather than blocking the thread render.
            }
        }
    }

    fun updateCommentInput(text: String) {
        _commentInput.value = text
    }

    fun submitComment(recipeId: String, authorId: String) {
        val text = _commentInput.value.trim()
        if (text.isEmpty()) return
        _isSubmittingComment.value = true
        _commentError.value = null
        viewModelScope.launch {
            try {
                commentRepository.addComment(recipeId, authorId, text)
                _commentInput.value = ""
                // observeComments flow will re-emit with the new comment included.
            } catch (e: Exception) {
                _commentError.value = e.message ?: "Failed to post comment"
            } finally {
                _isSubmittingComment.value = false
            }
        }
    }

    fun deleteComment(recipeId: String, commentId: String) {
        viewModelScope.launch {
            _commentError.value = null
            try {
                commentRepository.deleteComment(recipeId, commentId)
                // observeComments flow drops the deleted doc on its own.
            } catch (e: Exception) {
                _commentError.value = e.message ?: "Failed to delete comment"
            }
        }
    }

    fun clearCommentError() {
        _commentError.value = null
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

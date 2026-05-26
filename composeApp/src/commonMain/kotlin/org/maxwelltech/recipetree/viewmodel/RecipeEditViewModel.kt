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
import org.maxwelltech.recipetree.data.repository.PhotoStorageRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.ui.util.friendlyMessage

class RecipeEditViewModel(
    private val recipeRepository: RecipeRepository,
    private val cookbookRepository: CookbookRepository,
    private val photoStorageRepository: PhotoStorageRepository
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

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

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
                _error.value = friendlyMessage(e, "Couldn't load this recipe.")
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
                _error.value = friendlyMessage(e, "Couldn't load your cookbooks.")
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

    /**
     * Upload [bytes] to recipes/{recipeId}/{uuid}.jpg and append the resulting
     * URL to the recipe's photoUrls (replacing the first slot since the v1 UI
     * is single-hero). Mints a fresh recipe id lazily if this is a brand-new
     * recipe — saveRecipe() will honor that id when the doc is written.
     *
     * Caller (the screen) is expected to disable the picker while
     * isUploadingPhoto is true so a second pick can't race against an
     * in-flight upload and overwrite each other's _recipe writes.
     */
    fun uploadPhoto(bytes: ByteArray) {
        val current = _recipe.value
        val recipeId = current.id.ifEmpty { recipeRepository.newRecipeId() }

        _isUploadingPhoto.value = true
        _photoError.value = null
        viewModelScope.launch {
            try {
                val url = photoStorageRepository.uploadRecipePhoto(recipeId, bytes)
                _recipe.value = _recipe.value.copy(
                    id = recipeId,
                    photoUrls = listOf(url)
                )
            } catch (e: Exception) {
                _photoError.value = friendlyMessage(e, "Couldn't upload that photo.")
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    /**
     * Clear the photo locally. The Storage object stays — cleaning it up on
     * Save would be safer in theory (avoid orphans if the user picks again
     * after removing), but at family scale the orphan cost is cents/year and
     * doing the delete here would break the "cancel = no change" invariant
     * (deleting now then cancelling save would leave the recipe doc pointing
     * at a vanished URL → broken image).
     */
    fun removePhoto() {
        _recipe.value = _recipe.value.copy(photoUrls = emptyList())
    }

    fun clearPhotoError() {
        _photoError.value = null
    }

    fun deleteRecipe(recipeId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null
            try {
                recipeRepository.deleteRecipe(recipeId)
                onSuccess()
            } catch (e: Exception) {
                _error.value = friendlyMessage(e, "Couldn't delete this recipe.")
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
                _error.value = friendlyMessage(e, "Couldn't save this recipe.")
            } finally {
                _isSaving.value = false
            }
        }
    }
}

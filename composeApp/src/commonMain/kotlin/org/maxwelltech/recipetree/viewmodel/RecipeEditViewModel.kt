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
import org.maxwelltech.recipetree.data.repository.RecipeImportRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.platform.compressImageBytes
import org.maxwelltech.recipetree.ui.util.friendlyMessage

class RecipeEditViewModel(
    private val recipeRepository: RecipeRepository,
    private val cookbookRepository: CookbookRepository,
    private val photoStorageRepository: PhotoStorageRepository,
    private val recipeImportRepository: RecipeImportRepository
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

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    /**
     * Set true when the text import succeeded but the photo re-upload failed
     * (network error fetching the source image, or the encoded JPEG exceeded
     * the 8 MB Storage cap). Surfaces as a non-blocking note in the Photo
     * section so the recipe text is still importable; the user adds their
     * own photo via the existing picker.
     */
    private val _imageImportFailed = MutableStateFlow(false)
    val imageImportFailed: StateFlow<Boolean> = _imageImportFailed.asStateFlow()

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

    fun clearImportError() {
        _importError.value = null
    }

    fun clearImageImportFailed() {
        _imageImportFailed.value = false
    }

    /**
     * Fetch the recipe at [url], find its schema.org/Recipe JSON-LD, and
     * prefill the form. Mints a recipe id up-front so the source image
     * re-upload (which writes to recipes/{id}/...) has a stable place to
     * land before the recipe doc itself is saved.
     *
     * The image re-upload runs as a separate coroutine that's fire-and-
     * forget from the import's perspective — the text fields are usable
     * immediately and saved on success; if the image leg fails we set
     * imageImportFailed for the UI to surface a non-blocking note.
     */
    fun importFromUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            _importError.value = "That doesn't look like a web link. Paste the full URL starting with https://."
            return
        }

        _isImporting.value = true
        _importError.value = null
        _imageImportFailed.value = false

        viewModelScope.launch {
            try {
                val imported = recipeImportRepository.importFromUrl(trimmed)
                // Mint an id up-front so the image upload has a stable
                // recipes/{id}/... path. If the user already has an id on
                // _recipe (e.g. they uploaded a manual photo before tapping
                // Import), keep it.
                val recipeId = _recipe.value.id.ifEmpty { recipeRepository.newRecipeId() }
                val sourceImageUrl = imported.photoUrls.firstOrNull()

                // Merge imported fields onto current. Preserve any pre-import
                // user input on fields the import doesn't populate (tags,
                // isPrivate, ownerId, cookbookIds, id) and let imported
                // values win on the fields it does populate.
                val current = _recipe.value
                _recipe.value = current.copy(
                    id = recipeId,
                    title = imported.title,
                    description = imported.description,
                    ingredients = imported.ingredients,
                    steps = imported.steps,
                    servings = imported.servings,
                    sourceUrl = imported.sourceUrl,
                    // photoUrls intentionally NOT set yet — re-upload below
                    // owns it. If the import had no image, leave whatever
                    // the user might have already uploaded.
                    photoUrls = current.photoUrls
                )

                // Fire-and-forget image re-upload. Runs after the text is
                // already on screen so the user can start reviewing.
                if (sourceImageUrl != null) {
                    launchImageReupload(recipeId, sourceImageUrl)
                }
            } catch (e: Exception) {
                _importError.value = friendlyMessage(
                    e,
                    "Couldn't import that recipe. Try a different link or enter it manually."
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    /**
     * Download the source image, compress to JPEG, upload to our Storage so
     * the recipe owns its photo and survives the source site removing it.
     * On any failure: leave photoUrls empty and flip imageImportFailed so
     * the UI surfaces a "add your own photo" hint.
     */
    private fun launchImageReupload(recipeId: String, sourceImageUrl: String) {
        _isUploadingPhoto.value = true
        viewModelScope.launch {
            try {
                val raw = recipeImportRepository.fetchImage(sourceImageUrl)
                val jpeg = compressImageBytes(raw)
                val url = photoStorageRepository.uploadRecipePhoto(recipeId, jpeg)
                _recipe.value = _recipe.value.copy(photoUrls = listOf(url))
            } catch (_: Exception) {
                _imageImportFailed.value = true
            } finally {
                _isUploadingPhoto.value = false
            }
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

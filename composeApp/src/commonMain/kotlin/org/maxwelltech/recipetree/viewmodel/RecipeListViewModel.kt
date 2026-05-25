package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.ui.util.availableTags
import org.maxwelltech.recipetree.ui.util.filterRecipes

class RecipeListViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    /**
     * Recipes after applying searchQuery + selectedTag. Derived from the raw
     * recipes flow so a new Firestore emission flows through without any
     * extra wiring. authorsById is empty on this screen — observeUserRecipes
     * filters by ownerId == self, so author-name search would be a no-op
     * (always you). filterRecipes handles the empty map cleanly.
     */
    val displayedRecipes: StateFlow<List<Recipe>> = combine(
        _recipes,
        _searchQuery,
        _selectedTag
    ) { recipes, query, tag ->
        filterRecipes(recipes, query, tag, emptyMap<String, User>())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Distinct tags across the current recipe set, sorted for stable chip ordering. */
    val availableTags: StateFlow<List<String>> = _recipes
        .map { recipes -> availableTags(recipes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }
}

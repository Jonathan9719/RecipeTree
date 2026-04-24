package org.maxwelltech.recipetree.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.data.repository.UserProfileRepository

class CookbookDetailViewModel(
    private val cookbookRepository: CookbookRepository,
    private val recipeRepository: RecipeRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _cookbook = MutableStateFlow<Cookbook?>(null)
    val cookbook: StateFlow<Cookbook?> = _cookbook.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isProcessingMember = MutableStateFlow(false)
    val isProcessingMember: StateFlow<Boolean> = _isProcessingMember.asStateFlow()

    private val _memberActionError = MutableStateFlow<String?>(null)
    val memberActionError: StateFlow<String?> = _memberActionError.asStateFlow()

    // Track the last member set we resolved profiles for so we don't re-fetch on
    // every cookbook emission (e.g. when only the name changes).
    private var lastResolvedMemberIds: Set<String> = emptySet()

    fun observeCookbook(cookbookId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                cookbookRepository.observeCookbook(cookbookId).collect { cookbook ->
                    _cookbook.value = cookbook
                    _isLoading.value = false
                    if (cookbook != null) {
                        refreshMembersIfChanged(cookbook.memberIds)
                    } else {
                        _members.value = emptyList()
                        lastResolvedMemberIds = emptySet()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cookbook"
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

    private fun refreshMembersIfChanged(memberIds: List<String>) {
        val asSet = memberIds.toSet()
        if (asSet == lastResolvedMemberIds) return
        lastResolvedMemberIds = asSet
        viewModelScope.launch {
            try {
                val resolved = userProfileRepository.getUsers(asSet)
                // Keep the order of memberIds; fall back to a minimal placeholder for
                // ids we couldn't resolve (e.g. deleted users) so owners can still
                // remove them.
                _members.value = memberIds.map { id ->
                    resolved[id] ?: User(id = id, displayName = "Unknown member")
                }
            } catch (e: Exception) {
                _memberActionError.value = e.message ?: "Failed to load members"
            }
        }
    }

    fun removeMember(cookbookId: String, userId: String) {
        viewModelScope.launch {
            _isProcessingMember.value = true
            _memberActionError.value = null
            try {
                cookbookRepository.removeMember(cookbookId = cookbookId, userId = userId)
                // observeCookbook flow will emit the updated memberIds and trigger
                // refreshMembersIfChanged — no manual list mutation needed.
            } catch (e: Exception) {
                _memberActionError.value = e.message ?: "Failed to remove member"
            } finally {
                _isProcessingMember.value = false
            }
        }
    }

    fun leaveCookbook(cookbookId: String, userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isProcessingMember.value = true
            _memberActionError.value = null
            try {
                cookbookRepository.removeMember(cookbookId = cookbookId, userId = userId)
                onSuccess()
            } catch (e: Exception) {
                _memberActionError.value = e.message ?: "Failed to leave cookbook"
            } finally {
                _isProcessingMember.value = false
            }
        }
    }
}

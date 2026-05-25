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
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.model.Invite
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.InviteRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.data.repository.UserProfileRepository
import org.maxwelltech.recipetree.ui.util.availableTags
import org.maxwelltech.recipetree.ui.util.filterRecipes
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CookbookDetailViewModel(
    private val cookbookRepository: CookbookRepository,
    private val recipeRepository: RecipeRepository,
    private val userProfileRepository: UserProfileRepository,
    private val inviteRepository: InviteRepository
) : ViewModel() {

    /** Presets offered in the "+ New invite" UI. See [createInvite]. */
    enum class InvitePreset {
        /** Targeted invite — 1 use, 24h TTL. */
        SINGLE_USE_24H,
        /** Small-group invite — up to 10 uses, 7d TTL. */
        MULTI_USE_7D
    }

    private val _cookbook = MutableStateFlow<Cookbook?>(null)
    val cookbook: StateFlow<Cookbook?> = _cookbook.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _members = MutableStateFlow<List<User>>(emptyList())
    val members: StateFlow<List<User>> = _members.asStateFlow()

    private val _invites = MutableStateFlow<List<Invite>>(emptyList())
    val invites: StateFlow<List<Invite>> = _invites.asStateFlow()

    /** Set on successful createInvite so the UI can show the code in a dialog. Cleared with [clearNewlyCreatedInvite]. */
    private val _newlyCreatedInvite = MutableStateFlow<Invite?>(null)
    val newlyCreatedInvite: StateFlow<Invite?> = _newlyCreatedInvite.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isProcessingMember = MutableStateFlow(false)
    val isProcessingMember: StateFlow<Boolean> = _isProcessingMember.asStateFlow()

    private val _memberActionError = MutableStateFlow<String?>(null)
    val memberActionError: StateFlow<String?> = _memberActionError.asStateFlow()

    private val _isProcessingInvite = MutableStateFlow(false)
    val isProcessingInvite: StateFlow<Boolean> = _isProcessingInvite.asStateFlow()

    private val _inviteActionError = MutableStateFlow<String?>(null)
    val inviteActionError: StateFlow<String?> = _inviteActionError.asStateFlow()

    private val _recipeAuthors = MutableStateFlow<Map<String, User>>(emptyMap())
    val recipeAuthors: StateFlow<Map<String, User>> = _recipeAuthors.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    /**
     * Recipes filtered by [searchQuery] and [selectedTag], with author display
     * names resolved so the "author" arm of the search matches across multiple
     * owners (cookbook detail can show recipes from anyone in the cookbook,
     * unlike the user's own recipe list).
     */
    val displayedRecipes: StateFlow<List<Recipe>> = combine(
        _recipes,
        _searchQuery,
        _selectedTag,
        _recipeAuthors
    ) { recipes, query, tag, authors ->
        filterRecipes(recipes, query, tag, authors)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Distinct tags across the current recipe set, sorted alphabetically. */
    val availableTags: StateFlow<List<String>> = _recipes
        .map { recipes -> availableTags(recipes) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Track the last member set we resolved profiles for so we don't re-fetch on
    // every cookbook emission (e.g. when only the name changes).
    private var lastResolvedMemberIds: Set<String> = emptySet()

    // Same trick for the recipe owner set — a new recipe doesn't usually mean a
    // new author, so most emissions skip the profile re-fetch entirely.
    private var lastResolvedAuthorIds: Set<String> = emptySet()

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
                    refreshRecipeAuthorsIfChanged(recipes.map { it.ownerId })
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load recipes"
            }
        }
    }

    private fun refreshRecipeAuthorsIfChanged(authorIds: List<String>) {
        val asSet = authorIds.toSet()
        if (asSet == lastResolvedAuthorIds) return
        lastResolvedAuthorIds = asSet
        if (asSet.isEmpty()) {
            _recipeAuthors.value = emptyMap()
            return
        }
        viewModelScope.launch {
            try {
                _recipeAuthors.value = userProfileRepository.getUsers(asSet)
            } catch (_: Exception) {
                // Graceful degradation: author search just misses for the
                // unresolved entries, same as the comment-author path.
            }
        }
    }

    fun observeInvites(cookbookId: String) {
        viewModelScope.launch {
            try {
                inviteRepository.observeCookbookInvites(cookbookId).collect { invites ->
                    _invites.value = invites.sortedByDescending { it.createdAt ?: 0L }
                }
            } catch (e: Exception) {
                _inviteActionError.value = e.message ?: "Failed to load invites"
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

    fun createInvite(cookbookId: String, createdBy: String, preset: InvitePreset) {
        viewModelScope.launch {
            _isProcessingInvite.value = true
            _inviteActionError.value = null
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val (maxUses, ttl) = when (preset) {
                    InvitePreset.SINGLE_USE_24H -> 1 to 24.hours
                    InvitePreset.MULTI_USE_7D -> 10 to 7.days
                }
                val invite = inviteRepository.createInvite(
                    cookbookId = cookbookId,
                    createdBy = createdBy,
                    maxUses = maxUses,
                    expiresAt = now + ttl.inWholeMilliseconds
                )
                _newlyCreatedInvite.value = invite
            } catch (e: Exception) {
                _inviteActionError.value = e.message ?: "Failed to create invite"
            } finally {
                _isProcessingInvite.value = false
            }
        }
    }

    fun revokeInvite(code: String) {
        viewModelScope.launch {
            _isProcessingInvite.value = true
            _inviteActionError.value = null
            try {
                inviteRepository.revokeInvite(code)
                // observeInvites flow will re-emit without the revoked doc.
            } catch (e: Exception) {
                _inviteActionError.value = e.message ?: "Failed to revoke invite"
            } finally {
                _isProcessingInvite.value = false
            }
        }
    }

    fun clearNewlyCreatedInvite() {
        _newlyCreatedInvite.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }
}

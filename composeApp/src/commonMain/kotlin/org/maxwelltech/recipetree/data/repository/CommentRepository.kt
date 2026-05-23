package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.Comment

interface CommentRepository {

    /** Live stream of comments on a recipe, sorted newest-first. */
    fun observeComments(recipeId: String): Flow<List<Comment>>

    /**
     * Append a comment authored by [authorId]. Returns the saved Comment with its
     * generated id populated so the caller can address it without re-querying.
     */
    suspend fun addComment(
        recipeId: String,
        authorId: String,
        text: String,
        cookbookContext: String? = null
    ): Comment

    /**
     * Hard-delete a comment doc. Permission checking (author or recipe owner) is
     * the caller's job at the UI layer; firestore.rules enforces the same
     * invariant server-side.
     */
    suspend fun deleteComment(recipeId: String, commentId: String)
}

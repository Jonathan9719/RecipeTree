package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.maxwelltech.recipetree.data.model.Comment
import org.maxwelltech.recipetree.data.repository.CommentRepository

@OptIn(ExperimentalTime::class)
class FirebaseCommentRepository(
    private val firestore: FirebaseFirestore
) : CommentRepository {

    private val recipesCollection = firestore.collection("recipes")

    override fun observeComments(recipeId: String): Flow<List<Comment>> {
        // Client-side sort matches the existing pattern in observeCookbookInvites
        // (CookbookDetailViewModel) — avoids an extra Firestore composite index
        // and keeps the read query trivial. Family-scale comment counts are tiny.
        return recipesCollection
            .document(recipeId)
            .collection("comments")
            .snapshots
            .map { snap ->
                snap.documents
                    .map { it.data<Comment>() }
                    .sortedByDescending { it.createdAt ?: 0L }
            }
    }

    override suspend fun addComment(
        recipeId: String,
        authorId: String,
        text: String,
        cookbookContext: String?
    ): Comment {
        val docRef = recipesCollection
            .document(recipeId)
            .collection("comments")
            .document
        val comment = Comment(
            id = docRef.id,
            authorId = authorId,
            text = text,
            cookbookContext = cookbookContext,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        docRef.set(comment)
        return comment
    }

    override suspend fun deleteComment(recipeId: String, commentId: String) {
        recipesCollection
            .document(recipeId)
            .collection("comments")
            .document(commentId)
            .delete()
    }
}

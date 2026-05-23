package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.maxwelltech.recipetree.data.model.Rating
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RatingRepository

@OptIn(ExperimentalTime::class)
class FirebaseRatingRepository(
    private val firestore: FirebaseFirestore
) : RatingRepository {

    private val recipesCollection = firestore.collection("recipes")

    override suspend fun getMyRating(recipeId: String, userId: String): Rating? {
        val snap = recipesCollection
            .document(recipeId)
            .collection("ratings")
            .document(userId)
            .get()
        return if (snap.exists) snap.data<Rating>() else null
    }

    override suspend fun submitRating(recipeId: String, userId: String, stars: Int) {
        require(stars in 1..5) { "Rating must be between 1 and 5, got $stars" }

        val recipeRef = recipesCollection.document(recipeId)
        val ratingRef = recipeRef.collection("ratings").document(userId)

        // One transaction for read-recipe + read-existing-rating + write-rating +
        // patch-recipe-aggregates. Firestore retries the whole block if either doc
        // changes between read and commit, so two users rating the same recipe at
        // the same time can't leave averageRating inconsistent.
        firestore.runTransaction {
            // All reads first (Firestore transaction contract).
            val recipeSnap = get(recipeRef)
            if (!recipeSnap.exists) throw IllegalStateException("Recipe not found")
            val recipe = recipeSnap.data<Recipe>()

            val existingSnap = get(ratingRef)
            val existing = if (existingSnap.exists) existingSnap.data<Rating>() else null
            val now = Clock.System.now().toEpochMilliseconds()

            // Two recompute paths. Edit keeps ratingCount fixed and swaps old→new in
            // the running total; create bumps the count and folds the new rating in.
            // The float math is intentional — averageRating is Float on Recipe.
            val newAverage: Float
            val newCount: Int
            if (existing != null) {
                val total = recipe.averageRating * recipe.ratingCount
                val updatedTotal = total - existing.rating + stars
                newCount = recipe.ratingCount
                newAverage = if (newCount > 0) updatedTotal / newCount else stars.toFloat()
            } else {
                val total = recipe.averageRating * recipe.ratingCount
                newCount = recipe.ratingCount + 1
                newAverage = (total + stars) / newCount
            }

            // Writes.
            set(
                ratingRef,
                Rating(
                    userId = userId,
                    rating = stars,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
            update(
                recipeRef,
                mapOf(
                    "averageRating" to newAverage,
                    "ratingCount" to newCount
                )
            )
        }
    }
}

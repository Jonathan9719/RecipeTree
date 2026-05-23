package org.maxwelltech.recipetree.data.repository

import org.maxwelltech.recipetree.data.model.Rating

interface RatingRepository {

    /** Returns the user's own rating on a recipe, or null if they haven't rated it yet. */
    suspend fun getMyRating(recipeId: String, userId: String): Rating?

    /**
     * Set [userId]'s rating on [recipeId] to [stars] (1..5). Creates the rating doc if
     * absent, overwrites if present, and recomputes the parent recipe's averageRating /
     * ratingCount in the same Firestore transaction so the denormalized fields can't
     * drift from the underlying ratings.
     *
     * Edit-vs-create matters for the math: a new rating bumps ratingCount, an edit
     * doesn't, and the average formula differs accordingly (see FirebaseRatingRepository).
     */
    suspend fun submitRating(recipeId: String, userId: String, stars: Int)
}

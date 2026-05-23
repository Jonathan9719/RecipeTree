package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val authorId: String = "",
    val text: String = "",
    /**
     * The cookbook the author was viewing when they posted. Design doc §3.2
     * specifies this field so a future UI could surface "commented while
     * viewing [Family Favorites]" — wiring it through requires
     * Route.RecipeDetail to carry an optional cookbookId. Deferred for v1
     * but stored as nullable so plumbing it later is purely additive.
     */
    val cookbookContext: String? = null,
    val createdAt: Long? = null
)

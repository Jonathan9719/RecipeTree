package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Recipe(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String? = null,
    val ingredients: List<Ingredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val servings: Int = 4,
    val photoUrls: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val cookbookIds: List<String> = emptyList(),
    val averageRating: Float = 0f,
    val ratingCount: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    /**
     * URL the recipe was imported from, or null if entered manually. Surfaced on
     * the detail screen as a tappable "Imported from <domain>" attribution
     * line. Nullable + defaulted so every existing Firestore doc deserializes
     * unchanged.
     */
    val sourceUrl: String? = null
)

@Serializable
data class Ingredient(
    val name: String = "",
    val amount: String = "",
    val unit: String = ""
)
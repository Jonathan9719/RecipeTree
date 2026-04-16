package org.maxwelltech.recipetree.data.model

import kotlin.time.Instant

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
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null
)

data class Ingredient(
    val name: String = "",
    val amount: String = "",
    val unit: String = ""
)
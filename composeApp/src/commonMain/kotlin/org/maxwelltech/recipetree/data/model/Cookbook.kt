package org.maxwelltech.recipetree.data.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Cookbook(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val visibility: CookbookVisibility = CookbookVisibility.PRIVATE,
    val coverPhotoUrl: String? = null,
    val description: String? = null,
    val createdAt: Instant? = null
)

@Serializable
enum class CookbookVisibility {
    PRIVATE,
    UNLISTED,
    PUBLIC
}
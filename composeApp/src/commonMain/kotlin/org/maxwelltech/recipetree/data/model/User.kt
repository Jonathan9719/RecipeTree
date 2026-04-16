package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long? = null
)
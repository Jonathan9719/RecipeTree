package org.maxwelltech.recipetree.data.model

import kotlin.time.Instant

data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val createdAt: Instant? = null
)
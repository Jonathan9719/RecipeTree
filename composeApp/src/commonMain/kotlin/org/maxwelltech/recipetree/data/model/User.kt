package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val createdAt: Long? = null,
    /**
     * Flipped to true after the user dismisses the first-launch welcome
     * carousel (Skip or Get started on the final card). Null/false means
     * the carousel still hasn't been seen — every cold start re-checks
     * this and routes to WelcomeScreen until it's true. Nullable +
     * defaulted so every existing User doc deserializes unchanged with
     * no migration.
     */
    val seenWelcome: Boolean? = null
)
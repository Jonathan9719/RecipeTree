package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Rating(
    // Mirrors the doc id under recipes/{recipeId}/ratings/{userId} — keeping
    // it in the body too lets callers carry a Rating around without also
    // tracking the parent recipe id, and matches how Recipe / Cookbook / User
    // also duplicate id into the body.
    val userId: String = "",
    /** 1..5. 0 is reserved for "no rating yet" at the VM layer and is never persisted. */
    val rating: Int = 0,
    val createdAt: Long? = null,
    /** Stamped on edit as well as create, so the average-recompute path can be audited. */
    val updatedAt: Long? = null
)

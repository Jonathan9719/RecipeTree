package org.maxwelltech.recipetree.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Invite(
    /** 8-character uppercase code, no confusable characters. Also the doc id in `invites/{code}`. */
    val code: String = "",
    val cookbookId: String = "",
    val createdBy: String = "",
    val createdAt: Long? = null,
    /**
     * Millis since epoch. Null means no expiry (we don't create these in v1, but
     * the field stays nullable so existing docs without the field parse cleanly).
     */
    val expiresAt: Long? = null,
    val maxUses: Int = 1,
    val usedCount: Int = 0,
    val revoked: Boolean = false,
    /** Role granted on accept. Just "member" in v1. */
    val role: String = "member"
)

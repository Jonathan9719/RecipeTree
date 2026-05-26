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
    val seenWelcome: Boolean? = null,
    /**
     * Denormalized list of cookbook ids the user is currently a member of.
     * Mirrors the union of `cookbooks/{id}.memberIds contains userId` queries
     * and exists so the recipes Firestore read rule can do a single
     * `get(/users/$uid).data.memberCookbookIds.hasAny(recipe.cookbookIds)`
     * check without iterating — Firestore rules can't loop over arrays.
     *
     * Maintained by FirebaseCookbookRepository.addMember / removeMember,
     * FirebaseInviteRepository.acceptInvite, and a per-sign-in backfill in
     * FirebaseAuthRepository (idempotent re-sync against the source of
     * truth — `cookbooks where memberIds array-contains me`).
     *
     * Stale entries can linger if a cookbook is deleted without cascade
     * (consistent with the existing "orphans accepted at family scale"
     * policy documented in the action plan). The recipes rule degrades
     * gracefully — a stale cookbookId in this list just means hasAny
     * might match against deleted cookbooks, granting reads on already-
     * orphaned recipes that nothing references anyway.
     */
    val memberCookbookIds: List<String> = emptyList()
)
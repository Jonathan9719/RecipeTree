package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.Invite

interface InviteRepository {

    /**
     * Create a fresh invite for [cookbookId]. The repo generates a new 8-char code
     * (retrying on the astronomically unlikely collision) and writes the doc.
     */
    suspend fun createInvite(
        cookbookId: String,
        createdBy: String,
        maxUses: Int,
        expiresAt: Long?
    ): Invite

    /** O(1) lookup by code. Returns null if not found. [code] may contain dashes — they're stripped. */
    suspend fun getInvite(code: String): Invite?

    /** Hard-delete the invite doc so the code stops working immediately. */
    suspend fun revokeInvite(code: String)

    /** Live list of active invites for a cookbook (for the owner's management UI). */
    fun observeCookbookInvites(cookbookId: String): Flow<List<Invite>>

    /** Live list of invites this user created — used by account deletion to revoke them. */
    fun observeUserCreatedInvites(userId: String): Flow<List<Invite>>

    /**
     * Validate and consume an invite in a single transaction, then add [userId] to the
     * cookbook's memberIds + members subcollection. Returns the cookbookId on success.
     * Throws if the invite is missing, revoked, expired, or fully used.
     */
    suspend fun acceptInvite(code: String, userId: String): String
}

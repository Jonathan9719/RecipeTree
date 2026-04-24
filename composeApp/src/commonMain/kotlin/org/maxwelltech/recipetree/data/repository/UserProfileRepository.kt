package org.maxwelltech.recipetree.data.repository

import org.maxwelltech.recipetree.data.model.User

interface UserProfileRepository {

    /** Fetch a single user profile. Returns null if the user doc doesn't exist. */
    suspend fun getUser(userId: String): User?

    /**
     * Batch-fetch profiles for a set of user ids. The returned map only contains
     * entries for ids that were found — missing users are silently dropped. Order
     * is not preserved; callers should look up by id.
     */
    suspend fun getUsers(userIds: Collection<String>): Map<String, User>
}

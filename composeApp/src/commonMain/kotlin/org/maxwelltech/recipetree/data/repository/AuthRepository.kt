package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.maxwelltech.recipetree.data.model.User

interface AuthRepository {

    // The currently signed-in user, or null if signed out.
    // Exposed as a StateFlow so profile edits (which Firebase Auth's
    // authStateChanged does NOT re-emit for) can be patched into the same
    // source of truth — every screen reading this gets the live value.
    val currentUser: StateFlow<User?>

    // Returns the signed-in User on success, throws on failure
    suspend fun signIn(email: String, password: String): User

    // Creates auth account + user document in Firestore
    suspend fun signUp(email: String, password: String, displayName: String): User

    suspend fun signOut()

    /** Update display name in both Firebase Auth and the users/{uid} Firestore doc. */
    suspend fun updateDisplayName(newName: String)

    /**
     * Re-authenticate the current user with their existing password. Firebase
     * requires this before any "recent auth" operation (changePassword,
     * delete, email update). Throws if signed out or the password is wrong.
     */
    suspend fun reauthenticate(currentPassword: String)

    /**
     * Change the current user's password. Re-authenticates with [currentPassword]
     * first because Firebase requires recent auth for password updates, then
     * calls updatePassword([newPassword]).
     *
     * Throws if the user is signed out, the current password is wrong, or the
     * new password fails Firebase's minimum length rule (6 chars).
     */
    suspend fun changePassword(currentPassword: String, newPassword: String)

    /**
     * Delete the current user's profile doc and Firebase Auth account. Assumes
     * the caller has already called [reauthenticate]; if recent auth has
     * lapsed, Firebase rejects the underlying delete().
     *
     * This is the "point of no return" — callers should do any data-cascade
     * work (deleting owned cookbooks, recipes, invites, leaving memberships)
     * BEFORE calling this so a partial failure leaves the user able to retry.
     */
    suspend fun deleteCurrentUser()

    suspend fun sendPasswordResetEmail(email: String)

    // Hook for future SSO — implementations will handle Google, Apple etc.
    suspend fun signInWithSsoToken(token: String, provider: String): User
}
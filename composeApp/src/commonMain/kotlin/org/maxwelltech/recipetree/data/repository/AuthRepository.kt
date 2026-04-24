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

    suspend fun sendPasswordResetEmail(email: String)

    // Hook for future SSO — implementations will handle Google, Apple etc.
    suspend fun signInWithSsoToken(token: String, provider: String): User
}
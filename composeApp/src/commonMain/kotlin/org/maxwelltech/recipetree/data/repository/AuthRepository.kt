package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.User

interface AuthRepository {

    // The currently signed-in user, or null if signed out.
    // Flow means the UI automatically reacts when auth state changes.
    val currentUser: Flow<User?>

    // Returns the signed-in User on success, throws on failure
    suspend fun signIn(email: String, password: String): User

    // Creates auth account + user document in Firestore
    suspend fun signUp(email: String, password: String, displayName: String): User

    suspend fun signOut()

    suspend fun sendPasswordResetEmail(email: String)

    // Hook for future SSO — implementations will handle Google, Apple etc.
    suspend fun signInWithSsoToken(token: String, provider: String): User
}
package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.AuthRepository

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("users")

    override val currentUser: Flow<User?> = auth.authStateChanged.map { firebaseUser ->
        firebaseUser?.let {
            User(
                id = it.uid,
                displayName = it.displayName ?: "",
                email = it.email ?: ""
            )
        }
    }

    override suspend fun signIn(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password)
        val firebaseUser = result.user
            ?: throw Exception("Sign in failed — no user returned")
        return User(
            id = firebaseUser.uid,
            displayName = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: ""
        )
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): User {
        val result = auth.createUserWithEmailAndPassword(email, password)
        val firebaseUser = result.user
            ?: throw Exception("Sign up failed — no user returned")

        // Update display name in Firebase Auth
        firebaseUser.updateProfile(displayName = displayName)

        // Create user document in Firestore
        val user = User(
            id = firebaseUser.uid,
            displayName = displayName,
            email = email
        )
        usersCollection.document(firebaseUser.uid).set(user)

        return user
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
    }

    override suspend fun signInWithSsoToken(token: String, provider: String): User {
        // Placeholder for future Google/Apple SSO
        // Will use auth.signInWithCredential() when implemented
        throw NotImplementedError("SSO not yet implemented — coming soon")
    }
}
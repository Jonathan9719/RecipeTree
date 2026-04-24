package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.AuthRepository

class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val usersCollection = firestore.collection("users")

    // Owns the lifetime of the authStateChanged bridge below. The repo is a
    // singleton via AppContainer so this scope lives as long as the app does.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Bridge Firebase Auth's authStateChanged into our StateFlow. Anyone
        // collecting currentUser gets both auth state changes (sign-in /
        // sign-out, from Firebase) AND profile edits (from updateDisplayName
        // below, which patches _currentUser directly since authStateChanged
        // doesn't re-emit for profile changes).
        scope.launch {
            auth.authStateChanged.collect { firebaseUser ->
                _currentUser.value = firebaseUser?.let {
                    User(
                        id = it.uid,
                        displayName = it.displayName ?: "",
                        email = it.email ?: "",
                    )
                }
            }
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

    override suspend fun updateDisplayName(newName: String) {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("Must be signed in to update profile")
        // Firestore first — it's the source of truth other members read from in
        // the member list. If the Auth update fails afterward we're left with a
        // minor inconsistency (Firestore new, Auth old) that a subsequent retry
        // self-corrects; the reverse would leave other users seeing a stale name.
        usersCollection.document(firebaseUser.uid).update(
            mapOf("displayName" to newName)
        )
        firebaseUser.updateProfile(displayName = newName)
        // Firebase Auth's authStateChanged doesn't fire for profile edits, so
        // patch our StateFlow directly. Every consumer of currentUser (top-bar
        // avatar, profile screen, etc.) now reflects the new name immediately.
        _currentUser.update { it?.copy(displayName = newName) }
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
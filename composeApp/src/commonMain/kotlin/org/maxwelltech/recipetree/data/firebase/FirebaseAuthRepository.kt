package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.EmailAuthProvider
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
        //
        // We also fetch the user's Firestore profile doc on each auth state
        // change so fields that live ONLY in Firestore — avatarUrl, createdAt,
        // seenWelcome — flow through currentUser the same way Firebase Auth
        // fields do. Costs one Firestore read per auth state change; falls
        // back gracefully to the Firebase Auth-only fields if the read fails
        // (network error, missing doc on legacy accounts).
        scope.launch {
            auth.authStateChanged.collect { firebaseUser ->
                _currentUser.value = firebaseUser?.let { fbUser ->
                    val profile = runCatching {
                        val snapshot = usersCollection.document(fbUser.uid).get()
                        if (snapshot.exists) snapshot.data<User>() else null
                    }.getOrNull()
                    User(
                        id = fbUser.uid,
                        displayName = profile?.displayName?.takeUnless { it.isBlank() }
                            ?: (fbUser.displayName ?: ""),
                        email = profile?.email?.takeUnless { it.isBlank() }
                            ?: (fbUser.email ?: ""),
                        avatarUrl = profile?.avatarUrl,
                        createdAt = profile?.createdAt,
                        seenWelcome = profile?.seenWelcome
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
        val userDocRef = usersCollection.document(firebaseUser.uid)
        // Firestore first — source of truth for the member list. Branch on doc
        // existence because Firestore's update() throws NOT_FOUND on missing
        // docs (it's not a permission issue, just a strict contract). Accounts
        // that signed up before signUp started writing users/{uid} need a
        // backfill on first edit; everyone else just gets the displayName patch.
        val snapshot = userDocRef.get()
        if (snapshot.exists) {
            userDocRef.update(mapOf("displayName" to newName))
        } else {
            userDocRef.set(
                User(
                    id = firebaseUser.uid,
                    displayName = newName,
                    email = firebaseUser.email ?: ""
                )
            )
        }
        firebaseUser.updateProfile(displayName = newName)
        // Firebase Auth's authStateChanged doesn't fire for profile edits, so
        // patch our StateFlow directly. Every consumer of currentUser (top-bar
        // avatar, profile screen, etc.) now reflects the new name immediately.
        _currentUser.update { it?.copy(displayName = newName) }
    }

    override suspend fun reauthenticate(currentPassword: String) {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("Must be signed in to re-authenticate")
        val email = firebaseUser.email
            ?: throw IllegalStateException("Current account has no email on file")
        val credential = EmailAuthProvider.credential(email = email, password = currentPassword)
        firebaseUser.reauthenticate(credential)
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String) {
        // Re-auth first — Firebase requires it for updatePassword and if the
        // current password is wrong this throws before we touch the password.
        reauthenticate(currentPassword)
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("Must be signed in to change password")
        firebaseUser.updatePassword(newPassword)
    }

    override suspend fun deleteCurrentUser() {
        val firebaseUser = auth.currentUser
            ?: throw IllegalStateException("Must be signed in to delete account")
        // Best-effort clean of the Firestore profile doc first — once the Auth
        // user is gone, the rules' isSelf() check on users/{uid} can no longer
        // succeed and the doc would be orphaned. Wrapped in a try so a missing
        // profile doc (already cleaned up by a prior retry) doesn't block the
        // Auth delete.
        try {
            usersCollection.document(firebaseUser.uid).delete()
        } catch (_: Exception) {
            // Either the doc never existed (edge case) or rules rejected it.
            // We'll still attempt the Auth delete below; an admin tool can
            // sweep stragglers later.
        }
        firebaseUser.delete()
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
    }

    override suspend fun signInWithSsoToken(token: String, provider: String): User {
        // Placeholder for future Google/Apple SSO
        // Will use auth.signInWithCredential() when implemented
        throw NotImplementedError("SSO not yet implemented — coming soon")
    }

    override suspend fun markWelcomeSeen() {
        // Patch the StateFlow FIRST so the welcome redirect clears immediately
        // even if the Firestore write fails — user doesn't get stuck on the
        // carousel because of a transient network blip. Same pattern as the
        // updateDisplayName flow's optimistic StateFlow patch.
        _currentUser.update { it?.copy(seenWelcome = true) }

        val firebaseUser = auth.currentUser ?: return
        val userDocRef = usersCollection.document(firebaseUser.uid)
        try {
            val snapshot = userDocRef.get()
            if (snapshot.exists) {
                userDocRef.update(mapOf("seenWelcome" to true))
            } else {
                // Legacy account with no users/{uid} doc — backfill the way
                // updateDisplayName does. Same auth.currentUser-derived shape.
                userDocRef.set(
                    User(
                        id = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        seenWelcome = true
                    )
                )
            }
        } catch (_: Exception) {
            // Swallow — the StateFlow patch above already unblocked the UI.
            // Worst case: the next sign-in re-shows the welcome carousel,
            // which is annoying but not broken.
        }
    }
}
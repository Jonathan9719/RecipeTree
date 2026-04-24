package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.maxwelltech.recipetree.data.model.User
import org.maxwelltech.recipetree.data.repository.UserProfileRepository

class FirebaseUserProfileRepository(
    private val firestore: FirebaseFirestore
) : UserProfileRepository {

    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(userId: String): User? {
        if (userId.isEmpty()) return null
        val snapshot = usersCollection.document(userId).get()
        return if (snapshot.exists) snapshot.data<User>() else null
    }

    override suspend fun getUsers(userIds: Collection<String>): Map<String, User> {
        if (userIds.isEmpty()) return emptyMap()
        // Parallel doc reads — simpler than chunked `in` queries and plenty fast for
        // typical membership sizes (<20). If a cookbook ever has hundreds of members
        // we can switch to chunked `where(FieldPath.documentId) in <=10-chunk`.
        return coroutineScope {
            userIds
                .toSet()
                .filter { it.isNotEmpty() }
                .map { id -> id to async { getUser(id) } }
                .mapNotNull { (id, deferred) ->
                    deferred.await()?.let { user -> id to user }
                }
                .toMap()
        }
    }
}

package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maxwelltech.recipetree.data.model.Cookbook
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import kotlinx.serialization.Serializable

@Serializable
private data class RecipeRef(
    val addedById: String = ""
)

@Serializable
private data class MemberDoc(
    val role: String = ""
)

class FirebaseCookbookRepository(
    private val firestore: FirebaseFirestore
) : CookbookRepository {

    private val cookbooksCollection = firestore.collection("cookbooks")

    override suspend fun getCookbook(id: String): Cookbook {
        return cookbooksCollection.document(id).get().data()
    }

    override suspend fun createCookbook(cookbook: Cookbook) {
        val docRef = if (cookbook.id.isEmpty()) {
            cookbooksCollection.document // auto-generates ID
        } else {
            cookbooksCollection.document(cookbook.id)
        }
        // Write back the generated ID into the object
        docRef.set(cookbook.copy(id = docRef.id))
    }

    override suspend fun deleteCookbook(id: String) {
        cookbooksCollection.document(id).delete()
    }

    override fun observeUserCookbooks(userId: String): Flow<List<Cookbook>> {
        return cookbooksCollection
            .where { "memberIds" contains userId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data() } }
    }

    override suspend fun addRecipeToCookbook(
        recipeId: String,
        cookbookId: String,
        addedById: String
    ) {
        val batch = firestore.batch()

        val recipeRef = cookbooksCollection
            .document(cookbookId)
            .collection("recipeRefs")
            .document(recipeId)
        batch.set(recipeRef, RecipeRef(addedById = addedById))

        val recipe = firestore.collection("recipes").document(recipeId)
        batch.update(recipe, mapOf("cookbookIds" to FieldValue.arrayUnion(cookbookId)))

        batch.commit()
    }

    override suspend fun removeRecipeFromCookbook(recipeId: String, cookbookId: String) {
        val batch = firestore.batch()

        val recipeRef = cookbooksCollection
            .document(cookbookId)
            .collection("recipeRefs")
            .document(recipeId)
        batch.delete(recipeRef)

        val recipe = firestore.collection("recipes").document(recipeId)
        batch.update(recipe, mapOf("cookbookIds" to FieldValue.arrayRemove(cookbookId)))

        batch.commit()
    }

    override suspend fun addMember(cookbookId: String, userId: String, role: String) {
        val batch = firestore.batch()

        val cookbookRef = cookbooksCollection.document(cookbookId)
        batch.update(cookbookRef, mapOf("memberIds" to FieldValue.arrayUnion(userId)))

        val memberRef = cookbooksCollection
            .document(cookbookId)
            .collection("members")
            .document(userId)
        batch.set(memberRef, MemberDoc(role = role))

        batch.commit()
    }

    override suspend fun removeMember(cookbookId: String, userId: String) {
        val batch = firestore.batch()

        val cookbookRef = cookbooksCollection.document(cookbookId)
        batch.update(cookbookRef, mapOf("memberIds" to FieldValue.arrayRemove(userId)))

        val memberRef = cookbooksCollection
            .document(cookbookId)
            .collection("members")
            .document(userId)
        batch.delete(memberRef)

        batch.commit()
    }
}
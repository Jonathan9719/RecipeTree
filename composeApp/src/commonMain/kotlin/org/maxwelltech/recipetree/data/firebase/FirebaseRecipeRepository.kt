package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RecipeRepository

class FirebaseRecipeRepository(
    private val firestore: FirebaseFirestore
) : RecipeRepository {

    private val recipesCollection = firestore.collection("recipes")

    override suspend fun getRecipe(id: String): Recipe {
        return recipesCollection.document(id).get().data()
    }

    override suspend fun saveRecipe(recipe: Recipe): Recipe {
        val docRef = if (recipe.id.isEmpty()) {
            recipesCollection.document
        } else {
            recipesCollection.document(recipe.id)
        }
        val saved = recipe.copy(id = docRef.id)
        docRef.set(saved)
        return saved
    }

    override suspend fun deleteRecipe(id: String) {
        recipesCollection.document(id).delete()
    }

    override fun observeUserRecipes(userId: String): Flow<List<Recipe>> {
        return recipesCollection
            .where { "ownerId" equalTo userId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data() } }
    }

    override fun observeCookbookRecipes(cookbookId: String): Flow<List<Recipe>> {
        return recipesCollection
            .where { "cookbookIds" contains cookbookId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data() } }
    }

    override fun newRecipeId(): String {
        // Firestore's auto-id generation is client-side — collection.document
        // (no id arg) returns a reference with a freshly minted id and no
        // write. saveRecipe(recipe.copy(id = thisId)) later writes the doc.
        return recipesCollection.document.id
    }
}
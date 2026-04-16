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

    override suspend fun saveRecipe(recipe: Recipe) {
        val docRef = if (recipe.id.isEmpty()) {
            recipesCollection.document
        } else {
            recipesCollection.document(recipe.id)
        }
        docRef.set(recipe.copy(id = docRef.id))
    }

    override suspend fun deleteRecipe(id: String) {
        recipesCollection.document(id).delete()
    }

    override suspend fun getUserRecipes(userId: String): List<Recipe> {
        return recipesCollection
            .where { "ownerId" equalTo userId }
            .get()
            .documents
            .map { it.data() }
    }

    override fun observeCookbookRecipes(cookbookId: String): Flow<List<Recipe>> {
        return recipesCollection
            .where { "cookbookIds" contains cookbookId }
            .snapshots
            .map { snapshot -> snapshot.documents.map { it.data() } }
    }
}
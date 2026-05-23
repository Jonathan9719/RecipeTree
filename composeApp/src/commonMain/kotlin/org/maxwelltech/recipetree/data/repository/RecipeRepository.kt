package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.Recipe

interface RecipeRepository {
    suspend fun getRecipe(id: String): Recipe
    suspend fun saveRecipe(recipe: Recipe): Recipe
    suspend fun deleteRecipe(id: String)
    fun observeUserRecipes(userId: String): Flow<List<Recipe>>
    fun observeCookbookRecipes(cookbookId: String): Flow<List<Recipe>>

    /**
     * Mint a fresh recipe id without writing anything. Used by the new-recipe
     * edit flow so a photo can be uploaded to recipes/{id}/... before the
     * recipe doc itself is saved — saveRecipe() honors a pre-set id.
     */
    fun newRecipeId(): String
}
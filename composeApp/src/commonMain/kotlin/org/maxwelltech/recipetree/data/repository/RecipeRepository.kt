package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.Recipe

interface RecipeRepository {
    suspend fun getRecipe(id: String): Recipe
    suspend fun saveRecipe(recipe: Recipe)
    suspend fun deleteRecipe(id: String)
    suspend fun getUserRecipes(userId: String): List<Recipe>
    fun observeCookbookRecipes(cookbookId: String): Flow<List<Recipe>>
}
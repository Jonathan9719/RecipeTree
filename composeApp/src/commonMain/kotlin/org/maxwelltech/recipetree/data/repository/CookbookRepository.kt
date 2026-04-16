package org.maxwelltech.recipetree.data.repository

import kotlinx.coroutines.flow.Flow
import org.maxwelltech.recipetree.data.model.Cookbook

interface CookbookRepository {
    suspend fun getCookbook(id: String): Cookbook
    suspend fun createCookbook(cookbook: Cookbook)
    suspend fun deleteCookbook(id: String)
    fun observeUserCookbooks(userId: String): Flow<List<Cookbook>>
    suspend fun addRecipeToCookbook(recipeId: String, cookbookId: String, addedById: String)
    suspend fun removeRecipeFromCookbook(recipeId: String, cookbookId: String)
    suspend fun addMember(cookbookId: String, userId: String, role: String)
    suspend fun removeMember(cookbookId: String, userId: String)
}
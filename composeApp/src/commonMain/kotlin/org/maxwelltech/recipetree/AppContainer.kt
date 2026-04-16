package org.maxwelltech.recipetree

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.auth.auth
import org.maxwelltech.recipetree.data.firebase.FirebaseCookbookRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseRecipeRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseAuthRepository
import org.maxwelltech.recipetree.data.repository.AuthRepository
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository

object AppContainer {
    private val firestore by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }

    val recipeRepository: RecipeRepository by lazy {
        FirebaseRecipeRepository(firestore)
    }

    val cookbookRepository: CookbookRepository by lazy {
        FirebaseCookbookRepository(firestore)
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth, firestore)
    }

}
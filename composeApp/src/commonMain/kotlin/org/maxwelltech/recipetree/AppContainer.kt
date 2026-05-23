package org.maxwelltech.recipetree

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.storage.storage
import org.maxwelltech.recipetree.data.firebase.FirebaseCommentRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseCookbookRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseInviteRepository
import org.maxwelltech.recipetree.data.firebase.FirebasePhotoStorageRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseRatingRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseRecipeRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseAuthRepository
import org.maxwelltech.recipetree.data.firebase.FirebaseUserProfileRepository
import org.maxwelltech.recipetree.data.repository.AuthRepository
import org.maxwelltech.recipetree.data.repository.CommentRepository
import org.maxwelltech.recipetree.data.repository.CookbookRepository
import org.maxwelltech.recipetree.data.repository.InviteRepository
import org.maxwelltech.recipetree.data.repository.PhotoStorageRepository
import org.maxwelltech.recipetree.data.repository.RatingRepository
import org.maxwelltech.recipetree.data.repository.RecipeRepository
import org.maxwelltech.recipetree.data.repository.UserProfileRepository

object AppContainer {
    private val firestore by lazy { Firebase.firestore }
    private val auth by lazy { Firebase.auth }
    private val storage by lazy { Firebase.storage }

    val recipeRepository: RecipeRepository by lazy {
        FirebaseRecipeRepository(firestore)
    }

    val cookbookRepository: CookbookRepository by lazy {
        FirebaseCookbookRepository(firestore)
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(auth, firestore)
    }

    val inviteRepository: InviteRepository by lazy {
        FirebaseInviteRepository(firestore)
    }

    val ratingRepository: RatingRepository by lazy {
        FirebaseRatingRepository(firestore)
    }

    val commentRepository: CommentRepository by lazy {
        FirebaseCommentRepository(firestore)
    }

    val photoStorageRepository: PhotoStorageRepository by lazy {
        FirebasePhotoStorageRepository(storage)
    }

    val userProfileRepository: UserProfileRepository by lazy {
        FirebaseUserProfileRepository(firestore)
    }

}
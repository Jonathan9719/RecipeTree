package org.maxwelltech.recipetree.data.repository

interface PhotoStorageRepository {

    /**
     * Upload a JPEG-compressed image for [recipeId] and return the public download URL.
     * The caller is expected to have already resized + compressed [bytes] in the platform
     * layer; this repo is dumb about format.
     */
    suspend fun uploadRecipePhoto(recipeId: String, bytes: ByteArray): String

    /**
     * Best-effort delete of the Storage object behind a download URL. Swallows
     * "not found" errors so a stale photoUrls entry doesn't block recipe-doc
     * cleanup — same pattern as deleteCurrentUser's profile-doc delete.
     */
    suspend fun deleteRecipePhoto(downloadUrl: String)
}

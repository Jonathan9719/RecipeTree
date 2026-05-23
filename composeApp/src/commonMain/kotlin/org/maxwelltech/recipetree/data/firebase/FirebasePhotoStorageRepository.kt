package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.storage.Data
import dev.gitlive.firebase.storage.FirebaseStorage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.maxwelltech.recipetree.data.repository.PhotoStorageRepository

/**
 * Bridge that turns a raw ByteArray into the platform-specific `Data` wrapper
 * gitlive's `putData` expects (Android wraps ByteArray, iOS wraps NSData).
 * Hidden inside the data layer so callers stay ByteArray-shaped.
 */
internal expect fun firebaseStorageDataOf(bytes: ByteArray): Data

@OptIn(ExperimentalUuidApi::class)
class FirebasePhotoStorageRepository(
    private val storage: FirebaseStorage
) : PhotoStorageRepository {

    override suspend fun uploadRecipePhoto(recipeId: String, bytes: ByteArray): String {
        val filename = "${Uuid.random()}.jpg"
        val ref = storage.reference.child("recipes/$recipeId/$filename")
        ref.putData(firebaseStorageDataOf(bytes))
        return ref.getDownloadUrl()
    }

    override suspend fun deleteRecipePhoto(downloadUrl: String) {
        try {
            // gitlive exposes getReferenceFromUrl(fullUrl) for both gs:// and
            // https:// download URLs. We deliberately don't error on missing
            // objects (already deleted, garbage URL from before a bucket
            // migration, etc.); the parent recipe doc update is the
            // authoritative source of "is this photo gone?".
            storage.getReferenceFromUrl(downloadUrl).delete()
        } catch (_: Exception) {
            // Swallow — matches the deleteCurrentUser pattern of best-effort
            // cleanup ahead of an authoritative state change (here, the
            // photoUrls list on the recipe doc).
        }
    }
}

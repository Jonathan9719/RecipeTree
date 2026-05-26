package org.maxwelltech.recipetree.data.repository

import org.maxwelltech.recipetree.data.model.Recipe

interface RecipeImportRepository {

    /**
     * Fetch the given URL, find a schema.org/Recipe JSON-LD block in the
     * page, and return a Recipe with title / description / ingredients /
     * steps / servings / photoUrls (the source image URL, temporarily) /
     * sourceUrl populated. Other fields stay at their Recipe() defaults —
     * ownerId / cookbookIds / etc. are filled in at save time, same as a
     * manually-entered new recipe.
     *
     * Throws [IllegalStateException] with a readable message when the page
     * has no JSON-LD or no Recipe-typed object. Throws on network /
     * parse failures; the VM translates through friendlyMessage().
     */
    suspend fun importFromUrl(url: String): Recipe

    /**
     * GET an image URL and return its bytes. Used by the VM to download
     * the source image and re-upload it to Firebase Storage so the
     * recipe owns its photo permanently.
     */
    suspend fun fetchImage(url: String): ByteArray
}

package org.maxwelltech.recipetree.ui.util

import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.model.User

/**
 * Client-side recipe search — matches the design doc's §4.5 stance ("Filter
 * client-side after fetching") for the v1 family-scale data volumes.
 *
 * The text [query] does a case-insensitive substring match across:
 *  - title
 *  - each ingredient.name
 *  - each tag
 *  - the author's displayName, if [authorsById] resolves the recipe's ownerId
 *
 * Description is intentionally excluded — too many incidental matches for the
 * value added. Author display names only work if the caller has resolved them
 * (RecipeListScreen is your-recipes-only so the map can stay empty; cookbook
 * detail screens benefit from passing the resolved owner map).
 *
 * [selectedTag] is a single-select filter — null means "no tag filter". The
 * tag match is exact (case-sensitive) since tags come from a closed vocabulary
 * the user authored.
 *
 * Both filters compose with AND: a recipe must satisfy both to appear.
 */
fun filterRecipes(
    recipes: List<Recipe>,
    query: String,
    selectedTag: String?,
    authorsById: Map<String, User>
): List<Recipe> {
    val q = query.trim().lowercase()
    if (q.isEmpty() && selectedTag == null) return recipes
    return recipes.filter { recipe ->
        (selectedTag == null || selectedTag in recipe.tags) &&
            (q.isEmpty() || matchesQuery(recipe, q, authorsById[recipe.ownerId]))
    }
}

/** Distinct tag list across [recipes], sorted alphabetically for stable chip ordering. */
fun availableTags(recipes: List<Recipe>): List<String> =
    recipes.flatMap { it.tags }.distinct().sorted()

private fun matchesQuery(recipe: Recipe, q: String, author: User?): Boolean {
    if (recipe.title.lowercase().contains(q)) return true
    if (recipe.ingredients.any { it.name.lowercase().contains(q) }) return true
    if (recipe.tags.any { it.lowercase().contains(q) }) return true
    if (author != null && author.displayName.lowercase().contains(q)) return true
    return false
}

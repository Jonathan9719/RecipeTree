package org.maxwelltech.recipetree.data.web

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.maxwelltech.recipetree.data.model.Ingredient
import org.maxwelltech.recipetree.data.model.Recipe
import org.maxwelltech.recipetree.data.repository.RecipeImportRepository

/**
 * Fetches a recipe page over ktor, finds the first schema.org/Recipe
 * JSON-LD block, and maps it into a Recipe. Pure client-side — no API
 * keys, no backend. Works on the major recipe sites (NYT Cooking,
 * AllRecipes, Bon Appétit, Serious Eats, Food Network, King Arthur,
 * BBC Good Food etc.) because Google requires schema.org markup for
 * rich snippets, so almost every recipe-shaped page has it.
 */
class KtorRecipeImportRepository : RecipeImportRepository {

    private val client = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun importFromUrl(url: String): Recipe {
        val html = client.get(url) {
            // Some recipe sites (NYT Cooking notably) block default ktor UA.
            // A current desktop Chrome UA gets through everywhere we tested.
            header(HttpHeaders.UserAgent, DESKTOP_UA)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml")
        }.bodyAsText()

        val recipeNode = extractRecipeNode(html)
            ?: throw IllegalStateException(
                "Couldn't find recipe data on this page. Try a different link or enter it manually."
            )

        val title = recipeNode["name"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: throw IllegalStateException(
                "The page had recipe data but no title. Try a different link or enter it manually."
            )

        return Recipe(
            title = title,
            description = recipeNode["description"]?.jsonPrimitive?.contentOrNull?.trim(),
            ingredients = extractIngredients(recipeNode["recipeIngredient"]),
            steps = extractSteps(recipeNode["recipeInstructions"]),
            servings = extractServings(recipeNode["recipeYield"]),
            photoUrls = extractImageUrl(recipeNode["image"])?.let { listOf(it) } ?: emptyList(),
            sourceUrl = url
        )
    }

    override suspend fun fetchImage(url: String): ByteArray {
        return client.get(url) {
            header(HttpHeaders.UserAgent, DESKTOP_UA)
            header(HttpHeaders.Accept, "image/*")
        }.readRawBytes()
    }

    // --- JSON-LD extraction ---------------------------------------------------

    /**
     * Walks every <script type="application/ld+json"> block in the HTML,
     * parses each as JSON, and recursively searches for an object whose
     * @type is (or includes) "Recipe". Returns the first match, or null
     * if no Recipe-shaped node exists on the page.
     */
    private fun extractRecipeNode(html: String): JsonObject? {
        for (match in JSON_LD_BLOCK.findAll(html)) {
            val raw = match.groupValues[1].trim().ifEmpty { continue }
            val parsed = runCatching { json.parseToJsonElement(raw) }.getOrNull() ?: continue
            val found = findRecipeNode(parsed)
            if (found != null) return found
        }
        return null
    }

    private fun findRecipeNode(element: JsonElement): JsonObject? {
        when (element) {
            is JsonObject -> {
                if (matchesRecipeType(element["@type"])) return element
                for ((_, value) in element) {
                    val nested = findRecipeNode(value)
                    if (nested != null) return nested
                }
            }
            is JsonArray -> {
                for (item in element) {
                    val nested = findRecipeNode(item)
                    if (nested != null) return nested
                }
            }
            else -> {} // Primitives can't contain Recipe nodes.
        }
        return null
    }

    private fun matchesRecipeType(typeNode: JsonElement?): Boolean {
        return when (typeNode) {
            is JsonPrimitive -> typeNode.contentOrNull == "Recipe"
            is JsonArray -> typeNode.any {
                it is JsonPrimitive && it.contentOrNull == "Recipe"
            }
            else -> false
        }
    }

    // --- Field mappers --------------------------------------------------------

    private fun extractIngredients(node: JsonElement?): List<Ingredient> {
        val list = (node as? JsonArray) ?: return emptyList()
        return list.mapNotNull { item ->
            val raw = (item as? JsonPrimitive)?.contentOrNull?.trim()
            if (raw.isNullOrBlank()) null else parseIngredient(raw)
        }
    }

    /**
     * recipeInstructions can come in three common shapes:
     *  - List<String>: each is a step.
     *  - List<HowToStep>: each object has a `text` property.
     *  - List<HowToSection>: each object has an `itemListElement` that's a
     *    List<HowToStep>; we flatten and drop the section name.
     */
    private fun extractSteps(node: JsonElement?): List<String> {
        val list = (node as? JsonArray) ?: return emptyList()
        return list.flatMap { item ->
            when (item) {
                is JsonPrimitive -> listOfNotNull(item.contentOrNull?.trim()?.ifBlank { null })
                is JsonObject -> {
                    val type = (item["@type"] as? JsonPrimitive)?.contentOrNull
                    when (type) {
                        "HowToSection" -> extractSteps(item["itemListElement"])
                        else -> listOfNotNull(
                            (item["text"] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }
                                ?: (item["name"] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }
                        )
                    }
                }
                else -> emptyList()
            }
        }
    }

    private fun extractImageUrl(node: JsonElement?): String? {
        return when (node) {
            is JsonPrimitive -> node.contentOrNull?.trim()?.ifBlank { null }
            is JsonArray -> node.firstNotNullOfOrNull { extractImageUrl(it) }
            is JsonObject -> (node["url"] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }
            else -> null
        }
    }

    /**
     * recipeYield can be an Int, a numeric string ("4"), or a freeform
     * string ("4 servings" / "Makes 12 cookies"). Pull the first integer
     * we can find; fall back to Recipe()'s default 4 if nothing matches.
     */
    private fun extractServings(node: JsonElement?): Int {
        val raw = when (node) {
            is JsonPrimitive -> node.intOrNull?.toString() ?: node.contentOrNull
            is JsonArray -> (node.firstOrNull() as? JsonPrimitive)?.contentOrNull
            else -> null
        }?.trim().orEmpty()
        if (raw.isEmpty()) return DEFAULT_SERVINGS
        return SERVINGS_NUMBER.find(raw)?.value?.toIntOrNull() ?: DEFAULT_SERVINGS
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private const val REQUEST_TIMEOUT_MS = 20_000L
        private const val DEFAULT_SERVINGS = 4

        // Recent desktop Chrome UA. NYT Cooking specifically blocks empty /
        // ktor-default UAs; this gets through every site we tested against.
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/121.0.0.0 Safari/537.36"

        private val JSON_LD_BLOCK = Regex(
            """<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

        private val SERVINGS_NUMBER = Regex("""\d+""")
    }
}

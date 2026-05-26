package org.maxwelltech.recipetree.ui.util

/**
 * Pick a placeholder emoji for a cookbook that has no cover photo. Stable per
 * id — the same cookbook always shows the same icon across launches and
 * across screens (list card + detail hero use the same call, so they always
 * match). Empty-id fallback to the first emoji covers the brief window
 * before a brand-new cookbook gets its Firestore-generated id.
 *
 * Trim, reorder, or add to [COOKBOOK_PLACEHOLDER_EMOJIS] to taste — it's
 * the single source of truth.
 */
fun cookbookEmojiFor(cookbookId: String): String {
    if (cookbookId.isEmpty()) return COOKBOOK_PLACEHOLDER_EMOJIS.first()
    // String.hashCode is deterministic per content across runs. The
    // `(x mod n + n) mod n` dance handles hashCode's possible negative
    // values without reaching for kotlin.math.floorMod (which has its own
    // KMP quirks).
    val idx = (cookbookId.hashCode().mod(COOKBOOK_PLACEHOLDER_EMOJIS.size) +
        COOKBOOK_PLACEHOLDER_EMOJIS.size) % COOKBOOK_PLACEHOLDER_EMOJIS.size
    return COOKBOOK_PLACEHOLDER_EMOJIS[idx]
}

private val COOKBOOK_PLACEHOLDER_EMOJIS = listOf(
    "📖", "📗", "📘", "📕", "📙",  // colored books
    "🥧", "🍰", "🍞", "🥘", "🍲"   // kitchen
)

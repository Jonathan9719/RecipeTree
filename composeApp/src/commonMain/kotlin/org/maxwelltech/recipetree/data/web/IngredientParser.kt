package org.maxwelltech.recipetree.data.web

import org.maxwelltech.recipetree.data.model.Ingredient

/**
 * Parse a raw schema.org ingredient string ("1 1/2 cups all-purpose flour")
 * into the structured [Ingredient] data class. Best-effort:
 *
 *  - Amount: optional leading mixed-number / fraction / integer / decimal,
 *    with Unicode fractions (½ ⅓ ¼ ¾ ⅛ etc.) normalized to ASCII.
 *  - Unit: optional single token from a known-units set (case-insensitive,
 *    plural-tolerant, trailing punctuation tolerated).
 *  - Name: whatever's left after the optional amount and unit.
 *
 * Falls back to "whole string → name, amount/unit empty" when the regex
 * doesn't match. That graceful failure matches manual-entry behavior, and
 * the detail-screen composer renders it the same way either way.
 */
fun parseIngredient(raw: String): Ingredient {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return Ingredient()

    val normalized = normalizeFractions(trimmed)

    // Match the leading amount: a mixed number ("1 1/2"), a plain fraction
    // ("1/2"), a decimal ("1.5"), or a bare integer ("3"). The order in the
    // alternation matters — mixed numbers need to win over the bare integer
    // that's the first token.
    val amountMatch = AMOUNT_REGEX.find(normalized)
    val amount = amountMatch?.value?.trim().orEmpty()
    val afterAmount = if (amountMatch != null) {
        normalized.substring(amountMatch.range.last + 1).trim()
    } else {
        normalized
    }

    // Match the unit: first whitespace-separated token, lowercased and
    // stripped of trailing punctuation, must be in the known-units set.
    val firstToken = afterAmount.substringBefore(' ').trim()
    val cleanToken = firstToken.lowercase().trimEnd('.', ',', ')')
    val unit: String
    val afterUnit: String
    if (cleanToken.isNotEmpty() && cleanToken in UNITS) {
        unit = firstToken.trimEnd('.', ',', ')')
        afterUnit = afterAmount.substringAfter(' ', missingDelimiterValue = "").trim()
    } else {
        unit = ""
        afterUnit = afterAmount
    }

    // If nothing structural was matched, whole string is name.
    return if (amount.isBlank() && unit.isBlank()) {
        Ingredient(name = trimmed)
    } else {
        Ingredient(name = afterUnit, amount = amount, unit = unit)
    }
}

private val AMOUNT_REGEX = Regex(
    """^\s*(?:\d+\s+\d+/\d+|\d+/\d+|\d+\.\d+|\d+)"""
)

private val FRACTION_MAP = mapOf(
    "½" to "1/2",
    "⅓" to "1/3", "⅔" to "2/3",
    "¼" to "1/4", "¾" to "3/4",
    "⅕" to "1/5", "⅖" to "2/5", "⅗" to "3/5", "⅘" to "4/5",
    "⅙" to "1/6", "⅚" to "5/6",
    "⅛" to "1/8", "⅜" to "3/8", "⅝" to "5/8", "⅞" to "7/8"
)

private fun normalizeFractions(s: String): String {
    var out = s
    for ((unicode, ascii) in FRACTION_MAP) {
        if (unicode in out) {
            // Insert a space before the ASCII form so "2½" becomes "2 1/2"
            // and matches the mixed-number regex.
            out = out.replace(unicode, " $ascii").trim()
        }
    }
    return out
}

private val UNITS: Set<String> = setOf(
    // Volume — US
    "cup", "cups", "c",
    "tablespoon", "tablespoons", "tbsp", "tbsps", "tbs", "tb",
    "teaspoon", "teaspoons", "tsp", "tsps",
    "fluid ounce", "fluid ounces", "fl oz", "floz",
    "pint", "pints", "pt", "pts",
    "quart", "quarts", "qt", "qts",
    "gallon", "gallons", "gal",
    // Weight
    "ounce", "ounces", "oz",
    "pound", "pounds", "lb", "lbs",
    "gram", "grams", "g",
    "kilogram", "kilograms", "kg",
    // Volume — metric
    "milliliter", "milliliters", "ml",
    "liter", "liters", "l",
    // Counts / informal
    "pinch", "pinches",
    "dash", "dashes",
    "clove", "cloves",
    "sprig", "sprigs",
    "stalk", "stalks",
    "can", "cans",
    "package", "packages", "pkg",
    "slice", "slices",
    "stick", "sticks",
    "head", "heads",
    "bunch", "bunches",
    "piece", "pieces"
)

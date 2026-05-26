package org.maxwelltech.recipetree.ui.util

/**
 * Translate a Throwable into family-friendly text. Used by every ViewModel
 * that catches an exception and surfaces a message to a Compose screen.
 *
 * Three layers of decision:
 *
 *   1. **Known jargony patterns get rewritten.** Firestore PERMISSION_DENIED,
 *      UNAVAILABLE / network errors, RESOURCE_EXHAUSTED / quota etc. — the
 *      raw message is full of UPPER_SNAKE_CASE codes and is useless to a
 *      family member. We substitute a plain-language equivalent.
 *
 *   2. **Domain-throw messages pass through.** When a repository throws
 *      something readable like `IllegalStateException("Invite was revoked")`
 *      we want THAT to reach the UI, not the caller's generic fallback. The
 *      heuristic [looksLikeFirebaseJargon] separates "Invite was revoked"
 *      from "FirebaseFirestoreException: NOT_FOUND" — if the message has
 *      SNAKE_CASE codes or "Exception" / "com." in it, it's jargon.
 *
 *   3. **Fallback is the caller's verb-phrase.** A domain-specific string
 *      like "Couldn't load your cookbooks." — used when there's no message
 *      at all, or when the message is jargony but doesn't match a known
 *      pattern.
 *
 * AuthViewModel and SettingsViewModel intentionally don't call into this —
 * they have their own friendlyError that maps Firebase Auth specifics
 * (wrong password, weak password, email-already-in-use, etc.) and those
 * mappings would be lost if rerouted through here.
 */
fun friendlyMessage(throwable: Throwable?, fallback: String): String {
    val raw = throwable?.message ?: return fallback
    val lower = raw.lowercase()

    return when {
        // Rule denial — both the Firestore status code and the Firebase
        // Storage human-form ("user does not have permission to access this
        // object") show up here.
        "permission_denied" in lower ||
            "permission-denied" in lower ||
            "does not have permission" in lower -> "You don't have permission to do that."

        // Connectivity. The wrapper spells this differently across platforms,
        // so the OR-chain catches the common variants.
        "unavailable" in lower ||
            "could not reach" in lower ||
            ("network" in lower && "error" in lower) -> "Couldn't reach the server. Check your connection and try again."

        // Quota / rate-limit. Rare at family scale, but a "try later" message
        // is still better than the raw status code.
        "quota" in lower ||
            "resource_exhausted" in lower ||
            "too-many-requests" in lower -> "We're being rate limited. Please try again in a moment."

        // No known jargon match. Use the raw message if it looks like
        // something a human wrote (e.g. an IllegalStateException from a
        // repository), otherwise fall back to the caller's domain phrase.
        looksLikeFirebaseJargon(raw) -> fallback
        else -> raw
    }
}

private val SNAKE_CASE_CODE = Regex("[A-Z]{2,}_[A-Z]+")

private fun looksLikeFirebaseJargon(message: String): Boolean {
    // SNAKE_CASE error codes (NOT_FOUND, FAILED_PRECONDITION,
    // ERROR_WRONG_PASSWORD, etc.), exception-class names, package-path
    // qualifiers — none of these are appropriate to surface verbatim.
    return SNAKE_CASE_CODE.containsMatchIn(message) ||
        "Exception" in message ||
        message.startsWith("com.") ||
        message.startsWith("java.") ||
        message.startsWith("kotlin.")
}

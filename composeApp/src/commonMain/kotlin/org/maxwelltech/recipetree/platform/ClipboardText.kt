package org.maxwelltech.recipetree.platform

import androidx.compose.ui.platform.ClipEntry

/**
 * Build a ClipEntry that carries plain text. ClipEntry is `expect class` with
 * platform-specific constructors — Android wants a ClipData, iOS uses an
 * internal companion factory — so a tiny expect/actual is the cleanest path
 * from a shared screen.
 */
internal expect fun textClipEntry(text: String): ClipEntry

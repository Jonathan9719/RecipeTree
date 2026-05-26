package org.maxwelltech.recipetree.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun textClipEntry(text: String): ClipEntry =
    ClipEntry.withPlainText(text)

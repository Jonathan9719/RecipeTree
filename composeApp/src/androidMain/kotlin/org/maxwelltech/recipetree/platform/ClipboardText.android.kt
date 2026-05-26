package org.maxwelltech.recipetree.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

internal actual fun textClipEntry(text: String): ClipEntry =
    ClipEntry(ClipData.newPlainText("Recipe Tree", text))

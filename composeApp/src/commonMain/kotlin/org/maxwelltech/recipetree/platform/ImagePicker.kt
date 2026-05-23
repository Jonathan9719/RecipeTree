package org.maxwelltech.recipetree.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific gallery picker. Constructed via [rememberPhotoPicker]; call
 * [launch] from a button onClick to open the system picker. The selected image
 * is resized to ~1600px on its long edge and re-encoded as JPEG quality 80
 * before being handed to [rememberPhotoPicker]'s onPhotoPicked callback —
 * keeping uploads in the 300–500KB range instead of the 5–12MB original.
 *
 * Cancellation by the user is silent (no callback fires).
 */
expect class PhotoPickerLauncher {
    fun launch()
}

/**
 * Composable factory for [PhotoPickerLauncher]. The Android impl wraps an
 * ActivityResultLauncher; the iOS impl captures a closure that presents
 * PHPickerViewController from the key window's root view controller.
 */
@Composable
expect fun rememberPhotoPicker(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher

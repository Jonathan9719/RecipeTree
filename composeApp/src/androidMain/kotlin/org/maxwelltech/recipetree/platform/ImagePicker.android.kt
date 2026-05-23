package org.maxwelltech.recipetree.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual class PhotoPickerLauncher internal constructor(
    private val launcher: () -> Unit
) {
    actual fun launch() = launcher.invoke()
}

@Composable
actual fun rememberPhotoPicker(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Capture the latest callback so a recomposition mid-pick doesn't fire a
    // stale closure when the activity result returns.
    val callback by rememberUpdatedState(onPhotoPicked)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { compressImage(context, uri) }
                    .onSuccess { callback(it) }
                // Failures swallowed for now — the screen-level error state is
                // driven off the upload itself, not the decode. A torn JPEG /
                // unreadable URI is rare enough that the user retrying is the
                // simplest recovery.
            }
        }
    }

    return remember(launcher) {
        PhotoPickerLauncher {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }
}

/**
 * Bitmap decode + resize + JPEG encode. Runs on Dispatchers.IO because
 * BitmapFactory.decodeStream can be hundreds of ms for a multi-MB phone photo
 * and we don't want to jank the picker dismissal frame.
 */
private suspend fun compressImage(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
        val original = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Could not decode image at $uri")

        val resized = resizeToMaxEdge(original, MAX_EDGE_PX)
        // recycle only when we got a new bitmap back — createScaledBitmap can
        // return the same instance when no scaling is needed.
        if (resized !== original) original.recycle()

        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        resized.recycle()
        out.toByteArray()
    }

private fun resizeToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    val longEdge = maxOf(w, h)
    if (longEdge <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longEdge
    return Bitmap.createScaledBitmap(
        bitmap,
        (w * scale).toInt().coerceAtLeast(1),
        (h * scale).toInt().coerceAtLeast(1),
        /* filter = */ true
    )
}

private const val MAX_EDGE_PX = 1600
private const val JPEG_QUALITY = 80

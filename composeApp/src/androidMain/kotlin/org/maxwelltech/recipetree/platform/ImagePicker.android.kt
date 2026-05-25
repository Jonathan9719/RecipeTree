package org.maxwelltech.recipetree.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
import androidx.exifinterface.media.ExifInterface
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
 * Read the JPEG's EXIF orientation tag, decode the bitmap, rotate it to upright,
 * resize to MAX_EDGE_PX, and encode as JPEG quality 80. Runs on Dispatchers.IO
 * because BitmapFactory.decodeStream can be hundreds of ms for a multi-MB phone
 * photo and we don't want to jank the picker dismissal frame.
 *
 * Phone cameras almost always store landscape pixel data plus an EXIF
 * Orientation tag ("rotate 90° CW to display correctly"). BitmapFactory ignores
 * that tag, so without this rotation step every photo taken in portrait would
 * upload sideways.
 */
private suspend fun compressImage(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
        val orientation = readExifOrientation(context, uri)

        val decoded = context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Could not decode image at $uri")

        val upright = applyExifOrientation(decoded, orientation)
        val resized = resizeToMaxEdge(upright, MAX_EDGE_PX)
        // recycle intermediates only when a new instance was returned —
        // applyExifOrientation/resizeToMaxEdge can pass the input through
        // unchanged when no work is needed, and recycling that would crash
        // the next consumer.
        if (resized !== upright) upright.recycle()
        if (upright !== decoded) decoded.recycle()

        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        resized.recycle()
        out.toByteArray()
    }

private fun readExifOrientation(context: Context, uri: Uri): Int {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        ExifInterface(input).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL
}

private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, /* filter = */ true)
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

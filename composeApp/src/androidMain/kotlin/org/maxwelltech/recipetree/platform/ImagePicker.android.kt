package org.maxwelltech.recipetree.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
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
 * Decode the URI to an upright Bitmap, resize to MAX_EDGE_PX, and JPEG-encode.
 * Runs on Dispatchers.IO because decode can be hundreds of ms for a multi-MB
 * phone photo.
 *
 * Orientation handling: ImageDecoder (API 28+) reads EXIF through the
 * platform image pipeline and bakes the rotation onto the bitmap as part of
 * decode — far more reliable than our older ExifInterface-on-the-stream path,
 * which the Android 13+ Photo Picker URI scheme broke for some image
 * providers. ExifInterface remains the fallback for API 24-27.
 */
private suspend fun compressImage(context: Context, uri: Uri): ByteArray =
    withContext(Dispatchers.IO) {
        val upright = decodeUprightBitmap(context, uri)
        val resized = resizeToMaxEdge(upright, MAX_EDGE_PX)
        if (resized !== upright) upright.recycle()

        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        resized.recycle()
        out.toByteArray()
    }

private fun decodeUprightBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            // ALLOCATOR_SOFTWARE is required for the downstream
            // Bitmap.compress(JPEG, ...) call — hardware-allocated bitmaps
            // can't be re-encoded. ImageDecoder applies EXIF orientation
            // automatically (TARGET_COLOR_SPACE_DEFAULT covers that path).
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        decodeUprightLegacy(context, uri)
    }
}

private fun decodeUprightLegacy(context: Context, uri: Uri): Bitmap {
    val orientation = readExifOrientation(context, uri)
    val decoded = context.contentResolver.openInputStream(uri).use { input ->
        BitmapFactory.decodeStream(input)
    } ?: error("Could not decode image at $uri")
    val rotated = applyExifOrientation(decoded, orientation)
    if (rotated !== decoded) decoded.recycle()
    return rotated
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

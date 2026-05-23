package org.maxwelltech.recipetree.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual class PhotoPickerLauncher internal constructor(
    private val present: () -> Unit,
    // Delegate retained for the lifetime of the launcher. Kotlin/Native's ARC
    // bridge would otherwise drop the delegate when the present() closure
    // returns, and the picker's didFinishPicking callback would land on a
    // deallocated object.
    @Suppress("unused") private val delegate: NSObject
) {
    actual fun launch() = present()
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPhotoPicker(
    onPhotoPicked: (ByteArray) -> Unit
): PhotoPickerLauncher {
    val scope = rememberCoroutineScope()
    val callback by rememberUpdatedState(onPhotoPicked)

    return remember {
        val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(
                picker: PHPickerViewController,
                didFinishPicking: List<*>
            ) {
                picker.dismissViewControllerAnimated(true, null)
                val first = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                // Skipping loadObjectOfClass(UIImage, ...) — K/N's binding
                // for Class<NSItemProviderReading> is awkward to satisfy.
                // We need JPEG bytes either way, so go through the data
                // representation: NSData → UIImage → resize → JPEG bytes.
                first.itemProvider.loadDataRepresentationForTypeIdentifier(
                    typeIdentifier = "public.image"
                ) { data, _ ->
                    val nsData = data ?: return@loadDataRepresentationForTypeIdentifier
                    scope.launch {
                        val bytes = withContext(Dispatchers.Default) {
                            val image = UIImage.imageWithData(nsData) ?: return@withContext null
                            compressImage(image)
                        } ?: return@launch
                        callback(bytes)
                    }
                }
            }
        }

        val present: () -> Unit = {
            val config = PHPickerConfiguration().apply {
                selectionLimit = 1
                filter = PHPickerFilter.imagesFilter
            }
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            root?.presentViewController(picker, animated = true, completion = null)
        }

        PhotoPickerLauncher(present, delegate)
    }
}

/**
 * Resize a UIImage to MAX_EDGE_PX on its long edge and re-encode as JPEG
 * quality 0.8. Returns null if the source has no pixels or jpeg encoding fails.
 */
@OptIn(ExperimentalForeignApi::class)
private fun compressImage(image: UIImage): ByteArray? {
    val srcW = image.size.useContents { width }
    val srcH = image.size.useContents { height }
    if (srcW <= 0.0 || srcH <= 0.0) return null

    val longEdge = if (srcW > srcH) srcW else srcH
    val target: UIImage = if (longEdge <= MAX_EDGE_PX) {
        image
    } else {
        val scale = MAX_EDGE_PX / longEdge
        val newSize = CGSizeMake(srcW * scale, srcH * scale)
        val newW = newSize.useContents { width }
        val newH = newSize.useContents { height }
        val renderer = UIGraphicsImageRenderer(size = newSize)
        renderer.imageWithActions { _ ->
            image.drawInRect(CGRectMake(0.0, 0.0, newW, newH))
        }
    }

    val jpegData: NSData = UIImageJPEGRepresentation(target, JPEG_QUALITY) ?: return null
    return jpegData.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = this.length.toInt()
    if (len == 0) return ByteArray(0)
    return ByteArray(len).also { out ->
        out.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}

private const val MAX_EDGE_PX: CGFloat = 1600.0
private const val JPEG_QUALITY: Double = 0.8

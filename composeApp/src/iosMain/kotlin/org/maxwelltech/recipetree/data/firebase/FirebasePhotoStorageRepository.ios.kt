package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual fun firebaseStorageDataOf(bytes: ByteArray): Data {
    // Pin the byte array so its address stays stable for the NSData copy.
    // NSData.create(bytes:length:) does the copy itself, so it's safe to
    // unpin immediately after the constructor returns.
    val nsData = bytes.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
    }
    return Data(nsData)
}

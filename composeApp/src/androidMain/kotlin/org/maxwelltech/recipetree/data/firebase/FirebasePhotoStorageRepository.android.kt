package org.maxwelltech.recipetree.data.firebase

import dev.gitlive.firebase.storage.Data

internal actual fun firebaseStorageDataOf(bytes: ByteArray): Data = Data(bytes)

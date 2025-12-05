package io.novafoundation.nova.feature_account_impl.domain.utils

import io.novasama.substrate_sdk_android.extensions.fromHex

fun String.isSubstrateSeed(): Boolean {
    return fromHex().isSubstrateSeed()
}

fun ByteArray.isSubstrateSeed(): Boolean {
    return size == 32
}

fun String.isSubstrateKeypair(): Boolean {
    return fromHex().isSubstrateKeypair()
}

fun ByteArray.isSubstrateKeypair(): Boolean {
    return size == 64
}

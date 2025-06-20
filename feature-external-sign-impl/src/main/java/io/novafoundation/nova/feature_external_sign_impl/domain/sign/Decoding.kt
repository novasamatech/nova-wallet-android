package io.novafoundation.nova.feature_external_sign_impl.domain.sign

import io.novasama.substrate_sdk_android.extensions.fromHex

fun String.tryConvertHexToUtf8(): String {
    return runCatching { fromHex().decodeToString(throwOnInvalidSequence = true) }
        .getOrDefault(this)
}

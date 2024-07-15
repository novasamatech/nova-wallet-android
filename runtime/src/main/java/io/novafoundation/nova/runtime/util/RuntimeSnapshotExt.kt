package io.novafoundation.nova.runtime.util

import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.skipAliases

fun RuntimeSnapshot.isEthereumAddress(): Boolean {
    val addressType = typeRegistry["Address"]!!.skipAliases()!!

    return addressType is FixedByteArray && addressType.length == 20
}

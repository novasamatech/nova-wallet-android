package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.derivative

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromEntropy
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.definitions.types.useScaleWriter
import io.novasama.substrate_sdk_android.scale.utils.directWrite

private val PREFIX = "modlpy/utilisuba".encodeToByteArray()

fun generateDerivativeAddress(
    parent: AccountIdKey,
    index: Int
): AccountIdKey {
    val accountIdSize = parent.value.size

    val entropy = useScaleWriter {
        directWrite(PREFIX)

        directWrite(parent.value)

        writeUint16(index)
    }.blake2b256()

    return AccountIdKey.fromEntropy(entropy, accountIdSize)
}

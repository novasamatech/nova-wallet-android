package io.novafoundation.nova.runtime.extrinsic.visitor.extrinsic.impl.nodes.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.compareTo
import io.novafoundation.nova.common.utils.padEnd
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import io.novasama.substrate_sdk_android.runtime.definitions.types.useScaleWriter
import io.novasama.substrate_sdk_android.scale.utils.directWrite

private val PREFIX = "modlpy/utilisuba".encodeToByteArray()

fun generateMultisigAddress(
    signatory: AccountIdKey,
    otherSignatories: List<AccountIdKey>,
    threshold: Int
) = generateMultisigAddress(otherSignatories + signatory, threshold)

fun generateMultisigAddress(
    signatories: List<AccountIdKey>,
    threshold: Int
): AccountIdKey {
    val accountIdSize = signatories.first().value.size

    val sortedAccounts = signatories.sortedWith { a, b -> a.value.compareTo(b.value, unsigned = true) }

    val entropy = useScaleWriter {
        directWrite(PREFIX)

        writeCompact(sortedAccounts.size)
        sortedAccounts.forEach {
            directWrite(it.value)
        }

        writeUint16(threshold)
    }.blake2b256()

    val result = when {
        entropy.size == accountIdSize -> entropy
        entropy.size < accountIdSize -> entropy.padEnd(accountIdSize, 0)
        else -> entropy.copyOf(accountIdSize)
    }

    return result.intoKey()
}

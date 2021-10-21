package io.novafoundation.nova.runtime.ext

import io.novafoundation.nova.common.data.network.runtime.binding.MultiAddress
import io.novafoundation.nova.common.utils.ethereumAddressFromPublicKey
import io.novafoundation.nova.common.utils.ethereumAddressToHex
import io.novafoundation.nova.common.utils.formatNamed
import io.novafoundation.nova.common.utils.substrateAccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ExplorerTemplateExtractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.TypesUsage
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.addressByte
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress

val Chain.typesUsage: TypesUsage
    get() = when {
        types == null -> TypesUsage.BASE
        types.overridesCommon -> TypesUsage.OWN
        else -> TypesUsage.BOTH
    }

val Chain.utilityAsset
    get() = assets.first(Chain.Asset::isUtilityAsset)

val Chain.Asset.isUtilityAsset: Boolean
    get() = id == 0

val Chain.genesisHash: String
    get() = id

fun Chain.addressOf(accountId: ByteArray): String {
    return if (isEthereumBased) {
        accountId.ethereumAddressToHex()
    } else {
        accountId.toAddress(addressPrefix.toByte())
    }
}

fun Chain.accountIdOf(address: String): ByteArray {
    return if (isEthereumBased) {
        address.fromHex()
    } else {
        address.toAccountId()
    }
}

fun Chain.accountIdOf(publicKey: ByteArray): ByteArray {
    return if (isEthereumBased) {
        publicKey.ethereumAddressFromPublicKey()
    } else {
        publicKey.substrateAccountId()
    }
}

fun Chain.hexAccountIdOf(address: String): String {
    return accountIdOf(address).toHexString()
}

fun Chain.multiAddressOf(accountId: ByteArray): MultiAddress {
    return if (isEthereumBased) {
        MultiAddress.Address20(accountId)
    } else {
        MultiAddress.Id(accountId)
    }
}

fun Chain.addressFromPublicKey(publicKey: ByteArray): String {
    return if (isEthereumBased) {
        publicKey.ethereumAddressFromPublicKey().ethereumAddressToHex()
    } else {
        publicKey.toAddress(addressPrefix.toByte())
    }
}

fun Chain.accountIdFromPublicKey(publicKey: ByteArray): ByteArray {
    return if (isEthereumBased) {
        publicKey.ethereumAddressFromPublicKey()
    } else {
        publicKey.substrateAccountId()
    }
}

val Chain.historySupported: Boolean
    get() {
        val historyType = externalApi?.history?.type ?: return false

        return historyType != Chain.ExternalApi.Section.Type.UNKNOWN
    }

fun Chain.isValidAddress(address: String): Boolean {
    return runCatching {
        if (isEthereumBased) {
            address.fromHex().size == 20
        } else {
            address.addressByte() == addressPrefix.toByte()
        }
    }.getOrDefault(false)
}

fun Chain.multiAddressOf(address: String): MultiAddress = multiAddressOf(accountIdOf(address))

fun Chain.availableExplorersFor(field: ExplorerTemplateExtractor) = explorers.filter { field(it) != null }

fun Chain.Explorer.accountUrlOf(address: String): String {
    return format(Chain.Explorer::account, "address", address)
}

fun Chain.Explorer.extrinsicUrlOf(extrinsicHash: String): String {
    return format(Chain.Explorer::extrinsic, "hash", extrinsicHash)
}

fun Chain.Explorer.eventUrlOf(eventId: String): String {
    return format(Chain.Explorer::event, "event", eventId)
}

private inline fun Chain.Explorer.format(
    templateExtractor: ExplorerTemplateExtractor,
    argumentName: String,
    argumentValue: String,
): String {
    val template = templateExtractor(this) ?: throw Exception("Cannot find template in the chain explorer: $name")

    return template.formatNamed(argumentName to argumentValue)
}

object ChainGeneses {

    const val KUSAMA = "b0a8d493285c2df73290dfb7e61f870f17b41801197a149ca93654499ea3dafe"
    const val POLKADOT = "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3"
    const val ROCOCO_ACALA = "52f04247aeababaab46c262a94819b174b847dd403400145c0522c7f17844c68"
}

val Chain.Companion.Geneses
    get() = ChainGeneses

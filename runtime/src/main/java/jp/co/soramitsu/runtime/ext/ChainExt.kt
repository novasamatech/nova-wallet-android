package jp.co.soramitsu.runtime.ext

import jp.co.soramitsu.common.data.network.runtime.binding.MultiAddress
import jp.co.soramitsu.common.utils.ethereumAddressFromPublicKey
import jp.co.soramitsu.common.utils.ethereumAddressToHex
import jp.co.soramitsu.common.utils.formatNamed
import jp.co.soramitsu.common.utils.substrateAccountId
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.addressByte
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAccountId
import jp.co.soramitsu.fearless_utils.ss58.SS58Encoder.toAddress
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ExplorerTemplateExtractor
import jp.co.soramitsu.runtime.multiNetwork.chain.model.TypesUsage

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
    argumentValue: String
): String {
    val template = templateExtractor(this) ?: throw Exception("Cannot find template in the chain explorer: $name")

    return template.formatNamed(argumentName to argumentValue)
}

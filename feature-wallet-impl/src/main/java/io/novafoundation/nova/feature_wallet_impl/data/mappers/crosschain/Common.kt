package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.JunctionsRemote
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.toInterior
import io.novasama.substrate_sdk_android.extensions.fromHex

private const val PARENTS = "parents"
private const val ACCOUNT_KEY_20_SIZE = 20

fun mapJunctionsRemoteToMultiLocation(
    junctionsRemote: JunctionsRemote
): RelativeMultiLocation {
    return if (PARENTS in junctionsRemote) {
        val parents = junctionsRemote.getValue(PARENTS).asGsonParsedNumber().toInt()
        val withoutParents = junctionsRemote - PARENTS

        RelativeMultiLocation(
            parents = parents,
            interior = mapJunctionsRemoteToInterior(withoutParents)
        )
    } else {
        RelativeMultiLocation(
            parents = 0,
            interior = mapJunctionsRemoteToInterior(junctionsRemote)
        )
    }
}

fun JunctionsRemote.toAbsoluteLocation(): AbsoluteMultiLocation {
    return AbsoluteMultiLocation(mapJunctionsRemoteToInterior(this))
}

private fun mapJunctionsRemoteToInterior(
    junctionsRemote: JunctionsRemote
): MultiLocation.Interior {
    return junctionsRemote.map { (type, value) -> mapJunctionFromRemote(type, value) }
        .toInterior()
}

fun mapJunctionFromRemote(type: String, value: Any?): Junction {
    return when (type) {
        "parachainId" -> Junction.ParachainId(value.asGsonParsedNumber())
        "generalKey" -> Junction.GeneralKey(value as String)
        "palletInstance" -> Junction.PalletInstance(value.asGsonParsedNumber())
        "generalIndex" -> Junction.GeneralIndex(value.asGsonParsedNumber())
        "globalConsensus" -> Junction.GlobalConsensus(mapNetworkIdFromRemote(value))
        "accountKey20" -> Junction.AccountKey20(mapAccountKey20FromRemote(value))
        else -> throw IllegalArgumentException("Unknown junction type: $type")
    }
}

// Config shape: {"ethereum": {"chainId": 1}}. Only Ethereum is supported
private fun mapNetworkIdFromRemote(value: Any?): MultiLocation.NetworkId {
    val consensus = value as? Map<*, *> ?: throw IllegalArgumentException("globalConsensus must be an object: $value")
    val ethereum = consensus["ethereum"] as? Map<*, *> ?: throw IllegalArgumentException("Unsupported globalConsensus: $value")

    return MultiLocation.NetworkId.Ethereum(chainId = ethereum["chainId"].asGsonParsedNumber().toInt())
}

// Config shape: "0x" + 20 bytes hex. A wrong size would silently encode as a different contract address downstream
private fun mapAccountKey20FromRemote(value: Any?): AccountIdKey {
    val hex = value as? String ?: throw IllegalArgumentException("accountKey20 must be a hex string: $value")
    val bytes = runCatching { hex.fromHex() }
        .getOrElse { throw IllegalArgumentException("accountKey20 is not valid hex: $hex", it) }
    require(bytes.size == ACCOUNT_KEY_20_SIZE) { "accountKey20 must be $ACCOUNT_KEY_20_SIZE bytes, got ${bytes.size}: $hex" }

    return bytes.intoKey()
}

package io.novafoundation.nova.feature_xcm_api.config.remote

import io.novafoundation.nova.common.utils.asGsonParsedInt
import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.NetworkId
import io.novafoundation.nova.feature_xcm_api.multiLocation.toInterior
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

typealias JunctionsRemote = Map<String, Any?>

fun JunctionsRemote.toAbsoluteLocation(): AbsoluteMultiLocation {
    return AbsoluteMultiLocation(toInterior())
}

fun JunctionsRemote.toInterior(): MultiLocation.Interior {
    return map { (type, value) -> mapJunctionFromRemote(type, value) }
        .toInterior()
}

private fun mapJunctionFromRemote(type: String, value: Any?): Junction {
    return when (type) {
        "globalConsensus" -> Junction.GlobalConsensus(mapGlobalConsensusFromRemote(value))
        "parachainId" -> Junction.ParachainId(value.asGsonParsedNumber())
        "generalKey" -> Junction.GeneralKey(value as String)
        "palletInstance" -> Junction.PalletInstance(value.asGsonParsedNumber())
        "generalIndex" -> Junction.GeneralIndex(value.asGsonParsedNumber())
        else -> throw IllegalArgumentException("Unknown junction type: $type")
    }
}

private fun mapGlobalConsensusFromRemote(junctionValue: Any?): NetworkId {
    return when(junctionValue) {
        "Polkadot" -> NetworkId.Substrate(Chain.Geneses.POLKADOT)
        "Kusama" -> NetworkId.Substrate(Chain.Geneses.KUSAMA)
        is Map<*, *> -> {
            val entry = junctionValue.entries.first()

            when(entry.key) {
                "ByGenesis" -> {
                    val genesis = (entry.value as String).removeHexPrefix()
                    NetworkId.Substrate(genesis)
                }
                "Ethereum" -> {
                    val evmId = entry.asGsonParsedInt()
                    NetworkId.Ethereum(evmId)
                }

                else -> error("Unknown network id: ${entry.key}")
            }
        }

        else -> error("Unknown network id: $junctionValue")
    }
}

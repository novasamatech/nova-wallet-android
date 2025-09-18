package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.common.data.network.runtime.binding.ParaId
import io.novafoundation.nova.common.utils.reversed
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ChainLocationConverter {

    suspend fun chainFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain?

    suspend fun chainFromAbsoluteLocation(
        location: AbsoluteMultiLocation,
        consensusRoot: Chain,
    ): Chain?

    suspend fun absoluteLocationFromChain(
        chainId: ChainId
    ): AbsoluteMultiLocation
}

class RealChainLocationConverter(
    // parachainId can be added to chains.json, can be fetched from ParachainInfo.ParachainId storage from each parachain
    // relay is not included here as it has no para id obviously
    private val paraIdByChain: Map<ChainId, ParaId>,
    private val chainRegistry: ChainRegistry,
) : ChainLocationConverter {

    private val chainByParaId = paraIdByChain.reversed()

    override suspend fun chainFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain? {
        val consensusRoot = getConsensusRoot(pointOfView)
        val povAbsoluteLocation = absoluteLocationFromChain(pointOfView.id)

        val absoluteLocation = location.absoluteLocationViewingFrom(povAbsoluteLocation)

        return chainFromAbsoluteLocation(absoluteLocation, consensusRoot)
    }

    override suspend fun chainFromAbsoluteLocation(
        location: AbsoluteMultiLocation,
        consensusRoot: Chain,
    ): Chain? {
        val junctions = location.junctions

        return when (junctions.size) {
            0 -> consensusRoot
            1 -> {
                val parachainId = junctions.single() as? ParachainId ?: return null
                val chainId = chainByParaId[parachainId.id] ?: return null

                chainRegistry.getChain(chainId)
            }
            else -> null
        }
    }

    override suspend fun absoluteLocationFromChain(chainId: ChainId): AbsoluteMultiLocation {
        val parachainId = paraIdByChain[chainId]

        return if (parachainId != null) {
            AbsoluteMultiLocation(ParachainId(parachainId))
        } else {
            MultiLocation.Interior.Here.asLocation()
        }
    }

    private suspend fun getConsensusRoot(chain: Chain): Chain {
        val parentId = chain.parentId

        return if (parentId != null) {
            chainRegistry.getChain(parentId)
        } else {
            chain
        }
    }
}

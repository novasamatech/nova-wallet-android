package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.common.utils.reversedManyToOne
import io.novafoundation.nova.feature_xcm_api.config.model.ChainXcmConfig
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull

class RealChainLocationConverter(
    private val xcmConfig: ChainXcmConfig,
    private val chainRegistry: ChainRegistry,
) : ChainLocationConverter {

    private val chainsByParaId = xcmConfig.parachainIds.reversedManyToOne()

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
                val candidates = chainsByParaId[parachainId.id] ?: return null

                val chains = chainRegistry.chainsById()

                candidates.tryFindNonNull { candidateChainId ->
                    chains[candidateChainId]?.takeIf { it.parentId == consensusRoot.id }
                }
            }
            else -> null
        }
    }

    override suspend fun absoluteLocationFromChain(chainId: ChainId): AbsoluteMultiLocation {
        val parachainId = xcmConfig.parachainIds[chainId]

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

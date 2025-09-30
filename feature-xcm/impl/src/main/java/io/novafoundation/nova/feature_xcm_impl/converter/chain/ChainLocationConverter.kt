package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.common.utils.reversedManyToOne
import io.novafoundation.nova.feature_xcm_api.config.model.ChainXcmConfig
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.GlobalConsensus
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.junctions
import io.novafoundation.nova.runtime.ext.createEvmChainId
import io.novafoundation.nova.runtime.ext.evmChainIdOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
        val povAbsoluteLocation = absoluteLocationFromChain(pointOfView)
        val absoluteLocation = location.absoluteLocationViewingFrom(povAbsoluteLocation)

        return chainFromAbsoluteLocation(absoluteLocation)
    }

    override suspend fun chainFromAbsoluteLocation(
        location: AbsoluteMultiLocation,
    ): Chain? {
        return location.junctions.fold(null) { currentChain, junction ->
            when (junction) {
                is GlobalConsensus -> descendToConsensus(junction, currentChain)

                is ParachainId -> descendToParachain(junction, currentChain)

                else -> null
            }
        }
    }

    private suspend fun descendToConsensus(junction: GlobalConsensus, currentChain: Chain?): Chain? {
        if (currentChain != null) {
            // GlobalConsensus should be the first junction
            return null
        }

        return getNetwork(junction.networkId)
    }

    private suspend fun descendToParachain(junction: ParachainId, currentChain: Chain?): Chain? {
        if (currentChain == null) {
            // ParachainId should always be prepended with GlobalConsensus
            return null
        }

        val candidates = chainsByParaId[junction.id] ?: return null

        val chains = chainRegistry.chainsById()

        return candidates.tryFindNonNull { candidateChainId ->
            chains[candidateChainId]?.takeIf { it.parentId == currentChain.id }
        }
    }

    override suspend fun absoluteLocationFromChain(chain: Chain): AbsoluteMultiLocation {
        val consensusRoot = getConsensusRoot(chain)
        val parachainId = xcmConfig.parachainIds[chain.id]

        val junctions = listOfNotNull(
            consensusRoot.getNetworkId().let(::GlobalConsensus),
            parachainId?.let(::ParachainId)
        )

        return junctions.asLocation()
    }

    private suspend fun getConsensusRoot(chain: Chain): Chain {
        val parentId = chain.parentId

        return if (parentId != null) {
            chainRegistry.getChain(parentId)
        } else {
            chain
        }
    }

    private suspend fun getNetwork(networkId: MultiLocation.NetworkId): Chain? {
        val chainId = when (networkId) {
            is MultiLocation.NetworkId.Ethereum -> createEvmChainId(networkId.chainId)
            is MultiLocation.NetworkId.Substrate -> networkId.genesisHash
        }

        return chainRegistry.getChain(chainId)
    }

    private fun Chain.getNetworkId(): MultiLocation.NetworkId {
        val evmChainId = evmChainIdOrNull()
        if (evmChainId != null) {
            return MultiLocation.NetworkId.Ethereum(evmChainId.toInt())
        }

        return MultiLocation.NetworkId.Substrate(id)
    }
}

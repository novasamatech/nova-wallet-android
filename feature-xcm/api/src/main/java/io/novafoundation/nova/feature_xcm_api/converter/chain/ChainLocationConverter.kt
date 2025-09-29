package io.novafoundation.nova.feature_xcm_api.converter.chain

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

// TODO we need to switch to ConsensusRoot-prepended locations in the config
// So we can support the most generic case
// We can do that in V9 version of the config
interface ChainLocationConverter {

    /**
     * Get consensus root of the [chain]
     * Consensus root is defined to be:
     * 1. [chain] if [chain] is standalone
     * 2. [chain] if [chain] is a relay-chain
     * 3. relaychain of [chain] if [chain] is a parachain
     */
    suspend fun getConsensusRoot(chain: Chain): Chain

    /**
     * Determine chain that corresponds to the given relative [location]
     * when viewing from [pointOfView] chain
     */
    suspend fun chainFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain?

    /**
     *  Determine chain that corresponds to the given absolute [location] within the given [consensusRoot]
     */
    suspend fun chainFromAbsoluteLocation(
        location: AbsoluteMultiLocation,
        consensusRoot: Chain,
    ): Chain?

    /**
     * Determine absolute location of the given [chainId]
     */
    suspend fun absoluteLocationFromChain(
        chainId: ChainId
    ): AbsoluteMultiLocation
}

suspend fun ChainLocationConverter.chainLocationOf(chainId: ChainId): ChainLocation {
    return ChainLocation(
        chainId = chainId,
        location = absoluteLocationFromChain(chainId)
    )
}

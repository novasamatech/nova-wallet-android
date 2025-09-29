package io.novafoundation.nova.feature_xcm_api.converter.chain

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ChainLocationConverter {

    suspend fun getConsensusRoot(chain: Chain): Chain

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

suspend fun ChainLocationConverter.chainLocationOf(chainId: ChainId): ChainLocation {
    return ChainLocation(
        chainId = chainId,
        location = absoluteLocationFromChain(chainId)
    )
}

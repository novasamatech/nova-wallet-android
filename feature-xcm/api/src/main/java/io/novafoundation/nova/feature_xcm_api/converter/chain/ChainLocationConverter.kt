package io.novafoundation.nova.feature_xcm_api.converter.chain

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainLocationConverter {

    /**
     * Determine chain that corresponds to the given relative [location]
     * when viewing from [pointOfView] chain
     */
    suspend fun chainFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain?

    /**
     * Determine chain that corresponds to the given absolute [location]
     */
    suspend fun chainFromAbsoluteLocation(location: AbsoluteMultiLocation): Chain?

    /**
     * Determine absolute location of the given [chain]
     */
    suspend fun absoluteLocationFromChain(chain: Chain): AbsoluteMultiLocation
}

suspend fun ChainLocationConverter.chainLocationOf(chain: Chain): ChainLocation {
    return ChainLocation(
        chainId = chain.id,
        location = absoluteLocationFromChain(chain)
    )
}

package io.novafoundation.nova.feature_xcm_api.converter.chain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation

interface ChainMultiLocationConverter {

    suspend fun toChain(multiLocation: RelativeMultiLocation): Chain?
}

package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation

interface ChainMultiLocationConverter {

    suspend fun toChain(multiLocation: MultiLocation): Chain?
}

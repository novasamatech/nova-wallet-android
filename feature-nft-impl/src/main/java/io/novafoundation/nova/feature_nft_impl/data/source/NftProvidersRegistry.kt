package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_nft_impl.data.source.providers.UniquesNftProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class NftProvidersRegistry(
    private val uniquesNftSource: UniquesNftProvider
) {

    fun get(chain: Chain): List<NftProvider> {
        return when(chain.genesisHash) {
            Chain.Geneses.STATEMINE -> listOf(uniquesNftSource)
            else -> emptyList()
        }
    }
}

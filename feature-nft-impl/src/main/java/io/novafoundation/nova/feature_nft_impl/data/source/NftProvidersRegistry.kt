package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.RmrkV1NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.RmrkV2NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.UniquesNftProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class NftProvidersRegistry(
    private val uniquesNftSource: UniquesNftProvider,
    private val rmrkV1NftProvider: RmrkV1NftProvider,
    private val rmrkV2NftProvider: RmrkV2NftProvider,
) {

    fun get(chain: Chain): List<NftProvider> {
        return when (chain.genesisHash) {
            Chain.Geneses.STATEMINE -> listOf(uniquesNftSource)
            Chain.Geneses.KUSAMA -> listOf(rmrkV1NftProvider, rmrkV2NftProvider)
            else -> emptyList()
        }
    }
}

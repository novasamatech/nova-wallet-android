package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.RmrkV1NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.RmrkV2NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.UniquesNftProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class NftProvidersRegistry(
    private val uniquesNftProvider: UniquesNftProvider,
    private val rmrkV1NftProvider: RmrkV1NftProvider,
    private val rmrkV2NftProvider: RmrkV2NftProvider,
) {

    private val statemineProviders = listOf(uniquesNftProvider)
    private val kusamaProviders = listOf(rmrkV1NftProvider, rmrkV2NftProvider)

    fun get(chain: Chain): List<NftProvider> {
        return when (chain.genesisHash) {
            Chain.Geneses.STATEMINE -> statemineProviders
            Chain.Geneses.KUSAMA -> kusamaProviders
            else -> emptyList()
        }
    }

    fun get(nft: Nft): NftProvider {
        return when(nft.type) {
            is Nft.Type.Rmrk1 -> rmrkV1NftProvider
            is Nft.Type.Rmrk2 -> rmrkV2NftProvider
            is Nft.Type.Uniques -> uniquesNftProvider
        }
    }
}

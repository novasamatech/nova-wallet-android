package io.novafoundation.nova.feature_nft_impl.data.source

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.RmrkV1NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.RmrkV2NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.UniquesNftProvider
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class NftProvidersRegistry(
    private val uniquesNftProvider: UniquesNftProvider,
    private val rmrkV1NftProvider: RmrkV1NftProvider,
    private val rmrkV2NftProvider: RmrkV2NftProvider,
) {

    private val statemineProviders = listOf(uniquesNftProvider)
    private val kusamaProviders = listOf(rmrkV1NftProvider, rmrkV2NftProvider)

    fun get(chain: Chain): List<NftProvider> {
        return when (chain.id) {
            Chain.Geneses.STATEMINE -> statemineProviders
            Chain.Geneses.KUSAMA -> kusamaProviders
            else -> emptyList()
        }
    }

    fun get(nftTypeKey: Nft.Type.Key): NftProvider {
        return when (nftTypeKey) {
            Nft.Type.Key.RMRKV1 -> rmrkV1NftProvider
            Nft.Type.Key.RMRKV2 -> rmrkV2NftProvider
            Nft.Type.Key.UNIQUES -> uniquesNftProvider
        }
    }
}

package io.novafoundation.nova.feature_nft_impl.domain.common

import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun groupAndSortNftsByNetwork(
    nfts: List<Nft>,
    chainsById: Map<String, Chain>
): Map<Chain, List<Nft>> {
    return nfts.groupBy { chainsById.getValue(it.chain.id) }
}

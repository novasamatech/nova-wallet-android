package io.novafoundation.nova.feature_assets.domain.manageTokens

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId

class MultiChainToken(
    val id: String,
    val symbol: String,
    val icon: String?,
    val instances: List<ChainTokenInstance>,
) {

    class ChainTokenInstance(
        val chain: Chain,
        val chainAssetId: ChainAssetId
    )
}

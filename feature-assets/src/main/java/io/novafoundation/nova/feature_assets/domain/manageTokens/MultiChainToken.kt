package io.novafoundation.nova.feature_assets.domain.manageTokens

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class MultiChainToken(
    val id: String,
    val symbol: String,
    val icon: String?,
    val instances: List<ChainTokenInstance>,
) {

    class ChainTokenInstance(
        val chain: Chain,
        val chainAssetId: ChainAssetId,
        val isEnabled: Boolean
    )
}

fun MultiChainToken.isEnabled(): Boolean {
    return instances.any { it.isEnabled }
}

fun MultiChainToken.allChainAssetIds(): List<FullChainAssetId> {
    return instances.map {
        FullChainAssetId(it.chain.id, it.chainAssetId)
    }
}

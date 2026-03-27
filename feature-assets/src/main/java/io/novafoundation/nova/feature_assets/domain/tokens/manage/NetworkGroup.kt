package io.novafoundation.nova.feature_assets.domain.tokens.manage

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class NetworkGroup(
    val chain: Chain,
    val tokens: List<TokenInNetwork>
) {

    class TokenInNetwork(
        val chain: Chain,
        val asset: Chain.Asset,
        val isEnabled: Boolean,
        val isSwitchable: Boolean
    )
}

fun NetworkGroup.allChainAssetIds(): List<FullChainAssetId> {
    return tokens.map { FullChainAssetId(it.chain.id, it.asset.id) }
}

fun NetworkGroup.isEnabled(): Boolean {
    return tokens.any { it.isEnabled }
}

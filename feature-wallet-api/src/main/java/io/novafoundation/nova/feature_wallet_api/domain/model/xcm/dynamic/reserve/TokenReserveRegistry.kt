package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class TokenReserveRegistry(
    private val reservesById: Map<TokenReserveId, TokenReserve>,

    // By default, asset reserve id is equal to its symbol
    // This mapping allows to override that for cases like multiple reserves (Statemine & Polkadot for DOT)
    private val assetToReserveIdOverrides: Map<FullChainAssetId, TokenReserveId>
) {

    fun getReserve(chainAsset: Chain.Asset): TokenReserve {
        val reserveId = getReserveId(chainAsset)
        return reservesById.getValue(reserveId)
    }

    fun isReserveKnown(chainAsset: Chain.Asset): Boolean {
        val reserveId = getReserveId(chainAsset)
        return reserveId in reservesById
    }

    private fun getReserveId(chainAsset: Chain.Asset): TokenReserveId {
        return assetToReserveIdOverrides[chainAsset.fullId] ?: chainAsset.normalizeSymbol()
    }
}

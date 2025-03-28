package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_wallet_api.data.repository.getChainLocation
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.repository.ParachainInfoRepository

class TokenReserveRegistry(
    private val parachainInfoRepository: ParachainInfoRepository,

    private val reservesById: Map<TokenReserveId, TokenReserveConfig>,

    // By default, asset reserve id is equal to its symbol
    // This mapping allows to override that for cases like multiple reserves (Statemine & Polkadot for DOT)
    private val assetToReserveIdOverrides: Map<FullChainAssetId, TokenReserveId>
) {

    suspend fun getReserve(chainAsset: Chain.Asset): TokenReserve {
        val reserveId = getReserveId(chainAsset)
        val reserve = reservesById.getValue(reserveId)
        return TokenReserve(
            reserveChainLocation = parachainInfoRepository.getChainLocation(reserve.reserveChainId),
            tokenLocation = reserve.tokenReserveLocation
        )
    }

    private fun getReserveId(chainAsset: Chain.Asset): TokenReserveId {
        return assetToReserveIdOverrides[chainAsset.fullId] ?: chainAsset.normalizeSymbol()
    }
}

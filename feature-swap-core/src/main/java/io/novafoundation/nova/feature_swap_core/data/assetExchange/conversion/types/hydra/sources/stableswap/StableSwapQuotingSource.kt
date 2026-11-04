package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.stableswap

import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model.RemoteAndLocalId
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.TradeAmountLimitedEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

interface StableSwapQuotingSource : HydraDxQuotingSource<StableSwapQuotingSource.Edge> {

    val chain: Chain

    interface Edge : QuotableEdge, TradeAmountLimitedEdge {

        val fromAsset: RemoteAndLocalId

        val toAsset: RemoteAndLocalId

        val poolId: HydraDxAssetId

        suspend fun minTradingLimit(): BigInteger

        override suspend fun minAllowedAmountIn(): BigInteger? {
            // execute_sell withdraws shares via remove_liquidity_one_asset, which has no trading minimum.
            // Ordinary sells and deposits via add_assets_liquidity both require MinTradingLimit.
            return if (fromAsset.first == poolId) null else minTradingLimit()
        }

        override suspend fun minAllowedAmountOut(): BigInteger? {
            // execute_buy uses withdraw_asset_amount / add_liquidity_shares for LP edges.
            // Neither liquidity call applies MinTradingLimit to the specified output.
            return if (fromAsset.first == poolId || toAsset.first == poolId) null else minTradingLimit()
        }
    }
}

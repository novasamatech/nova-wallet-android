package io.novafoundation.nova.feature_swap_impl.data.assetExchange

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface AssetExchange {

    interface Factory {

        fun create(chainId: ChainId): AssetExchange
    }

    suspend fun availableSwapDirections(): Map<FullChainAssetId, Set<FullChainAssetId>>

    suspend fun availableDirectionsForAsset(asset: Chain.Asset): Set<FullChainAssetId>

    suspend fun quote(args: SwapArgs): Result<SwapQuote>

    suspend fun swap(args: SwapArgs): Result<ExtrinsicHash>
}

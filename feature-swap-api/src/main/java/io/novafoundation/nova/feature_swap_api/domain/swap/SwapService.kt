package io.novafoundation.nova.feature_swap_api.domain.swap

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope

interface SwapService {

    suspend fun assetsAvailableForSwap(computationScope: CoroutineScope): Set<FullChainAssetId>

    suspend fun availableSwapDirectionsFor(asset: Chain.Asset, computationScope: CoroutineScope): Set<FullChainAssetId>

    suspend fun quote(args: SwapQuoteArgs): Result<SwapQuote>

    suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicHash>
}

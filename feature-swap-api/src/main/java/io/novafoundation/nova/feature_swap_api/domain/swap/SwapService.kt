package io.novafoundation.nova.feature_swap_api.domain.swap

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SwapService {

    suspend fun warmUpCommonChains(computationScope: CoroutineScope): Result<Unit>

    suspend fun sync(coroutineScope: CoroutineScope)

    suspend fun assetsAvailableForSwap(computationScope: CoroutineScope): Flow<Set<FullChainAssetId>>

    suspend fun availableSwapDirectionsFor(asset: Chain.Asset, computationScope: CoroutineScope): Flow<Set<FullChainAssetId>>

    suspend fun hasAvailableSwapDirections(asset: Chain.Asset, computationScope: CoroutineScope): Flow<Boolean>

    suspend fun quote(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): Result<SwapQuote>

    suspend fun estimateFee(executeArgs: SwapFeeArgs): SwapFee

    suspend fun swap(calculatedFee: SwapFee): Flow<SwapProgress>

    suspend fun submitFirstSwapStep(calculatedFee: SwapFee): Result<SwapSubmissionResult>

    suspend fun defaultSlippageConfig(chainId: ChainId): SlippageConfig

    fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger>

    suspend fun isDeepSwapAllowed(): Boolean
}

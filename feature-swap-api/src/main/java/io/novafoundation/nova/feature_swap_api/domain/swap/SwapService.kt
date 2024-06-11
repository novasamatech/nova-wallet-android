package io.novafoundation.nova.feature_swap_api.domain.swap

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface SwapService {

    suspend fun assetsAvailableForSwap(computationScope: CoroutineScope): Flow<Set<FullChainAssetId>>

    suspend fun availableSwapDirectionsFor(asset: Chain.Asset, computationScope: CoroutineScope): Flow<Set<FullChainAssetId>>

    suspend fun canPayFeeInNonUtilityAsset(asset: Chain.Asset): Boolean

    suspend fun quote(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): Result<SwapQuote>

    suspend fun estimateFee(quote: SwapQuote): SwapFee

    suspend fun swap(args: SwapExecuteArgs): Result<ExtrinsicSubmission>

    suspend fun slippageConfig(chainId: ChainId): SlippageConfig?

    fun runSubscriptions(chainIn: Chain, metaAccount: MetaAccount): Flow<ReQuoteTrigger>
}

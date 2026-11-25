package io.novafoundation.nova.feature_swap_api.domain.swap

import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapRouteEdgeId
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_swap_api.domain.model.ReQuoteTrigger
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFeeArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuotePreview
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapSubmissionResult
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

enum class SwapAssetRole { PAY, RECEIVE, EITHER }

interface SwapService {

    suspend fun resolveRoute(
        route: List<SwapRouteEdgeId>,
        computationScope: CoroutineScope
    ): List<SwapGraphEdge>

    suspend fun constructOperations(args: SwapFeeArgs): List<AtomicSwapOperation>

    suspend fun warmUpCommonChains(computationScope: CoroutineScope): Result<Unit>

    suspend fun sync(coroutineScope: CoroutineScope)

    suspend fun assetsAvailableForSwap(computationScope: CoroutineScope, role: SwapAssetRole = SwapAssetRole.EITHER): Flow<Set<FullChainAssetId>>

    suspend fun canSwap(pay: FullChainAssetId, receive: FullChainAssetId, metaAccountId: Long, scope: CoroutineScope): Boolean

    suspend fun availableSwapDirectionsFor(
        asset: Chain.Asset,
        computationScope: CoroutineScope,
        metaAccountId: Long? = null
    ): Flow<Set<FullChainAssetId>>

    suspend fun availableAssetsForSelection(
        oppositeAsset: FullChainAssetId,
        selectingPay: Boolean,
        metaAccountId: Long,
        computationScope: CoroutineScope
    ): Flow<Set<FullChainAssetId>>

    suspend fun hasAvailableSwapDirections(asset: Chain.Asset, computationScope: CoroutineScope): Flow<Boolean>

    // TODO(NW-2502): Once Review supports ExternalSwapQuote, use quote() for both the form and Review and remove preview().
    suspend fun preview(args: SwapQuoteArgs, computationSharingScope: CoroutineScope): SwapQuotePreview

    suspend fun quote(
        args: SwapQuoteArgs,
        computationSharingScope: CoroutineScope
    ): Result<SwapQuote>

    suspend fun estimateFee(executeArgs: SwapFeeArgs): SwapFee

    suspend fun swap(calculatedFee: SwapFee): Flow<SwapProgress>

    suspend fun submitFirstSwapStep(calculatedFee: SwapFee): Result<SwapSubmissionResult>

    suspend fun defaultSlippageConfig(chainId: ChainId): SlippageConfig

    fun runSubscriptions(metaAccount: MetaAccount): Flow<ReQuoteTrigger>

    suspend fun isDeepSwapAllowed(metaAccountId: Long? = null): Boolean
}

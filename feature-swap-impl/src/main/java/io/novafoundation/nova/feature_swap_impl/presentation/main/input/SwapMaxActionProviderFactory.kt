package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.utils.zipWithLastNonNull
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.swapMaximum
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableBalance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SwapMaxActionProviderFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun create(
        scope: CoroutineScope,
        assets: Flow<Asset>,
        outputAssets: Flow<Chain.Asset?>,
        fees: Flow<FeeStatus<SwapFee, *>>
    ): MaxActionProvider {
        val pairs = combine(assets.map { it.token.configuration }, outputAssets, ::SwapAssets)
            .distinctUntilChanged()

        val maximum = pairs.flatMapLatest { pair ->
            // Resolve the reserve before the first fee/quote, not as a side effect of estimating a fee.
            val chain = chainRegistry.getChain(pair.input.chainId)
            val minimum = deductionUseCase.invoke(pair.input, pair.output ?: pair.input, keepAlive = chain.swap.assetConversionSupported())
            val matchingFees = fees.map { status ->
                status.loadedFeeOrNull()?.takeIf {
                    it.segments.first().operation.assetIn == pair.input.fullId &&
                        it.segments.last().operation.assetOut == pair.output?.fullId
                }
            }.zipWithLastNonNull().map { (previous, current) -> current ?: previous }

            combine(assets, matchingFees) { asset, fee ->
                if (asset.token.configuration.fullId != pair.input.fullId) return@combine null
                val amount = swapMaximum(
                    transferable = asset.transferableInPlanks,
                    countedTowardsEd = asset.balanceCountedTowardsEDInPlanks,
                    minimumBalance = fee?.additionalMaxAmountDeduction?.fromCountedTowardsEd ?: minimum,
                    fee = fee?.totalFeeAmount(pair.input) ?: BigInteger.ZERO
                )
                MaxAvailableBalance.fromSingle(pair.input, amount)
            }
        }

        return MaxActionProvider.create(scope) {
            val provider = object : MaxActionProvider {
                override val maxAvailableBalance = maximum.filterNotNull().distinctUntilChanged()
            }
            provider
        }
    }

    private data class SwapAssets(val input: Chain.Asset, val output: Chain.Asset?)
}

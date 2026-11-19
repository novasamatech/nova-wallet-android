package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapRouteEdgeId
import io.novafoundation.nova.feature_swap_api.domain.model.routeId
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.SwapDirection
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.swapMaxFeeMargin
import io.novafoundation.nova.feature_swap_impl.domain.swapMaximum
import io.novafoundation.nova.feature_swap_impl.domain.withSwapMaxFeeMargin
import io.novafoundation.nova.feature_swap_impl.domain.validation.SwapDustCheckFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableBalance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.loadedFeeOrNull
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.runtime.ext.assetConversionSupported
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

internal data class SwapMaxEstimate(
    val balance: MaxAvailableBalance,
    val dustCheckFee: SwapDustCheckFee,
    val route: List<SwapRouteEdgeId>,
)

internal class SwapMaxActionProvider(
    private val maxEstimates: Flow<SwapMaxEstimate>,
    val affordableBalanceProvider: MaxActionProvider,
) : MaxActionProvider {

    override val maxAvailableBalance = maxEstimates.map { it.balance }.distinctUntilChanged()

    suspend fun dustCheckFeeFor(quote: SwapQuote): SwapDustCheckFee {
        if (quote.direction != SwapDirection.SPECIFIED_IN) return SwapDustCheckFee.QuoteFee

        val estimate = maxEstimates.first()
        val matchesAmount = estimate.balance.chainAsset.fullId == quote.assetIn.fullId && estimate.balance.actualBalance == quote.planksIn
        if (!matchesAmount) return SwapDustCheckFee.QuoteFee
        if (estimate.route != quote.quotedPath.path.map { it.edge.routeId() }) return SwapDustCheckFee.QuoteFee

        return estimate.dustCheckFee
    }
}

class SwapMaxActionProviderFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun create(
        scope: CoroutineScope,
        assets: Flow<Asset>,
        outputAssets: Flow<Chain.Asset?>,
        maxActive: Flow<Boolean>,
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>,
        feeResetVersion: Flow<Int> = flowOf(0)
    ): SwapMaxActionProvider {
        val swapAssets = combine(
            assets.map { it.token.configuration },
            outputAssets,
            ::SwapAssets
        ).distinctUntilChanged()

        val balances = with(scope) {
            feeResetVersion.flatMapLatest {
                swapAssets.flatMapLatest { pair -> balancesForAssets(pair, assets, feeLoaderMixin, maxActive) }
            }.shareInBackground()
        }

        val affordableBalanceProvider = object : MaxActionProvider {
            override val maxAvailableBalance = balances.map { it.affordable }.distinctUntilChanged()
        }

        return SwapMaxActionProvider(
            maxEstimates = balances.map { it.maximum },
            affordableBalanceProvider = affordableBalanceProvider
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun balancesForAssets(
        swapAssets: SwapAssets,
        assets: Flow<Asset>,
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>,
        maxActive: Flow<Boolean>
    ): Flow<SwapBalances> {
        return matchingFeesFlow(feeLoaderMixin, swapAssets, maxActive)
            .flatMapLatest { fees ->
                val reserve = fees?.latest?.additionalMaxAmountDeduction?.fromCountedTowardsEd ?: minimumBeforeQuote(swapAssets)
                val latestFee = fees?.latest?.totalFeeAmount(swapAssets.input) ?: Balance.ZERO
                val maxFee = fees?.anchor?.let { anchor ->
                    SwapDustCheckFee.MaxFeeWithMargin(anchor.totalFeeAmount(swapAssets.input).withSwapMaxFeeMargin())
                }

                assets.mapNotNull { asset ->
                    if (asset.token.configuration.fullId != swapAssets.input.fullId) return@mapNotNull null

                    SwapBalances(
                        maximum = SwapMaxEstimate(
                            balance = MaxAvailableBalance.fromSingle(swapAssets.input, asset.swapMaximum(reserve, maxFee?.amount ?: Balance.ZERO)),
                            dustCheckFee = maxFee ?: SwapDustCheckFee.QuoteFee,
                            route = fees?.anchor?.route.orEmpty()
                        ),
                        affordable = MaxAvailableBalance.fromSingle(swapAssets.input, asset.swapMaximum(reserve, latestFee))
                    )
                }
            }
    }

    private fun matchingFeesFlow(
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>,
        swapAssets: SwapAssets,
        maxActive: Flow<Boolean>
    ): Flow<MaxFees?> = flow {
        var latest: SwapFee? = null
        var anchor: SwapFee? = null

        combine(feeLoaderMixin.fee, maxActive, ::Pair).collect { (status, isMaxActive) ->
            status.loadedFeeOrNull()
                ?.takeIf {
                    it.segments.first().operation.assetIn == swapAssets.input.fullId &&
                        it.segments.last().operation.assetOut == swapAssets.output?.fullId
                }
                ?.let { latest = it }

            val current = latest
            if (current == null) {
                emit(null)
            } else {
                // Always compare with the fee that set Max so small successive changes cannot drift past its buffer.
                val maxFee = anchor?.takeIf { isMaxActive && it.canCoverMaxFee(current, swapAssets.input) } ?: current
                anchor = maxFee
                emit(MaxFees(latest = current, anchor = maxFee))
            }
        }
    }.distinctUntilChanged()

    private fun SwapFee.canCoverMaxFee(current: SwapFee, amountAsset: Chain.Asset): Boolean {
        if (this === current) return true
        if (route != current.route) return false
        if (additionalMaxAmountDeduction.fromCountedTowardsEd != current.additionalMaxAmountDeduction.fromCountedTowardsEd) return false
        if (initialSubmissionFee.asset.fullId != current.initialSubmissionFee.asset.fullId) return false

        val anchorFee = totalFeeAmount(amountAsset)
        return (current.totalFeeAmount(amountAsset) - anchorFee).abs() <= anchorFee.swapMaxFeeMargin()
    }

    private suspend fun minimumBeforeQuote(pair: SwapAssets): Balance {
        val chain = chainRegistry.getChain(pair.input.chainId)
        return deductionUseCase.invoke(pair.input, pair.output ?: pair.input, keepAlive = chain.swap.assetConversionSupported())
    }

    private data class SwapAssets(val input: Chain.Asset, val output: Chain.Asset?)

    private data class MaxFees(val latest: SwapFee, val anchor: SwapFee)

    private data class SwapBalances(val maximum: SwapMaxEstimate, val affordable: MaxAvailableBalance)
}

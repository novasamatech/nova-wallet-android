package io.novafoundation.nova.feature_swap_impl.presentation.main.input

import io.novafoundation.nova.common.utils.zipWithLastNonNull
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_impl.domain.AssetInAdditionalSwapDeductionUseCase
import io.novafoundation.nova.feature_swap_impl.domain.swapMaximum
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxActionProvider
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.MaxAvailableBalance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction.create
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class SwapMaxActionProviderFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val deductionUseCase: AssetInAdditionalSwapDeductionUseCase
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun create(
        scope: CoroutineScope,
        assets: Flow<Asset>,
        outputAssets: Flow<Chain.Asset?>,
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>
    ): MaxActionProvider {
        val maximum = getSwapAssetsFlow(assets, outputAssets)
            .flatMapLatest { swapAssets ->
                maximumAmountFlow(feeLoaderMixin, swapAssets, assets)
            }

        return MaxActionProvider.create(scope) {
            val provider = object : MaxActionProvider {
                override val maxAvailableBalance = maximum.distinctUntilChanged()
            }
            provider
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun maximumAmountFlow(
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>,
        swapAssets: SwapAssets,
        assets: Flow<Asset>
    ): Flow<MaxAvailableBalance> {
        return matchingFeesFlow(feeLoaderMixin, swapAssets)
            .flatMapLatest { fee ->
                val minimumBeforeQuote = if (fee == null) minimumBeforeQuote(swapAssets) else Balance.ZERO

                assets.mapNotNull { asset ->
                    if (asset.token.configuration.fullId != swapAssets.input.fullId) return@mapNotNull null
                    val amount = fee?.let { asset.swapMaximum(it) } ?: asset.swapMaximum(minimumBeforeQuote)
                    MaxAvailableBalance.fromSingle(swapAssets.input, amount)
                }
            }
    }

    private fun getSwapAssetsFlow(
        assets: Flow<Asset>,
        outputAssets: Flow<Chain.Asset?>
    ): Flow<SwapAssets> {
        return combine(
            assets.map { it.token.configuration },
            outputAssets,
            ::SwapAssets
        ).distinctUntilChanged()
    }

    private fun matchingFeesFlow(
        feeLoaderMixin: FeeLoaderMixinV2<SwapFee, *>,
        swapAssets: SwapAssets
    ): Flow<SwapFee?> {
        return feeLoaderMixin.fee.map { status ->
            status.loadedFeeOrNull()
                ?.takeIf {
                    it.segments.first().operation.assetIn == swapAssets.input.fullId &&
                        it.segments.last().operation.assetOut == swapAssets.output?.fullId
                }
        }
            .zipWithLastNonNull()
            .map { (previous, current) -> current ?: previous }
            .distinctUntilChanged()
    }

    private suspend fun minimumBeforeQuote(pair: SwapAssets): Balance {
        val chain = chainRegistry.getChain(pair.input.chainId)
        return deductionUseCase.invoke(pair.input, pair.output ?: pair.input, keepAlive = chain.swap.assetConversionSupported())
    }

    private data class SwapAssets(val input: Chain.Asset, val output: Chain.Asset?)
}

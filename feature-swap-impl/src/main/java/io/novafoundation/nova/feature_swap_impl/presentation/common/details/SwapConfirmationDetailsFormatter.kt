package io.novafoundation.nova.feature_swap_impl.presentation.common.details

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.formatting.formatPercents
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_api.domain.model.totalTime
import io.novafoundation.nova.feature_swap_api.presentation.formatters.SwapRateFormatter
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetView
import io.novafoundation.nova.feature_swap_api.presentation.view.SwapAssetsView
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.details.model.SwapConfirmationDetailsModel
import io.novafoundation.nova.feature_swap_impl.presentation.common.route.SwapRouteFormatter
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

interface SwapConfirmationDetailsFormatter {

    suspend fun format(quote: SwapQuote, slippage: Fraction): SwapConfirmationDetailsModel
}

class RealSwapConfirmationDetailsFormatter(
    private val chainRegistry: ChainRegistry,
    private val assetIconProvider: AssetIconProvider,
    private val tokenRepository: TokenRepository,
    private val swapRouteFormatter: SwapRouteFormatter,
    private val swapRateFormatter: SwapRateFormatter,
    private val priceImpactFormatter: PriceImpactFormatter,
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) : SwapConfirmationDetailsFormatter {

    override suspend fun format(quote: SwapQuote, slippage: Fraction): SwapConfirmationDetailsModel {
        val assetIn = quote.assetIn
        val assetOut = quote.assetOut
        val chainIn = chainRegistry.getChain(assetIn.chainId)
        val chainOut = chainRegistry.getChain(assetOut.chainId)

        return SwapConfirmationDetailsModel(
            assets = SwapAssetsView.Model(
                assetIn = formatAssetDetails(chainIn, assetIn, quote.planksIn),
                assetOut = formatAssetDetails(chainOut, assetOut, quote.planksOut)
            ),
            rate = formatRate(quote.swapRate(), assetIn, assetOut),
            priceDifference = formatPriceDifference(quote.priceImpact),
            slippage = slippage.formatPercents(),
            swapRouteModel = swapRouteFormatter.formatSwapRoute(quote),
            estimatedExecutionTime = resourceManager.formatDuration(quote.executionEstimate.totalTime(), estimated = true)
        )
    }

    private suspend fun formatAssetDetails(
        chain: Chain,
        chainAsset: Chain.Asset,
        amountInPlanks: BigInteger
    ): SwapAssetView.Model {
        val amount = formatAmount(chainAsset, amountInPlanks)

        return SwapAssetView.Model(
            assetIcon = assetIconProvider.getAssetIconOrFallback(chainAsset),
            amount = amount,
            chainUi = mapChainToUi(chain),
        )
    }

    private fun formatRate(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String {
        return swapRateFormatter.format(rate, assetIn, assetOut)
    }

    private fun formatPriceDifference(priceDifference: Fraction): CharSequence? {
        return priceImpactFormatter.format(priceDifference)
    }

    private suspend fun formatAmount(chainAsset: Chain.Asset, amount: BigInteger): AmountModel {
        val token = tokenRepository.getToken(chainAsset)
        return amountFormatter.formatAmountToAmountModel(amount, token, AmountConfig(includeZeroFiat = false, estimatedFiat = true))
    }
}

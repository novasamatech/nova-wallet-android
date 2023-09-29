package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import org.junit.Test
import java.math.BigDecimal

class SwapServiceIntegrationTest : BaseIntegrationTest() {

    private val swapApi = FeatureUtils.getFeature<SwapFeatureComponent>(context, SwapFeatureApi::class.java)

    private val walletApi = FeatureUtils.getFeature<WalletFeatureComponent>(context, WalletFeatureApi::class.java)

    private val tokenRepository = walletApi.provideTokenRepository()
    private val swapService = swapApi.swapService

    @Test
    fun shouldRetrieveAvailableDirections() = runTest {
        val allAvailableAssetIdsToSwap = swapService.assetsAvailableForSwap(this)
        val allAvailableChainAssetsToSwap = allAvailableAssetIdsToSwap.map {
            val (chain, asset) = chainRegistry.chainWithAsset(it.chainId, it.assetId)

            "${chain.name}::${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "All available tokens: ${allAvailableChainAssetsToSwap.joinToString()}")

        val westmint = chainRegistry.getChain(Chain.Geneses.WESTMINT)
        val wndOnWestmint = westmint.utilityAsset
        val siriOnWestmint = westmint.assets.first { it.symbol == "SIRI" }

        val directionsForWnd = swapService.availableSwapDirectionsFor(wndOnWestmint, this)
        val directionsForWndFormatted = directionsForWnd.map { otherId ->
            val asset = chainRegistry.asset(otherId)

            "${wndOnWestmint.symbol} - ${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "Available directions for ${wndOnWestmint.symbol}: ${directionsForWndFormatted.joinToString()}")

        val swapQuote = swapService.quote(
            args = SwapQuoteArgs(
                tokenIn = tokenRepository.getToken(wndOnWestmint),
                tokenOut = tokenRepository.getToken(siriOnWestmint),
                amount = wndOnWestmint.planksFromAmount(0.001.toBigDecimal()),
                swapDirection = SwapDirection.SPECIFIED_IN,
                slippage = Percent(1.0)
            )
        ).getOrThrow()

        Log.d("SwapServiceIntegrationTest", swapQuote.format())
    }

    private fun SwapQuote.format(): String {
        return """
            Swapping ${planksIn.formatPlanks(assetIn)} yields ${planksOut.formatPlanks(assetOut)}.
            Swap rate is ${BigDecimal.ONE.formatTokenAmount(assetIn)} = ${swapRate().formatTokenAmount(assetOut)}"
        """.trimIndent()
    }
}

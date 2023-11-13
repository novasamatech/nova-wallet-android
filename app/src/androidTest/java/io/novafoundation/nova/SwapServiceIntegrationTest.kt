package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_swap_api.domain.model.swapRate
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureComponent
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import org.junit.Test
import java.math.BigDecimal

class SwapServiceIntegrationTest : BaseIntegrationTest() {

    private val swapApi = FeatureUtils.getFeature<SwapFeatureComponent>(context, SwapFeatureApi::class.java)

    private val walletApi = FeatureUtils.getFeature<WalletFeatureComponent>(context, WalletFeatureApi::class.java)

    private val tokenRepository = walletApi.provideTokenRepository()
    private val arbitraryAssetUseCase = walletApi.arbitraryAssetUseCase
    private val swapService = swapApi.swapService

    @Test
    fun shouldRetrieveAvailableDirections() = runTest {
        val allAvailableAssetIdsToSwap = swapService.assetsAvailableForSwap(this)
        val allAvailableChainAssetsToSwap = allAvailableAssetIdsToSwap.map {
            val (chain, asset) = chainRegistry.chainWithAsset(it.chainId, it.assetId)

            "${chain.name}::${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "All available tokens: ${allAvailableChainAssetsToSwap.joinToString()}")
    }

    @Test
    fun shouldRetrieveAvailableDirectionsForNativeAsset() = runTest {
        val westmint = chainRegistry.westmint()

        findAvailableDirectionsFor(westmint.wnd())
    }

    @Test
    fun shouldRetrieveAvailableDirectionsForLocalAsset() = runTest {
        val westmint = chainRegistry.westmint()

        findAvailableDirectionsFor(westmint.siri())
    }

    @Test
    fun shouldRetrieveAvailableDirectionsForForeignAsset() = runTest {
        val westmint = chainRegistry.westmint()

        findAvailableDirectionsFor(westmint.dot())
    }

    @Test
    fun shouldCalculateNativeAssetFee() = runTest {
        val westmint = chainRegistry.westmint()
        val wnd = westmint.wnd()
        val siri = westmint.siri()

        val swapArgs = SwapExecuteArgs(
            assetIn = wnd,
            assetOut = siri,
            swapLimit = SwapLimit.SpecifiedIn(
                expectedAmountIn = siri.planksFromAmount(0.000001.toBigDecimal()),
                amountOutMin = Balance.ZERO
            ),
            customFeeAsset = null,
            nativeAsset = arbitraryAssetUseCase.assetFlow(westmint.commissionAsset).first()
        )

        val fee = swapService.estimateFee(swapArgs)

        Log.d("SwapServiceIntegrationTest", "Fee for swapping ${wnd.symbol} to ${wnd.symbol} is ${fee.networkFee.amount.formatPlanks(wnd)}")
    }

    @Test
    fun shouldCalculateCustomAssetFee() = runTest {
        val westmint = chainRegistry.westmint()
        val wnd = westmint.wnd()
        val siri = westmint.siri()

        val swapArgs = SwapExecuteArgs(
            assetIn = siri,
            assetOut = wnd,
            swapLimit = SwapLimit.SpecifiedIn(
                expectedAmountIn = siri.planksFromAmount(0.000001.toBigDecimal()),
                amountOutMin = Balance.ZERO
            ),
            customFeeAsset = siri,
            nativeAsset = arbitraryAssetUseCase.assetFlow(westmint.commissionAsset).first()
        )

        val fee = swapService.estimateFee(swapArgs)

        Log.d("SwapServiceIntegrationTest", "Fee for swapping ${wnd.symbol} to ${siri.symbol} is ${fee.networkFee.amount.formatPlanks(siri)}")
    }

    @Test
    fun shouldQuoteLocalAssetSwap() = runTest {
        val westmint = chainRegistry.westmint()

        quoteSwap(from = westmint.wnd(), to = westmint.siri(), amount = 0.000001)
    }

    @Test(expected = SwapQuoteException.NotEnoughLiquidity::class)
    fun shouldQuoteForeignAssetSwap() = runTest {
        val westmint = chainRegistry.westmint()

        quoteSwap(from = westmint.wnd(), to = westmint.dot(), amount = 0.000001)
    }

    private suspend fun quoteSwap(from: Chain.Asset, to: Chain.Asset, amount: Double) {
        val swapQuote = swapService.quote(
            args = SwapQuoteArgs(
                tokenIn = tokenRepository.getToken(from),
                tokenOut = tokenRepository.getToken(to),
                amount = from.planksFromAmount(amount.toBigDecimal()),
                swapDirection = SwapDirection.SPECIFIED_IN,
                slippage = Percent(1.0),
            )
        ).getOrThrow()

        Log.d("SwapServiceIntegrationTest", swapQuote.format())
    }

    private suspend fun CoroutineScope.findAvailableDirectionsFor(asset: Chain.Asset) {
        val directionsForWnd = swapService.availableSwapDirectionsFor(asset, this)
        val directionsForWndFormatted = directionsForWnd.map { otherId ->
            val otherAsset = chainRegistry.asset(otherId)

            "${asset.symbol} - ${otherAsset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "Available directions for ${asset.symbol}: ${directionsForWndFormatted.joinToString()}")
    }

    private fun SwapQuote.format(): String {
        return """
            Swapping ${planksIn.formatPlanks(assetIn)} yields ${planksOut.formatPlanks(assetOut)}.
            Swap rate is ${BigDecimal.ONE.formatTokenAmount(assetIn)} = ${swapRate().formatTokenAmount(assetOut)}"
        """.trimIndent()
    }

    private suspend fun ChainRegistry.westmint(): Chain {
        return getChain(Chain.Geneses.WESTMINT)
    }

    private fun Chain.siri(): Chain.Asset {
        return assets.first { it.symbol == "SIRI" }
    }

    private fun Chain.dot(): Chain.Asset {
        return assets.first { it.symbol == "DOT" }
    }

    private fun Chain.wnd(): Chain.Asset {
        return utilityAsset
    }
}

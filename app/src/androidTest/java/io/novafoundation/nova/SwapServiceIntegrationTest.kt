package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import org.junit.Test

class SwapServiceIntegrationTest : BaseIntegrationTest() {

    private val swapApi = FeatureUtils.getFeature<SwapFeatureComponent>(context, SwapFeatureApi::class.java)

    private val swapService = swapApi.swapService

    @Test
    fun shouldRetrieveAvailableDirections() = runTest {
        val allAvailableAssetIdsToSwap = swapService.assetsAvailableForSwap(this)
        val allAvailableChainAssetsToSwap = allAvailableAssetIdsToSwap.map {
            val (chain, asset) = chainRegistry.chainWithAsset(it.chainId, it.assetId)

            "${chain.name}::${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "All available tokens: ${allAvailableChainAssetsToSwap.joinToString()}")

        val wndOnWestmint = chainRegistry.asset(Chain.Geneses.WESTMINT, 0)
        val directionsForWnd = swapService.availableSwapDirectionsFor(wndOnWestmint, this)
        val directionsForWndFormatted = directionsForWnd.map { otherId ->
            val asset = chainRegistry.asset(otherId)

            "${wndOnWestmint.symbol} - ${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", "Available directions for ${wndOnWestmint.symbol}: ${directionsForWndFormatted.joinToString()}")
    }
}

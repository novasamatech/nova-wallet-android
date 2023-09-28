package io.novafoundation.nova

import android.util.Log
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureComponent
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import org.junit.Test

class SwapServiceIntegrationTest : BaseIntegrationTest() {

    private val swapApi = FeatureUtils.getFeature<SwapFeatureComponent>(context, SwapFeatureApi::class.java)

    @Test
    fun shouldRetrieveAvailableDirections() = runTest {
        val allAvailableAssetIdsToSwap = swapApi.swapService.assetsAvailableForSwap(this)
        val allAvailableChainAssetsToSwap = allAvailableAssetIdsToSwap.map {
            val (chain, asset) = chainRegistry.chainWithAsset(it.chainId, it.assetId)

            "${chain.name}.${asset.symbol}"
        }

        Log.d("SwapServiceIntegrationTest", allAvailableChainAssetsToSwap.joinToString())
    }
}

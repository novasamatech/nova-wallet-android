package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk

import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

class XYKSwapSourceFactory() : HydraDxSwapSource.Factory {

    override fun create(chain: Chain): HydraDxSwapSource {
        return XYKSwapSource()
    }
}

private class XYKSwapSource() : HydraDxSwapSource {

    override val identifier: String = "Xyk"

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        // We don't need a specific implementation for XYKSwap extrinsics since it is done by HydraDxExchange on the upper level via Router
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        return DictEnum.Entry("XYK", null)
    }
}

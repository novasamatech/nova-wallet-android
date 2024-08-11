package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap

import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.HydraDxSwapSource
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

private const val POOL_ID_PARAM_KEY = "PoolId"

class StableSwapSourceFactory() : HydraDxSwapSource.Factory {

    companion object {
        const val ID = "StableSwap"
    }

    override fun create(chain: Chain): HydraDxSwapSource {
        return StableSwapSource()
    }
}

private class StableSwapSource() : HydraDxSwapSource {

    override val identifier: String = StableSwapSourceFactory.ID

    override suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs) {
        // We don't need a specific implementation for StableSwap extrinsics since it is done by HydraDxExchange on the upper level via Router
    }

    override fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*> {
        val poolId = params.getValue(POOL_ID_PARAM_KEY).toBigInteger()

        return DictEnum.Entry("Stableswap", poolId)
    }
}

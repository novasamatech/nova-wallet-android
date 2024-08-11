package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder

interface HydraDxSwapSource : Identifiable {

    suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs)

    fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*>

    interface Factory {

        fun create(chain: Chain): HydraDxSwapSource
    }
}

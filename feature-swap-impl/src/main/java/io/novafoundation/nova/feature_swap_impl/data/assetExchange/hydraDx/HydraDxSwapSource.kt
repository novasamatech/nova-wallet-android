package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.HydraSwapDirection
import io.novafoundation.nova.feature_swap_core.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

interface HydraDxSwapSource : Identifiable {

    suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs)

    fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*>

    interface Factory {

        fun create(chain: Chain): HydraDxSwapSource
    }
}

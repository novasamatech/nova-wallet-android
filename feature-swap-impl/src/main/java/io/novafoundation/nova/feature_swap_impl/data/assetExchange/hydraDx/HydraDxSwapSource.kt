package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuoteException
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

typealias HydraDxSwapSourceId = String

interface HydraDxSwapSource : Identifiable {

    suspend fun availableSwapDirections(): MultiMapList<FullChainAssetId, HydraSwapDirection>

    suspend fun ExtrinsicBuilder.executeSwap(args: SwapExecuteArgs)

    @Throws(SwapQuoteException::class)
    suspend fun quote(args: HydraDxSwapSourceQuoteArgs): Balance

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

    fun routerPoolTypeFor(params: Map<String, String>): DictEnum.Entry<*>

    interface Factory {

        fun create(chain: Chain): HydraDxSwapSource
    }
}

data class HydraDxSwapSourceQuoteArgs(
    val chainAssetIn: Chain.Asset,
    val chainAssetOut: Chain.Asset,
    val amount: Balance,
    val swapDirection: SwapDirection,
    val params: Map<String, String>
)

interface HydraSwapDirection {

    val from: FullChainAssetId

    val to: FullChainAssetId

    val params: Map<String, String>
}

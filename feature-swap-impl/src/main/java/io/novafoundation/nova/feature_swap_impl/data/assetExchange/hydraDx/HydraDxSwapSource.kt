package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.QuotableEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

typealias HydraDxSwapSourceId = String
typealias HydraDxStandaloneSwapBuilder = ExtrinsicBuilder.(args: SwapExecuteArgs) -> Unit

interface HydraDxSourceEdge : QuotableEdge {

    fun routerPoolArgument(): DictEnum.Entry<*>

    /**
     * Whether hydra swap source is able to perform optimized standalone swap without using Router
     */
    val standaloneSwapBuilder: HydraDxStandaloneSwapBuilder?
}

interface HydraDxSwapSource : Identifiable {

    suspend fun availableSwapDirections(): Collection<QuotableEdge>

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

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

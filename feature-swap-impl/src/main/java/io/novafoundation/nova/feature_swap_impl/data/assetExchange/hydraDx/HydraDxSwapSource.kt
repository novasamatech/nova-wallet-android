package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.QuotableEdge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

typealias HydraDxSwapSourceId = String
typealias HydraDxStandaloneSwapBuilder = ExtrinsicBuilder.(args: AtomicSwapOperationArgs) -> Unit

interface HydraDxSourceEdge : QuotableEdge {

    fun routerPoolArgument(): DictEnum.Entry<*>

    /**
     * Whether hydra swap source is able to perform optimized standalone swap without using Router
     */
    val standaloneSwapBuilder: HydraDxStandaloneSwapBuilder?

    suspend fun debugLabel(): String
}

interface HydraDxSwapSource : Identifiable {

    suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge>

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

    interface Factory {

        fun create(chain: Chain): HydraDxSwapSource
    }
}

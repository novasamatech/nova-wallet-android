package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.feature_swap_core_api.data.types.hydra.HydraDxQuotingSource
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow

interface StandaloneHydraSwap {

    context(ExtrinsicBuilder)
    fun addSwapCall(args: AtomicSwapOperationArgs)

    fun extractReceivedAmount(events: List<GenericEvent.Instance>): Balance
}

interface HydraDxSourceEdge : QuotableEdge {

    fun routerPoolArgument(): DictEnum.Entry<*>

    /**
     * Whether hydra swap source is able to perform optimized standalone swap without using Router
     */
    val standaloneSwap: StandaloneHydraSwap?

    suspend fun debugLabel(): String
}

interface HydraDxSwapSource : Identifiable {

    suspend fun sync()

    suspend fun availableSwapDirections(): Collection<HydraDxSourceEdge>

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

    interface Factory<D : HydraDxQuotingSource<*>> : Identifiable {

        fun create(delegate: D): HydraDxSwapSource
    }
}

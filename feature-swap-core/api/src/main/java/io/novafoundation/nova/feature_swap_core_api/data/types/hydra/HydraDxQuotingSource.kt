package io.novafoundation.nova.feature_swap_core_api.data.types.hydra

import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core_api.data.primitive.SwapQuoting
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface HydraDxQuotingSource<E : QuotableEdge> : Identifiable {

    suspend fun sync()

    suspend fun availableSwapDirections(): Collection<E>

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>

    interface Factory<S : HydraDxQuotingSource<*>> {

        fun create(chain: Chain, host: SwapQuoting.QuotingHost): S
    }
}

package io.novafoundation.nova.feature_swap_core_api.data.primitive

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.feature_swap_core_api.data.primitive.model.QuotableEdge
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow

interface SwapQuoting {

    interface QuotingHost {

        val sharedSubscriptions: SwapQuotingSubscriptions
    }

    /**
     * Perform initial data sync needed to later perform [runSubscriptions]
     * This is separated from [runSubscriptions] since [runSubscriptions] might be io-intense
     * [sync] should be sufficient for [availableSwapDirections] to work
     * whereas [runSubscriptions] should enable [QuotableEdge.quote] method to work
     */
    suspend fun sync()

    suspend fun availableSwapDirections(): List<QuotableEdge>

    suspend fun runSubscriptions(
        userAccountId: AccountId,
        subscriptionBuilder: SharedRequestsBuilder
    ): Flow<Unit>
}

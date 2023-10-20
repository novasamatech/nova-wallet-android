package io.novafoundation.nova.feature_staking_impl.presentation.pools.searchPool

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.selecting.PoolAvailabilityFailure

fun handleSelectPoolValidationFailure(error: PoolAvailabilityFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (error) {
        PoolAvailabilityFailure.PoolIsFull -> TitleAndMessage(
            resourceManager.getString(R.string.pool_full_failure_title),
            resourceManager.getString(R.string.pool_full_failure_message)
        )

        PoolAvailabilityFailure.PoolIsClosed -> TitleAndMessage(
            resourceManager.getString(R.string.pool_inactive_failure_title),
            resourceManager.getString(R.string.pool_inactive_failure_message)
        )
    }
}

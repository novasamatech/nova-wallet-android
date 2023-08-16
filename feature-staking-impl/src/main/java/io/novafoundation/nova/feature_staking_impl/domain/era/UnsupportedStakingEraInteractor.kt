package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.model.StartStakingEraInfo
import kotlinx.coroutines.flow.Flow

class UnsupportedStakingEraInteractor : StakingEraInteractor {

    override fun observeEraInfo(): Flow<StartStakingEraInfo> {
        throw UnsupportedOperationException("Unsupported staking type")
    }
}
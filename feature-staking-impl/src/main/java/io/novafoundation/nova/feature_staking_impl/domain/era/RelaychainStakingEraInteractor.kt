package io.novafoundation.nova.feature_staking_impl.domain.era

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RelaychainStakingEraInteractor(
    private val stakingInteractor: StakingInteractor
) : StakingEraInteractor {

    override fun observeRemainingEraTime(): Flow<Duration> {
        return flowOf { stakingInteractor.getRemainingEraTime().toLong().milliseconds }
    }

    override fun observeUnstakeTime(): Flow<Duration> {
        return flowOf { stakingInteractor.getLockupDuration() }
    }

    override fun observeEraDuration(): Flow<Duration> {
        return flowOf { stakingInteractor.getEraDuration() }
    }
}

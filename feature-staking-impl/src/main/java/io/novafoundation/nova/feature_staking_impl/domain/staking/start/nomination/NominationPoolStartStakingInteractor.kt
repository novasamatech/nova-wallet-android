package io.novafoundation.nova.feature_staking_impl.domain.staking.start.nomination

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.PayoutType
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.StartStakingInteractor
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NominationPoolStartStakingInteractor(
) : StartStakingInteractor {

    override fun observeMaxEarningRate(): Flow<Double> {
        return flowOf(0.0)
    }

    override fun observeMinStake(): Flow<BigInteger> {
        return flowOf(BigInteger.ZERO)
    }

    override fun observeParticipationInGovernance(): Flow<Boolean> {
        return flowOf(false)
    }

    override fun observePayoutType(): Flow<PayoutType> {
        return flowOf(PayoutType.Manual)
    }
}

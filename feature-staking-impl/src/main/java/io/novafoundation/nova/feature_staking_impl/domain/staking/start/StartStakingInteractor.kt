package io.novafoundation.nova.feature_staking_impl.domain.staking.start

import java.math.BigInteger
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

sealed interface PayoutType {

    sealed interface Automatic : PayoutType {

        object Restake : Automatic

        object Payout : Automatic
    }

    object Manual : PayoutType
}

interface StartStakingInteractor {

    fun observeMaxEarningRate(): Flow<Double>

    fun observeMinStake(): Flow<BigInteger>

    fun observePayoutType(): Flow<PayoutType>
}

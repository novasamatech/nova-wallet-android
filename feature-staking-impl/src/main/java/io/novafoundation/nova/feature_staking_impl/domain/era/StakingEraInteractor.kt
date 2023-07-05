package io.novafoundation.nova.feature_staking_impl.domain.era

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface StakingEraInteractor {

    fun observeRemainingEraTime(): Flow<Duration>

    fun observeUnstakeTime(): Flow<Duration>

    fun observeEraDuration(): Flow<Duration>
}

package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ParachainNetworkInfoInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    fun observeNetworkInfo(chainId: ChainId): Flow<NetworkInfo> = flow {
        val maxRewardedDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)
        val systemForcedMinStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)

        val realtimeChanges = currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest {
            val currentCollatorSnapshot = currentRoundRepository.collatorsSnapshot(chainId, it.current)

            val minimumStake = currentCollatorSnapshot.minimumStake(maxRewardedDelegatorsPerCollator, systemForcedMinStake)
            val totalStake = currentCollatorSnapshot.totalStake()
            val nominatorsCount = currentCollatorSnapshot.activeDelegatorsCount(maxRewardedDelegatorsPerCollator)

            roundDurationEstimator.unstakeDurationFlow(chainId).map { lockupPeriodDuration ->
                NetworkInfo(
                    lockupPeriod = lockupPeriodDuration,
                    minimumStake = minimumStake,
                    totalStake = totalStake,
                    stakingPeriod = StakingPeriod.Unlimited,
                    nominatorsCount = nominatorsCount
                )
            }
        }

        emitAll(realtimeChanges)
    }

    private fun AccountIdMap<CollatorSnapshot>.activeDelegatorsCount(maximumRewardedDelegatorsPerCollator: Int, ): Int {
        return values.flatMapTo(mutableSetOf()) { collatorSnapshot ->
            collatorSnapshot.delegations
                .sortedByDescending { it.balance }
                .take(maximumRewardedDelegatorsPerCollator)
                .map { it.owner.toHexString() }
        }.size
    }

    private fun AccountIdMap<CollatorSnapshot>.totalStake() = values.sumByBigInteger { it.total }

    private fun AccountIdMap<CollatorSnapshot>.minimumStake(
        maximumRewardedDelegators: Int,
        systemForcedMinStake: BigInteger,
    ): BigInteger {
        val lastRewardedDelegatorIndex = maximumRewardedDelegators - 1

        val minStakeFromCollators = values.minOfOrNull { collatorSnapshot ->
            collatorSnapshot.delegations.map { it.balance }
                .sortedDescending()
                .getOrElse(lastRewardedDelegatorIndex) { systemForcedMinStake }
        } ?: systemForcedMinStake

        return minStakeFromCollators.max(systemForcedMinStake)
    }
}

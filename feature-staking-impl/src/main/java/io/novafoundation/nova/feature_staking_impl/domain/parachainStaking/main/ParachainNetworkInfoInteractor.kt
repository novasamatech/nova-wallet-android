package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main

import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CollatorSnapshot
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.systemForcedMinStake
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.minimumStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.math.BigInteger
import kotlin.time.ExperimentalTime

class ParachainNetworkInfoInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    fun observeNetworkInfo(chainId: ChainId): Flow<NetworkInfo> = flow {
        val systemForcedMinStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)
        val maxRewardedDelegatorsPerCollator = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)

        val realtimeChanges = currentRoundRepository.currentRoundInfoFlow(chainId).flatMapLatest {
            val currentCollatorSnapshot = currentRoundRepository.collatorsSnapshot(chainId, it.current)

            val minimumStake = currentCollatorSnapshot.minimumStake(systemForcedMinStake, maxRewardedDelegatorsPerCollator)
            val nominatorsCount = currentCollatorSnapshot.activeDelegatorsCount()

            combine(
                currentRoundRepository.totalStakedFlow(chainId),
                roundDurationEstimator.unstakeDurationFlow(chainId)
            ) { totalStaked, lockupPeriodDuration ->
                NetworkInfo(
                    lockupPeriod = lockupPeriodDuration,
                    minimumStake = minimumStake,
                    totalStake = totalStaked,
                    stakingPeriod = StakingPeriod.Unlimited,
                    nominatorsCount = nominatorsCount
                )
            }
        }

        emitAll(realtimeChanges)
    }

    private fun AccountIdMap<CollatorSnapshot>.activeDelegatorsCount(): Int {
        return values.flatMapTo(mutableSetOf()) { collatorSnapshot ->
            collatorSnapshot.delegations.map { it.owner.toHexString() }
        }.size
    }

    private fun AccountIdMap<CollatorSnapshot>.minimumStake(
        systemForcedMinStake: Balance,
        maxRewardedDelegatorsPerCollator: BigInteger
    ): BigInteger {
        val minStakeFromCollators = values.minOfOrNull { collatorSnapshot ->
            collatorSnapshot.minimumStake(systemForcedMinStake, maxRewardedDelegatorsPerCollator)
        } ?: systemForcedMinStake

        return minStakeFromCollators
    }
}

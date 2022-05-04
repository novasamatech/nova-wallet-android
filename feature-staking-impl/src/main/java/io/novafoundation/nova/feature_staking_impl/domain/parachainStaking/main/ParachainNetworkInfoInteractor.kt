package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main

import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.feature_staking_api.domain.api.AccountIdMap
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import java.math.BigInteger

class ParachainNetworkInfoInteractor(
    private val currentRoundRepository: CurrentRoundRepository,
    private val parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
) {

    fun observeNetworkInfo(chainId: ChainId): Flow<NetworkInfo> = flow {
        val maximumRewardedDelegators = parachainStakingConstantsRepository.maxRewardedDelegatorsPerCollator(chainId)
        val systemForcedMinStake = parachainStakingConstantsRepository.systemForcedMinStake(chainId)

        val realtimeChanges = currentRoundRepository.currentRoundInfoFlow(chainId).mapLatest {
            val currentCollatorSnapshot = currentRoundRepository.collatorsSnapshot(chainId, it.current)

            NetworkInfo(
                // TODO
                lockupPeriodInDays = 0,
                minimumStake = currentCollatorSnapshot.minimumStake(maximumRewardedDelegators, systemForcedMinStake),
                totalStake = currentCollatorSnapshot.totalStake(),
                stakingPeriod = StakingPeriod.Unlimited,
                nominatorsCount = currentCollatorSnapshot.nominatorsCount()
            )
        }

        emitAll(realtimeChanges)
    }

    private fun AccountIdMap<CollatorSnapshot>.nominatorsCount(): Int {
        return values.flatMapTo(mutableSetOf()) { collatorSnapshot ->
            collatorSnapshot.delegations.map { it.owner.toHexString() }
        }.size
    }

    private fun AccountIdMap<CollatorSnapshot>.totalStake() = values.sumByBigInteger { it.total }

    private fun AccountIdMap<CollatorSnapshot>.minimumStake(
        maximumRewardedDelegators: Int,
        systemForcedMinStake: BigInteger,
    ) : BigInteger {
        val lastRewardedDelegatorIndex = maximumRewardedDelegators - 1

        val minStakeFromCollators =  values.minOfOrNull { collatorSnapshot ->
            collatorSnapshot.delegations.map { it.balance }
                .sortedDescending()
                .getOrElse(lastRewardedDelegatorIndex) { systemForcedMinStake }
        } ?: systemForcedMinStake

        return minStakeFromCollators.max(systemForcedMinStake)
    }
}

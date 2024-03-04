package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.participatingBondedPoolStateFlow
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.poolState.isPoolStaking
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

interface NominationPoolStakeSummaryInteractor {

    fun stakeSummaryFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<StakeSummary<PoolMemberStatus>>
}

class RealNominationPoolStakeSummaryInteractor(
    private val stakingSharedComputation: StakingSharedComputation,
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val poolAccountDerivation: PoolAccountDerivation,
) : NominationPoolStakeSummaryInteractor {

    override fun stakeSummaryFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<StakeSummary<PoolMemberStatus>> = flowOfAll {
        val chainId = stakingOption.assetWithChain.chain.id
        val poolStash = poolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)

        combineTransform(
            nominationPoolSharedComputation.participatingBondedPoolStateFlow(poolStash, poolMember.poolId, chainId, sharedComputationScope),
            nominationPoolSharedComputation.participatingPoolNominationsFlow(poolStash, poolMember.poolId, chainId, sharedComputationScope),
            stakingSharedComputation.electedExposuresWithActiveEraFlow(chainId, sharedComputationScope)
        ) { bondedPoolState, poolNominations, (eraStakers, activeEra) ->
            val activeStaked = bondedPoolState.amountOf(poolMember.points)

            val stakeSummaryFlow = flow {
                determineStakeStatus(stakingOption, eraStakers, activeEra, poolNominations, poolStash, poolMember, sharedComputationScope)
            }
                .map { status -> StakeSummary(status, activeStaked) }

            emitAll(stakeSummaryFlow)
        }
    }

    private suspend fun FlowCollector<PoolMemberStatus>.determineStakeStatus(
        stakingOption: StakingOption,
        eraStakers: AccountIdMap<Exposure>,
        activeEra: EraIndex,
        poolNominations: Nominations?,
        poolStash: AccountId,
        poolMember: PoolMember,
        sharedComputationScope: CoroutineScope
    ) {
        when {
            poolMember.points.value.isZero -> emit(PoolMemberStatus.Inactive)

            eraStakers.isPoolStaking(poolStash, poolNominations) -> emit(PoolMemberStatus.Active)

            poolNominations != null && poolNominations.isWaiting(activeEra) -> {
                val nominationsEffectiveEra = poolNominations.submittedInEra + EraIndex.ONE

                val statusFlow = stakingSharedComputation.eraCalculatorFlow(stakingOption, sharedComputationScope).map { eraTimerCalculator ->
                    val waitingTime = eraTimerCalculator.calculate(nominationsEffectiveEra)

                    PoolMemberStatus.Waiting(waitingTime.toLong().milliseconds)
                }

                emitAll(statusFlow)
            }

            else -> emit(PoolMemberStatus.Inactive)
        }
    }
}

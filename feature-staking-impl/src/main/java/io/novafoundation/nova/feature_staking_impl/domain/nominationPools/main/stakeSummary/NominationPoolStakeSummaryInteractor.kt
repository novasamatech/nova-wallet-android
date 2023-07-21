package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.model.AccountIdMap
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Exposure
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.bondedAccountOf
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.model.StakeSummary
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.BondedPoolState
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.time.Duration.Companion.milliseconds

interface NominationPoolStakeSummaryInteractor {

    fun stakeSummaryFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<StakeSummary<PoolMemberStatus>>
}

class RealNominationPoolStakeSummaryInteractor(
    private val nominationPoolStateRepository: NominationPoolStateRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val noPoolAccountDerivation: PoolAccountDerivation,
    private val eraTimeCalculatorFactory: EraTimeCalculatorFactory,
) : NominationPoolStakeSummaryInteractor {

    override fun stakeSummaryFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<StakeSummary<PoolMemberStatus>> = flowOfAll {
        val chainId = stakingOption.assetWithChain.chain.id
        val poolStash = noPoolAccountDerivation.bondedAccountOf(poolMember.poolId, chainId)

        combine(
            nominationPoolStateRepository.observeParticipatingBondedPool(poolMember.poolId, chainId),
            nominationPoolStateRepository.observeParticipatingPoolNominations(poolStash, chainId),
            nominationPoolStateRepository.observeParticipatingBondedBalance(poolStash, chainId),
            stakingSharedComputation.electedExposuresWithActiveEraFlow(chainId, sharedComputationScope)
        ) { bondedPool, poolNominations, bondedPoolBalance, (eraStakers, activeEra) ->
            val bondedPoolState = BondedPoolState(bondedPool, bondedPoolBalance)

            StakeSummary(
                totalStaked = bondedPoolState.amountOf(poolMember.points),
                status = determineStakeStatus(stakingOption, eraStakers, activeEra, poolNominations, poolStash)
            )
        }
    }

    private suspend fun determineStakeStatus(
        stakingOption: StakingOption,
        eraStakers: AccountIdMap<Exposure>,
        activeEra: EraIndex,
        poolNominations: Nominations?,
        poolStash: AccountId
    ): PoolMemberStatus {
        return when {
            eraStakers.isPoolStaking(poolStash, poolNominations) -> PoolMemberStatus.Active

            poolNominations != null && poolNominations.isWaiting(activeEra) -> {
                val nominationsEffectiveEra = poolNominations.submittedInEra + EraIndex.ONE
                val eraTimeCalculator = eraTimeCalculatorFactory.create(stakingOption)
                val waitingTime = eraTimeCalculator.calculate(nominationsEffectiveEra)

                PoolMemberStatus.Waiting(waitingTime.toLong().milliseconds)
            }

            else -> PoolMemberStatus.Inactive
        }
    }

    private fun AccountIdMap<Exposure>.isPoolStaking(poolStash: AccountId, poolNominations: Nominations?): Boolean {
        // whereas pool might still stake without nominations if era it has chilled in haven't yet finished
        // we still mark it as Inactive to warn user preemptively
        if (poolNominations == null) return false

        return poolNominations.targets.any { validator ->
            val accountIdHex = validator.toHexString()
            val exposure = get(accountIdHex) ?: return@any false

            exposure.others.any { it.who.contentEquals(poolStash) }
        }
    }
}

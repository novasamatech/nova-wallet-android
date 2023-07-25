package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings

import io.novafoundation.nova.common.utils.formatting.toTimerValue
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.EraRedeemable
import io.novafoundation.nova.feature_staking_api.domain.model.isRedeemableIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.UnbondingPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.EraTimeCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.calculateDurationTill
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope.coroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface NominationPoolUnbondingsInteractor {

    fun unbondingsFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings>
}

class RealNominationPoolUnbondingsInteractor(
    private val nominationPoolUnbondRepository: NominationPoolUnbondRepository,
    private val stakingSharedComputation: StakingSharedComputation,
    private val eraTimeCalculatorFactory: EraTimeCalculatorFactory,
) : NominationPoolUnbondingsInteractor {

    override fun unbondingsFlow(
        poolMember: PoolMember,
        stakingOption: StakingOption,
        sharedComputationScope: CoroutineScope,
    ): Flow<Unbondings> {
        val chainId = stakingOption.assetWithChain.chain.id
        return combine(
            stakingSharedComputation.activeEraFlow(chainId, sharedComputationScope),
            nominationPoolUnbondRepository.unbondingPoolsFlow(poolMember.poolId, chainId)
        ) { activeEraIndex, unbondingPools ->
            val unbondings = unbondingPools.unbondingsFor(poolMember, activeEraIndex, stakingOption)

            Unbondings.from(unbondings, rebondPossible = false)
        }
    }

    private suspend fun UnbondingPools?.unbondingsFor(
        poolMember: PoolMember,
        activeEra: EraIndex,
        stakingOption: StakingOption
    ): List<Unbonding> {
        if (this == null) return emptyList()

        val eraTimeCalculator by CoroutineScope(coroutineContext).lazyAsync { eraTimeCalculatorFactory.create(stakingOption) }

        return poolMember.unbondingEras.entries.mapIndexed { index, (unbondEra, unbondPoints) ->
            val unbondingPool = getPool(unbondEra)
            val unbondBalance = unbondingPool.amountOf(unbondPoints)
            val isRedeemable = EraRedeemable(unbondEra).isRedeemableIn(activeEra)

            val status = if (isRedeemable) {
                Unbonding.Status.Redeemable
            } else {
                val timer = eraTimeCalculator.await().calculateDurationTill(unbondEra).toTimerValue()

                Unbonding.Status.Unbonding(timer)
            }

            Unbonding(id = index.toString(), amount = unbondBalance, status = status)
        }
    }

    private fun UnbondingPools.getPool(era: EraIndex): UnbondingPool {
        return withEra[era] ?: noEra
    }
}

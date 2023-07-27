package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond

import io.novafoundation.nova.common.utils.formatting.toTimerValue
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.EraRedeemable
import io.novafoundation.nova.feature_staking_api.domain.model.isRedeemableIn
import io.novafoundation.nova.feature_staking_api.domain.model.isUnbondingIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.common.calculateDurationTill
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

fun StakingSharedComputation.constructUnbondingList(
    eraRedeemables: List<EraRedeemable>,
    activeEra: EraIndex,
    stakingOption: StakingOption,
    sharedComputationScope: CoroutineScope,
): Flow<List<Unbonding>> {
    val stillUnbondingCount = eraRedeemables.count { it.isUnbondingIn(activeEra) }

    if (stillUnbondingCount == 0) {
        val allRedeemable = eraRedeemables.mapIndexed { index, eraRedeemable ->
            Unbonding(
                id = index.toString(),
                amount = eraRedeemable.amount,
                status = Unbonding.Status.Redeemable
            )
        }

        return flowOf(allRedeemable)
    }

    return eraCalculatorFlow(stakingOption, sharedComputationScope).map { eraTimeCalculator ->
        eraRedeemables.mapIndexed { index, eraRedeemable ->
            val isRedeemable = eraRedeemable.isRedeemableIn(activeEra)

            val status = if (isRedeemable) {
                Unbonding.Status.Redeemable
            } else {
                val timer = eraTimeCalculator.calculateDurationTill(eraRedeemable.redeemEra).toTimerValue()

                Unbonding.Status.Unbonding(timer)
            }

            Unbonding(
                id = index.toString(),
                amount = eraRedeemable.amount,
                status = status
            )
        }
    }
}

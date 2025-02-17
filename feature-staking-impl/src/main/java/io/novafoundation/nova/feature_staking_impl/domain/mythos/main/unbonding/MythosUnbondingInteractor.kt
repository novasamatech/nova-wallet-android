package io.novafoundation.nova.feature_staking_impl.domain.mythos.main.unbonding

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythReleaseRequest
import io.novafoundation.nova.feature_staking_impl.domain.model.Unbonding
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondingList
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.Unbondings
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.from
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimatorFlow
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.isBlockedPassed
import io.novafoundation.nova.runtime.util.timerUntil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

interface MythosUnbondingInteractor {

    context(ComputationalScope)
    fun unbondingsFlow(
        delegatorState: MythosDelegatorState.Staked,
        stakingOption: StakingOption
    ): Flow<Unbondings>
}

@FeatureScope
class RealMythosUnbondingInteractor @Inject constructor(
    private val chainStateRepository: ChainStateRepository,
    private val accountRepository: AccountRepository,
    private val mythosSharedComputation: MythosSharedComputation,
) : MythosUnbondingInteractor {

    context(ComputationalScope)
    override fun unbondingsFlow(delegatorState: MythosDelegatorState.Staked, stakingOption: StakingOption): Flow<Unbondings> {
        val chainId = stakingOption.chain.id

        return flowOfAll {
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(stakingOption.chain).intoKey()

            combine(
                mythosSharedComputation.releaseQueuesFlow(chainId, accountId),
                chainStateRepository.blockDurationEstimatorFlow(chainId)
            ) { releaseQueues, durationEstimator ->
                val unbondingList = releaseQueues.toUnbondingList(durationEstimator)
                Unbondings.from(unbondingList, rebondPossible = false)
            }
        }
    }

    private fun List<MythReleaseRequest>.toUnbondingList(durationEstimator: BlockDurationEstimator): UnbondingList {
        return mapIndexed { index, releaseRequest ->
            Unbonding(
                id = index.toString(),
                amount = releaseRequest.amount,
                status = releaseRequest.unbondingStatus(durationEstimator)
            )
        }
    }

    private fun MythReleaseRequest.unbondingStatus(durationEstimator: BlockDurationEstimator): Unbonding.Status {
        return if (durationEstimator.isBlockedPassed(block)) {
            Unbonding.Status.Redeemable
        } else {
            val timer = durationEstimator.timerUntil(block)

            Unbonding.Status.Unbonding(timer)
        }
    }
}

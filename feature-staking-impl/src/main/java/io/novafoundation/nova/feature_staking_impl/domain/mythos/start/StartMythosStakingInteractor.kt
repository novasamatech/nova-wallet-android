package io.novafoundation.nova.feature_staking_impl.domain.mythos.start

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.StakingIntent
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.lock
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.stake
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.stakedCollatorsCount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.DelegationsLimit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface StartMythosStakingInteractor {

    context(ComputationalScope)
    suspend fun minStake(): Balance

    suspend fun estimateFee(
        currentState: MythosDelegatorState,
        candidate: AccountIdKey,
        amount: Balance
    ): Fee

    suspend fun stake(
        currentState: MythosDelegatorState,
        candidate: AccountIdKey,
        amount: Balance
    ): Result<Unit>

    suspend fun checkDelegationsLimit(
        delegatorState: MythosDelegatorState
    ): DelegationsLimit
}

@FeatureScope
class RealStartMythosStakingInteractor @Inject constructor(
    private val mythosSharedComputation: MythosSharedComputation,
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: MythosStakingRepository
) : StartMythosStakingInteractor {

    context(ComputationalScope)
    override suspend fun minStake(): Balance {
        return mythosSharedComputation.minStakeFlow(stakingSharedState.chainId()).first()
    }

    override suspend fun estimateFee(
        currentState: MythosDelegatorState,
        candidate: AccountIdKey,
        amount: Balance
    ): Fee {
        val chain = stakingSharedState.chain()
        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            stakeMore(candidate, amount)
        }
    }

    override suspend fun stake(currentState: MythosDelegatorState, candidate: AccountIdKey, amount: Balance): Result<Unit> {
        val chain = stakingSharedState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            stakeMore(candidate, amount)
        }
            .requireOk()
            .coerceToUnit()
    }

    override suspend fun checkDelegationsLimit(delegatorState: MythosDelegatorState): DelegationsLimit {
        return withContext(Dispatchers.IO) {
            val chainId = stakingSharedState.chainId()
            val maxCandidatesPerCollator = stakingRepository.maxCandidatesPerDelegator(chainId)

            if (delegatorState.stakedCollatorsCount() < maxCandidatesPerCollator) {
                DelegationsLimit.NotReached
            } else {
                DelegationsLimit.Reached(maxCandidatesPerCollator)
            }
        }
    }

    private fun ExtrinsicBuilder.stakeMore(
        candidate: AccountIdKey,
        amount: Balance
    ) {
        collatorStaking.lock(amount)

        collatorStaking.stake(listOf(StakingIntent(candidate, amount)))
    }
}

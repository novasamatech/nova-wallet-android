package io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.unlock
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.unstakeFrom
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.delegationAmountTo
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.rewards.MythosClaimPendingRewardsUseCase
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import javax.inject.Inject

interface UnbondMythosStakingInteractor {

    suspend fun estimateFee(delegatorState: MythosDelegatorState, candidate: AccountIdKey): Fee

    suspend fun unbond(delegatorState: MythosDelegatorState, candidate: AccountIdKey): Result<ExtrinsicExecutionResult>
}

@FeatureScope
class RealUnbondMythosStakingInteractor @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val claimPendingRewardsUseCase: MythosClaimPendingRewardsUseCase,
) : UnbondMythosStakingInteractor {

    override suspend fun estimateFee(delegatorState: MythosDelegatorState, candidate: AccountIdKey): Fee {
        val chain = stakingSharedState.chain()
        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            claimPendingRewardsUseCase.claimPendingRewards(chain)

            unbond(delegatorState, candidate)
        }
    }

    override suspend fun unbond(delegatorState: MythosDelegatorState, candidate: AccountIdKey): Result<ExtrinsicExecutionResult> {
        val chain = stakingSharedState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            claimPendingRewardsUseCase.claimPendingRewards(chain)

            unbond(delegatorState, candidate)
        }
            .requireOk()
    }

    private fun ExtrinsicBuilder.unbond(
        delegatorState: MythosDelegatorState,
        candidate: AccountIdKey,
    ) {
        collatorStaking.unstakeFrom(candidate)

        val stakedAmount = delegatorState.delegationAmountTo(candidate)
        collatorStaking.unlock(stakedAmount)
    }
}

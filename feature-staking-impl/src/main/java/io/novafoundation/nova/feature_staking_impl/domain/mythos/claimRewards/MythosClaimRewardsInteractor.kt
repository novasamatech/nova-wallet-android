package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.claimRewards
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface MythosClaimRewardsInteractor {

    context(ComputationalScope)
    fun pendingRewardsFlow(): Flow<Balance>

    suspend fun estimateFee(): Fee

    suspend fun claimRewards(): Result<Unit>
}

@FeatureScope
class RealMythosClaimRewardsInteractor @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val delegatorStateUseCase: MythosDelegatorStateUseCase,
    private val accountRepository: AccountRepository,
    private val userStakeRepository: MythosUserStakeRepository,
) : MythosClaimRewardsInteractor {

    context(ComputationalScope)
    override fun pendingRewardsFlow(): Flow<Balance> {
        return delegatorStateUseCase.currentDelegatorState()
            .filterIsInstance<MythosDelegatorState.Staked>()
            .distinctUntilChangedBy { it.userStakeInfo.maybeLastRewardSession }
            .mapLatest {
                val chain = stakingSharedState.chain()
                val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain).intoKey()

                userStakeRepository.getpPendingRewards(chain.id, accountId)
            }
    }

    override suspend fun estimateFee(): Fee {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            collatorStaking.claimRewards()
        }
    }

    override suspend fun claimRewards(): Result<Unit> {
        val chain = stakingSharedState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            collatorStaking.claimRewards()
        }
            .requireOk()
            .coerceToUnit()
    }
}

package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.splitByWeights
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdKeyOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.StakingIntent
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.claimRewards
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.lock
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.stake
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface MythosClaimRewardsInteractor {

    context(ComputationalScope)
    fun pendingRewardsFlow(): Flow<Balance>

    suspend fun initialShouldRestakeSetting(): Boolean

    suspend fun estimateFee(
        claimableRewards: Balance,
        shouldRestake: Boolean
    ): Fee

    suspend fun claimRewards(
        claimableRewards: Balance,
        shouldRestake: Boolean
    ): Result<Unit>
}

private const val SHOULD_RESTAKE_DEFAULT = true

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
            }.distinctUntilChanged()
    }

    override suspend fun initialShouldRestakeSetting(): Boolean {
        return userStakeRepository.lastShouldRestakeSelection() ?: SHOULD_RESTAKE_DEFAULT
    }

    override suspend fun estimateFee(
        claimableRewards: Balance,
        shouldRestake: Boolean
    ): Fee {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            claimRewards(claimableRewards, shouldRestake)
        }
    }

    override suspend fun claimRewards(
        claimableRewards: Balance,
        shouldRestake: Boolean
    ): Result<Unit> {
        val chain = stakingSharedState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            claimRewards(claimableRewards, shouldRestake)
        }
            .requireOk()
            .onSuccess { userStakeRepository.setLastShouldRestakeSelection(shouldRestake) }
            .coerceToUnit()
    }

    private suspend fun ExtrinsicBuilder.claimRewards(
        claimableRewards: Balance,
        shouldRestake: Boolean
    ) {
        collatorStaking.claimRewards()

        val canRestakeViaBondMore = isAutoCompoundDisabled()
        if (shouldRestake && canRestakeViaBondMore) {
            restakeRewards(claimableRewards)
        }
    }

    private suspend fun ExtrinsicBuilder.restakeRewards(claimableRewards: Balance) {
        collatorStaking.lock(claimableRewards)

        val newStakes = determineNewCollatorStakes(claimableRewards)
        collatorStaking.stake(newStakes)
    }

    private suspend fun isAutoCompoundDisabled(): Boolean {
        val chain = stakingSharedState.chain()
        val accountId = accountRepository.requireIdKeyOfSelectedMetaAccountIn(chain)

        val autoCompoundPercentage = userStakeRepository.getAutoCompoundPercentage(chain.id, accountId)
        return autoCompoundPercentage.isZero
    }

    private suspend fun determineNewCollatorStakes(claimedRewards: Balance): List<StakingIntent> {
        val delegations = delegatorStateUseCase.getUserDelegations()
        val splitWeights = delegations.map { (_, delegation) -> delegation.stake }

        val rewardAllocations = claimedRewards.splitByWeights(splitWeights)

        return delegations.keys.zip(rewardAllocations).map { (collatorId, stakeMoreAmount) ->
            StakingIntent(collatorId, stakeMoreAmount)
        }
    }
}

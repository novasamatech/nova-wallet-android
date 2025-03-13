package io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards

import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdKeyOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.claimRewards
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.calls.setAutoCompoundPercentage
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model.MythosAutoCompoundState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model.MythosAutoCompoundState.Configurable.NotSet
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model.MythosAutoCompoundState.Configurable.Restake
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model.MythosAutoCompoundState.Configurable.Transferable
import io.novafoundation.nova.feature_staking_impl.domain.mythos.claimRewards.model.isConfigurable
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.activeStake
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.state.chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface MythosClaimRewardsInteractor {

    context(ComputationalScope)
    fun pendingRewardsFlow(): Flow<Balance>

    context(ComputationalScope)
    fun currentAutoCompoundState(): Flow<MythosAutoCompoundState>

    suspend fun estimateFee(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ): Fee

    suspend fun claimRewards(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ): Result<Unit>
}

@FeatureScope
class RealMythosClaimRewardsInteractor @Inject constructor(
    private val stakingSharedState: StakingSharedState,
    private val extrinsicService: ExtrinsicService,
    private val delegatorStateUseCase: MythosDelegatorStateUseCase,
    private val accountRepository: AccountRepository,
    private val stakeRepository: MythosStakingRepository,
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

    context(ComputationalScope)
    override fun currentAutoCompoundState(): Flow<MythosAutoCompoundState> {
        return flowOfAll {
            val chain = stakingSharedState.chain()
            val autoCompoundThreshold = stakeRepository.autoCompoundThreshold(chain.id)
            val accountId = accountRepository.requireIdKeyOfSelectedMetaAccountIn(chain)
            val userModifiedSettingsPreviously = userStakeRepository.userModifiedCompoundPercentageViaNova()

            combine(
                delegatorStateUseCase.currentDelegatorState().map { it.activeStake },
                userStakeRepository.getAutoCompoundPercentage(chain.id, accountId)
            ) { userStake, userCompoundPercentage ->
                MythosAutoCompoundState.from(
                    userStake = userStake,
                    threshold = autoCompoundThreshold,
                    userCompoundFraction = userCompoundPercentage,
                    userModifiedStateInPast = userModifiedSettingsPreviously
                )
            }
        }
    }

    override suspend fun estimateFee(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ): Fee {
        val chain = stakingSharedState.chain()

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            claimRewards(currentAutoCompoundState, shouldRestake)
        }
    }

    override suspend fun claimRewards(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ): Result<Unit> {
        val chain = stakingSharedState.chain()

        return extrinsicService.submitExtrinsicAndAwaitExecution(chain, TransactionOrigin.SelectedWallet) {
            claimRewards(currentAutoCompoundState, shouldRestake)
        }
            .requireOk()
            .coerceToUnit()
    }

    private fun ExtrinsicBuilder.claimRewards(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ) {
        // TODO `maybe_set_compound_percentage` call expects all rewards to be claimed, so we do it first
        // even if the compound percentage will only be applied to the next claim
        // this should be changed once Mythos changes the implementation
        collatorStaking.claimRewards()

        maybeSetCompoundPercentage(currentAutoCompoundState, shouldRestake)
    }

    private fun ExtrinsicBuilder.maybeSetCompoundPercentage(
        currentAutoCompoundState: MythosAutoCompoundState,
        shouldRestake: Boolean
    ) {
        if (!currentAutoCompoundState.isConfigurable()) return

        val newState = MythosAutoCompoundState.Configurable.fromSelected(shouldRestake)
        val needsToChangeState = newState != currentAutoCompoundState

        if (needsToChangeState) {
            setCompoundPercentage(shouldRestake)
        }
    }

    private fun ExtrinsicBuilder.setCompoundPercentage(shouldRestake: Boolean) {
        val percentage = if (shouldRestake) 100.percents else 0.percents
        collatorStaking.setAutoCompoundPercentage(percentage)
    }
}

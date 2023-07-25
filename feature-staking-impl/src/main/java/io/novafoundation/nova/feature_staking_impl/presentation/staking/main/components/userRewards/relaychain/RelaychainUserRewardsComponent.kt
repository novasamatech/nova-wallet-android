package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.period.mapRewardPeriodToString
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onStart
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import kotlinx.coroutines.flow.transformLatest

class RelaychainUserRewardsComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = RelaychainUserRewardsComponent(
        stakingInteractor = stakingInteractor,
        stakingOption = stakingOption,
        hostContext = hostContext,
        stakingSharedComputation = stakingSharedComputation,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager
    )
}

private class RelaychainUserRewardsComponent(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) : BaseRewardComponent(hostContext) {

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = stakingOption.assetWithChain,
        scope = hostContext.scope
    )

    private val rewardPeriodState = rewardPeriodsInteractor.observeRewardPeriod(
        stakingOption.assetWithChain.chain,
        stakingOption.assetWithChain.asset,
        stakingOption.assetWithChain.asset.supportedStakingOptions().first()
    )

    private val rewardAmountState = selectedAccountStakingStateFlow.transformLatest { stakingState ->
        if (stakingState is StakingState.Stash) {
            emitAll(rewardsFlow(stakingState))
        } else {
            emit(null)
        }
    }
        .onStart { emit(null) }
        .shareInBackground()

    override val state: Flow<UserRewardsState?> = combine(
        rewardAmountState,
        rewardPeriodState
    ) { rewardAmount, rewardPeriod ->
        rewardAmount?.let {
            UserRewardsState(
                rewardAmount,
                mapRewardPeriodToString(resourceManager, rewardPeriod)
            )
        }
    }

    init {
        syncStakingRewards()
    }

    private fun rewardsFlow(stakingState: StakingState.Stash): Flow<LoadingState<AmountModel>> = combine(
        stakingInteractor.observeUserRewards(stakingState, stakingOption.assetWithChain.chain, stakingOption.assetWithChain.asset),
        hostContext.assetFlow
    ) { totalReward, asset ->
        mapAmountToAmountModel(totalReward, asset)
    }.withLoading()

    private fun syncStakingRewards() {
        val stashAccountStakingStateFlow = selectedAccountStakingStateFlow.filterIsInstance<StakingState.Stash>()
        combine(stashAccountStakingStateFlow, rewardPeriodState) { staking, period ->
            stakingInteractor.syncStakingRewards(staking, stakingOption.assetWithChain.chain, stakingOption.assetWithChain.asset, period)
        }
            .inBackground()
            .launchIn(this)
    }
}

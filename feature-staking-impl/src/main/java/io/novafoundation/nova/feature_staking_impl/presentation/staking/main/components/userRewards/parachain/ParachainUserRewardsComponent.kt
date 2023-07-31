package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards.ParachainStakingUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.period.mapRewardPeriodToString
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ParachainUserRewardsComponentFactory(
    private val interactor: ParachainStakingUserRewardsInteractor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = ParachainUserRewardsComponent(
        interactor = interactor,
        delegatorStateUseCase = delegatorStateUseCase,
        stakingOption = stakingOption,
        hostContext = hostContext,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager
    )
}

private class ParachainUserRewardsComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingUserRewardsInteractor,
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) : BaseRewardComponent(hostContext) {

    private val rewardPeriodState = rewardPeriodsInteractor.observeRewardPeriod(stakingOption)
        .shareInBackground()

    private val rewardAmountState = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = stakingOption.assetWithChain,
        stateProducer = ::rewardsFlow,
        onDelegatorChange = ::syncStakingRewards
    )

    override val state = combine(
        rewardAmountState,
        rewardPeriodState
    ) { rewardAmount, rewardPeriod ->
        rewardAmount?.let {
            UserRewardsState(
                amount = rewardAmount,
                claimableRewards = null,
                selectedRewardPeriod = mapRewardPeriodToString(resourceManager, rewardPeriod)
            )
        }
    }
        .shareInBackground()

    private fun rewardsFlow(delegatorState: DelegatorState.Delegator): Flow<AmountModel> = combine(
        interactor.observeRewards(delegatorState, stakingOption),
        hostContext.assetFlow
    ) { totalReward, asset ->
        mapAmountToAmountModel(totalReward, asset)
    }

    private fun syncStakingRewards(delegatorState: DelegatorState.Delegator) {
        rewardPeriodState.onEach {
            interactor.syncRewards(delegatorState, stakingOption, it)
        }.launchIn(this)
    }
}

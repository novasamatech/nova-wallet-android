package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.mythos

import io.novafoundation.nova.common.presentation.flatMap
import io.novafoundation.nova.common.presentation.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.userRewards.MythosUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.period.mapRewardPeriodToString
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos.loadUserStakeState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn

class MythosUserRewardsComponentFactory(
    private val router: MythosStakingRouter,
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosUserRewardsInteractor,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = MythosUserRewardsComponent(
        interactor = interactor,
        stakingOption = stakingOption,
        hostContext = hostContext,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager,
        router = router,
        mythosSharedComputation = mythosSharedComputation,
        amountFormatter = amountFormatter
    )
}

private class MythosUserRewardsComponent(
    private val router: MythosStakingRouter,
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosUserRewardsInteractor,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val amountFormatter: AmountFormatter
) : BaseRewardComponent(hostContext) {

    private val stateDiffing = { old: MythosDelegatorState.Staked, new: MythosDelegatorState.Staked ->
        old.userStakeInfo.maybeLastRewardSession == new.userStakeInfo.maybeLastRewardSession
    }

    private val rewardPeriodState = rewardPeriodsInteractor.observeRewardPeriod(stakingOption)
        .shareInBackground()

    private val nominationPoolRewardsState = mythosSharedComputation.loadUserStakeState(
        hostContext = hostContext,
        stateProducer = { interactor.rewardsFlow(stakingOption) },
        distinctUntilChanged = stateDiffing
    )
        .shareInBackground()

    override fun onAction(action: UserRewardsAction) {
        if (action is UserRewardsAction.ClaimRewardsClicked) {
            router.openClaimRewards()
        } else {
            super.onAction(action)
        }
    }

    init {
        launchRewardsSync()
    }

    override val state = combine(
        nominationPoolRewardsState,
        rewardPeriodState,
        hostContext.assetFlow
    ) { rewardsState, rewardPeriod, asset ->
        if (rewardsState == null) return@combine null

        val total = rewardsState.flatMap { poolRewards ->
            poolRewards.total.map { total -> amountFormatter.formatAmountToAmountModel(total, asset) }
        }
        val claimable = rewardsState.flatMap { poolRewards ->
            poolRewards.claimable.map { claimable ->
                UserRewardsState.ClaimableRewards(
                    amountModel = amountFormatter.formatAmountToAmountModel(claimable, asset),
                    canClaim = claimable.isPositive()
                )
            }
        }

        UserRewardsState(
            amount = total,
            claimableRewards = claimable,
            iconRes = R.drawable.ic_direct_staking_banner_picture,
            selectedRewardPeriod = mapRewardPeriodToString(resourceManager, rewardPeriod)
        )
    }
        .shareInBackground()

    private fun launchRewardsSync() {
        val stateUpdates = mythosSharedComputation.delegatorStateFlow()
            .filterIsInstance<MythosDelegatorState.Staked>()
            .distinctUntilChanged(stateDiffing)

        combine(
            rewardPeriodState,
            stateUpdates
        ) { rewardPeriod, _ ->
            interactor.syncTotalRewards(stakingOption, rewardPeriod)
        }.launchIn(this)
    }
}

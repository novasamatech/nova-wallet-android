package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.parachain.ParachainUserRewardsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain.RelaychainUserRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

typealias UserRewardsComponent = StatefullComponent<UserRewardsState, UserRewardsEvent, UserRewardsAction>

class UserRewardsState(
    val amount: LoadingState<AmountModel>,
    val selectedRewardPeriod: String
)

sealed class UserRewardsEvent {

    object UserRewardPeriodClicked : UserRewardsEvent()
}

sealed class UserRewardsAction {

    object UserRewardPeriodClicked : UserRewardsAction()
}

class UserRewardsComponentFactory(
    private val relaychainComponentFactory: RelaychainUserRewardsComponentFactory,
    private val parachainComponentFactory: ParachainUserRewardsComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): UserRewardsComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = parachainComponentFactory::create,
        hostContext = hostContext
    )
}

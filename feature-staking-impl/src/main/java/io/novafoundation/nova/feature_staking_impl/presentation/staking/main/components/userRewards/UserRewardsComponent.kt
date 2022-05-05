package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain.RelaychainUserRewardsComponentFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

typealias UserRewardsComponent = StatefullComponent<UserRewardsState, UserRewardsEvent, UserRewardsAction>

typealias UserRewardsState = LoadingState<AmountModel>

typealias UserRewardsAction = Nothing
typealias UserRewardsEvent = Nothing

class UserRewardsComponentFactory(
    private val relaychainComponentFactory: RelaychainUserRewardsComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): UserRewardsComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = { _, _ -> UnsupportedComponent() },
        hostContext = hostContext
    )
}

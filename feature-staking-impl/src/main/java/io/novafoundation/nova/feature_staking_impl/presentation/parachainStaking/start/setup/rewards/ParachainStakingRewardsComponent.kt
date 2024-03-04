package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards

import io.novafoundation.nova.common.utils.firstNotNull
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.Action
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.State
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal

interface ParachainStakingRewardsComponent : StatefullComponent<State, Nothing, Action>, CoroutineScope {

    class State(
        val rewardsConfiguration: RewardsConfiguration,
        val rewardEstimation: RewardEstimation
    )

    data class RewardsConfiguration(
        val collator: AccountId?,
        val amount: BigDecimal
    )

    sealed class Action {

        class ConfigurationUpdated(val newConfiguration: RewardsConfiguration) : Action()
    }
}

@JvmName("connectWithAmount")
infix fun ParachainStakingRewardsComponent.connectWith(amountFlow: Flow<BigDecimal>) {
    amountFlow.onEach { newAmount ->
        val rewardsConfiguration = state.firstNotNull().rewardsConfiguration
        val newConfiguration = rewardsConfiguration.copy(amount = newAmount)

        onAction(Action.ConfigurationUpdated(newConfiguration))
    }.launchIn(this)
}

infix fun ParachainStakingRewardsComponent.connectWith(selectedCollatorIdFlow: Flow<AccountId?>) {
    selectedCollatorIdFlow.onEach { newCollatorId ->
        val rewardsConfiguration = state.firstNotNull().rewardsConfiguration
        val newConfiguration = rewardsConfiguration.copy(collator = newCollatorId)

        onAction(Action.ConfigurationUpdated(newConfiguration))
    }.launchIn(this)
}

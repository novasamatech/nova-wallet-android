package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards

import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.Action
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.rewards.ParachainStakingRewardsComponent.State
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal

interface ParachainStakingRewardsComponent : StatefullComponent<State, Nothing, Action>, CoroutineScope {

    class State(val rewardEstimation: RewardEstimation)

    data class RewardsConfiguration(
        val collator: AccountId?,
        val amount: BigDecimal
    )

    sealed class Action {

        class CollatorIdUpdated(val newCollatorId: AccountId?) : Action()

        class AmountUpdated(val amount: BigDecimal): Action()
    }
}

@JvmName("connectWithAmount")
infix fun ParachainStakingRewardsComponent.connectWith(amountFlow: Flow<BigDecimal>) {
    amountFlow.onEach { newAmount ->
        onAction(Action.AmountUpdated(newAmount))
    }.launchIn(this)
}

infix fun ParachainStakingRewardsComponent.connectWith(selectedCollatorIdFlow: Flow<AccountId?>) {
    selectedCollatorIdFlow.onEach { newCollatorId ->
        onAction(Action.CollatorIdUpdated(newCollatorId))
    }.launchIn(this)
}

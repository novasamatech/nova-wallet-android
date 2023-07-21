package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import androidx.annotation.StringRes
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools.NominationPoolsStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain.ParachainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.relaychain.RelaychainStakeSummaryComponentFactory
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

typealias StakeSummaryComponent = StatefullComponent<StakeSummaryState, StakeSummaryEvent, StakeSummaryAction>

typealias StakeSummaryState = LoadingState<StakeSummaryModel>

class StakeSummaryModel(
    val totalStaked: AmountModel,
    val status: StakeStatusModel,
)

sealed class StakeStatusModel(val details: TitleAndMessage?) {

    class Active(details: TitleAndMessage? = null) : StakeStatusModel(details)

    class Waiting(
        val timeLeft: Long,
        @StringRes val messageFormat: Int,
        details: TitleAndMessage? = null
    ) : StakeStatusModel(details)

    class Inactive(details: TitleAndMessage? = null) : StakeStatusModel(details)
}

sealed class StakeSummaryEvent {

    class ShowStatusDialog(val titleAndMessage: TitleAndMessage) : StakeSummaryEvent()
}

sealed class StakeSummaryAction {

    object StatusClicked : StakeSummaryAction()
}

class StakeSummaryComponentFactory(
    private val relaychainComponentFactory: RelaychainStakeSummaryComponentFactory,
    private val parachainStakeSummaryComponentFactory: ParachainStakeSummaryComponentFactory,
    private val nominationPoolsStakeSummaryComponentFactory: NominationPoolsStakeSummaryComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): StakeSummaryComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = parachainStakeSummaryComponentFactory::create,
        nominationPoolsCreator = nominationPoolsStakeSummaryComponentFactory::create,
        hostContext = hostContext
    )
}

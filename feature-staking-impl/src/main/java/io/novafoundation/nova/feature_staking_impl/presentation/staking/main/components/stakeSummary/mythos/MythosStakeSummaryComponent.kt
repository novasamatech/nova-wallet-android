package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.mythos

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosDelegatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos.loadUserStakeState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest

class MythosStakeSummaryComponentFactory(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosStakeSummaryInteractor,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): StakeSummaryComponent = MythosStakeSummaryComponent(
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor,
        stakingOption = stakingOption,
        hostContext = hostContext
    )
}

private class MythosStakeSummaryComponent(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosStakeSummaryInteractor,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : BaseStakeSummaryComponent(hostContext.scope) {

    override val state: Flow<StakeSummaryState?> = mythosSharedComputation.loadUserStakeState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::userSTakeSummary
    )
        .shareInBackground()

    private fun userSTakeSummary(userStakeInfo: UserStakeInfo): Flow<StakeSummaryModel> {
        return interactor.stakeSummaryFlow(userStakeInfo, stakingOption).flatMapLatest { stakeSummary ->
            val status = stakeSummary.status.toUi()

            hostContext.assetFlow.mapLatest { asset ->
                StakeSummaryModel(
                    totalStaked = mapAmountToAmountModel(stakeSummary.activeStake, asset),
                    status = status
                )
            }
        }
    }

    private fun MythosDelegatorStatus.toUi(): StakeStatusModel {
        return when (this) {
            MythosDelegatorStatus.Active -> StakeStatusModel.Active
            MythosDelegatorStatus.Inactive -> StakeStatusModel.Inactive
        }
    }
}

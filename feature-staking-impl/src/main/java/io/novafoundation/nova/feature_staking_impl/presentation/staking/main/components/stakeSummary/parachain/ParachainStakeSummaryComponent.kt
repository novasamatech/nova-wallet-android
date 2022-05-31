package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.activeBonded
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary.DelegatorStatus
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary.ParachainStakingStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.BaseStakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeStatusModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.StakeSummaryState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlin.time.ExperimentalTime

class ParachainStakeSummaryComponentFactory(
    private val resourceManager: ResourceManager,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingStakeSummaryInteractor,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext,
    ): StakeSummaryComponent = ParachainStakeSummaryComponent(
        resourceManager = resourceManager,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor
    )
}

@OptIn(ExperimentalTime::class)
private class ParachainStakeSummaryComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingStakeSummaryInteractor,
    private val resourceManager: ResourceManager,

    assetWithChain: SingleAssetSharedState.AssetWithChain,
    private val hostContext: ComponentHostContext,
) : BaseStakeSummaryComponent(hostContext.scope) {

    override val state: Flow<StakeSummaryState?> = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = assetWithChain,
        stateProducer = ::delegatorSummaryStateFlow
    )
        .shareInBackground()

    private suspend fun delegatorSummaryStateFlow(delegatorState: DelegatorState.Delegator): Flow<StakeSummaryModel> {
        return interactor.delegatorStatusFlow(delegatorState).flatMapLatest { delegatorStatus ->
            val status = mapDelegatorStatusToStakeStatusModel(delegatorStatus)

            hostContext.assetFlow.mapLatest { asset ->
                StakeSummaryModel(
                    totalStaked = mapAmountToAmountModel(delegatorState.activeBonded, asset),
                    status = status
                )
            }
        }
    }

    private fun mapDelegatorStatusToStakeStatusModel(
        delegatorStatus: DelegatorStatus
    ): StakeStatusModel {
        return when (delegatorStatus) {
            DelegatorStatus.Active -> StakeStatusModel.Active(
                details = resourceManager.getString(R.string.staking_nominator_status_alert_active_title) to
                    resourceManager.getString(R.string.staking_parachain_delegator_status_active_message)
            )
            DelegatorStatus.Inactive -> StakeStatusModel.Inactive(
                details = resourceManager.getString(R.string.staking_nominator_status_alert_inactive_title) to
                    resourceManager.getString(R.string.staking_parachain_status_inactive_message)
            )
            is DelegatorStatus.Waiting -> StakeStatusModel.Waiting(
                timeLeft = delegatorStatus.timeLeft.toLongMilliseconds(),
                messageFormat = R.string.staking_parachain_next_round_format,
                details = null
            )
        }
    }
}
